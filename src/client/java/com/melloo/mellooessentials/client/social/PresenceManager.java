package com.melloo.mellooessentials.client.social;

import com.melloo.mellooessentials.client.api.ApiClient;
import com.melloo.mellooessentials.client.api.ModAuthManager;
import com.melloo.mellooessentials.client.config.EssentialsConfig;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects other players also running MellooEssentials, via sky.melloo.me's EXISTING presence
 * rendezvous (see ApiClient's own doc comment - no new server-side code at all). Each client
 * reports its own UUID + currently-enabled cosmetics every ~2s, and periodically asks which of the
 * players in the current tab list have reported in recently, getting back both their cosmetics AND
 * their server-resolved sky.melloo.me team role (for the fixed pink staff highlight). Purely
 * additive - a network hiccup just means nobody's cosmetics/staff status syncs for a bit, nothing
 * else breaks. Always on, not user-togglable.
 */
public final class PresenceManager {
	private static final Logger LOGGER = LoggerFactory.getLogger("MellooEssentials/PresenceManager");
	private static final int REPORT_INTERVAL_TICKS = 40; // 2s
	private static final int QUERY_INTERVAL_TICKS = 100; // 5s

	private static int tickCounter = 0;
	private static volatile boolean reportInFlight = false;
	private static volatile boolean queryInFlight = false;

	private static final Map<UUID, Map<String, Integer>> otherCosmetics = new ConcurrentHashMap<>();
	private static final Map<UUID, Map<String, String>> otherParticleKinds = new ConcurrentHashMap<>();
	private static final Map<UUID, String> otherRoles = new ConcurrentHashMap<>();
	// Which presence entries also reported via SkyMelloo's own client recently (server-resolved from
	// the report's client header, see ApiClient.PresenceEntry's doc comment) - this, not generic
	// isModUser (true for ANY mod, since both mods share the same /presence endpoint), is the real
	// signal for the mod-user marker's pink-vs-light-blue choice. Absent from this set = light blue.
	private static final Set<UUID> otherIsSkyMelloo = ConcurrentHashMap.newKeySet();

	private PresenceManager() {
	}

	public static void tick(Minecraft client) {
		if (client.player == null || client.getConnection() == null) {
			return;
		}
		tickCounter++;
		if (tickCounter % REPORT_INTERVAL_TICKS == 0) {
			reportSelf(client);
		}
		if (tickCounter % QUERY_INTERVAL_TICKS == 0) {
			queryNearby(client);
		}
	}

	private static void reportSelf(Minecraft client) {
		if (reportInFlight) {
			return;
		}
		reportInFlight = true;
		String uuid = client.player.getUUID().toString();
		String username = client.player.getGameProfile().name();
		List<String> cosmetics = collectEnabledCosmetics();
		ModAuthManager.getIdentity(client)
				.exceptionally(error -> null)
				.thenCompose(identity -> ApiClient.reportPresence(uuid, username, cosmetics, identity))
				.whenComplete((v, error) -> {
					reportInFlight = false;
					LOGGER.debug(error == null
							? "Presence: reported self (" + cosmetics.size() + " cosmetic(s) active)."
							: "Presence: self-report failed (" + error.getMessage() + ").");
				});
	}

	private static void queryNearby(Minecraft client) {
		if (queryInFlight) {
			return;
		}
		// Deliberately INCLUDES the local player's own UUID (unlike SkyMelloo's ModPresenceManager,
		// which only ever needs to know about others) - otherRoles/isStaff would otherwise never
		// resolve for yourself at all, since nothing else here ever asks the server "what's MY role".
		// You're trivially always a member of your own party, so without this, a staff player looking
		// at themselves fell through to the party (light blue) highlight instead of staff (pink).
		List<String> uuids = new ArrayList<>();
		uuids.add(client.player.getUUID().toString());
		for (var info : client.getConnection().getOnlinePlayers()) {
			UUID id = info.getProfile().id();
			if (!id.equals(client.player.getUUID())) {
				uuids.add(id.toString());
			}
		}
		queryInFlight = true;
		ModAuthManager.getIdentity(client)
				.exceptionally(error -> null)
				.thenCompose(identity -> ApiClient.queryPresence(uuids, identity))
				.whenComplete((present, error) -> {
					queryInFlight = false;
					if (error != null || present == null) {
						LOGGER.debug("Presence: nearby query failed" + (error != null ? " (" + error.getMessage() + ")" : "") + ".");
						return;
					}
					Map<UUID, Map<String, Integer>> updatedCosmetics = new HashMap<>();
					Map<UUID, Map<String, String>> updatedParticleKinds = new HashMap<>();
					Map<UUID, String> updatedRoles = new HashMap<>();
					Set<UUID> updatedIsSkyMelloo = new HashSet<>();
					for (ApiClient.PresenceEntry entry : present) {
						UUID uuid;
						try {
							uuid = UUID.fromString(entry.uuid());
						} catch (IllegalArgumentException e) {
							continue;
						}
						Map<String, Integer> parsed = new HashMap<>();
						Map<String, String> parsedParticleKinds = new HashMap<>();
						for (String token : entry.cosmetics()) {
							// "key=PARTICLENAME" - a color-capable cosmetic switched away from colored dust to a
							// named particle instead (mutually exclusive with color, see CosmeticEditScreen).
							int particleSep = token.indexOf('=');
							if (particleSep != -1) {
								parsed.put(token.substring(0, particleSep), -1);
								parsedParticleKinds.put(token.substring(0, particleSep), token.substring(particleSep + 1));
								continue;
							}
							int sep = token.indexOf(':');
							if (sep == -1) {
								parsed.put(token, -1);
								continue;
							}
							try {
								int rgb = Integer.parseInt(token.substring(sep + 1), 16) & 0xFFFFFF;
								parsed.put(token.substring(0, sep), rgb);
							} catch (NumberFormatException ignored) {
								// malformed color token from a mismatched/future version - skip it
							}
						}
						updatedCosmetics.put(uuid, parsed);
						updatedParticleKinds.put(uuid, parsedParticleKinds);
						if (entry.role() != null) {
							updatedRoles.put(uuid, entry.role());
						}
						if (entry.skymelloo()) {
							updatedIsSkyMelloo.add(uuid);
						}
					}
					otherCosmetics.clear();
					otherCosmetics.putAll(updatedCosmetics);
					otherParticleKinds.clear();
					otherParticleKinds.putAll(updatedParticleKinds);
					otherRoles.clear();
					otherRoles.putAll(updatedRoles);
					otherIsSkyMelloo.clear();
					otherIsSkyMelloo.addAll(updatedIsSkyMelloo);
				});
	}

	public static boolean isModUser(UUID uuid) {
		return otherCosmetics.containsKey(uuid);
	}

	/** True only if this uuid has ALSO reported presence via SkyMelloo's own client recently, not just MellooEssentials' - see the mod-user marker's pink-vs-light-blue choice in EntityDisplayNameMixin/PlayerTabOverlayMixin. isModUser alone can't answer this: it's true for anyone running either mod, since both report to the same /presence endpoint. */
	public static boolean isSkyMelloo(UUID uuid) {
		return otherIsSkyMelloo.contains(uuid);
	}

	public static boolean hasCosmetic(UUID uuid, String effectKey) {
		Map<String, Integer> cosmetics = otherCosmetics.get(uuid);
		return cosmetics != null && cosmetics.containsKey(effectKey);
	}

	/** ARGB color that player reported for this effect, or -1 if enabled with no custom color (use the default), 0 if not enabled at all. */
	public static int getCosmeticColor(UUID uuid, String effectKey) {
		Map<String, Integer> cosmetics = otherCosmetics.get(uuid);
		if (cosmetics == null) {
			return 0;
		}
		Integer color = cosmetics.get(effectKey);
		if (color == null) {
			return 0;
		}
		return color == -1 ? -1 : (color | 0xFF000000);
	}

	/** Non-null only if that player switched this cosmetic away from colored dust to a named particle instead - see the "key=PARTICLENAME" wire format in collectEnabledCosmetics/queryNearby. */
	public static String getCosmeticParticleKind(UUID uuid, String effectKey) {
		Map<String, String> kinds = otherParticleKinds.get(uuid);
		return kinds == null ? null : kinds.get(effectKey);
	}

	/** Any sky.melloo.me team role at all ("owner"/"admin"/"developer"/"moderator") - all of them get the fixed staff/contributor pink highlight here, not just owner/admin/developer like SkyMelloo's own narrower distinction. */
	public static boolean isStaff(UUID uuid) {
		return otherRoles.get(uuid) != null;
	}

	private static String hex(Color color) {
		return String.format("%06X", color.getRGB() & 0xFFFFFF);
	}

	/** "key=PARTICLENAME" if this color-capable cosmetic was switched to a named particle, else "key:HEXCOLOR" - see getCosmeticParticleKind's doc comment. */
	private static String encode(String key, Color color, String particleKind) {
		return particleKind != null ? key + "=" + particleKind : key + ":" + hex(color);
	}

	/** Same idea for a cosmetic with no color at all - bare "key" (its own default look) or "key=PARTICLENAME" (overridden). */
	private static String encodeParticleOnly(String key, String particleKind) {
		return particleKind != null ? key + "=" + particleKind : key;
	}

	private static List<String> collectEnabledCosmetics() {
		List<String> list = new ArrayList<>();
		if (!EssentialsConfig.get().cosmeticsEnabled) {
			return list;
		}
		EssentialsConfig c = EssentialsConfig.get();
		if (c.haloEnabled) {
			list.add(encode("halo", c.haloColor, c.haloParticleKind));
		}
		if (c.cherryBlossomEnabled) {
			list.add("cherryBlossom");
		}
		if (c.rainbowHelixEnabled) {
			list.add(encode("rainbowHelix", c.rainbowHelixColor, c.rainbowHelixParticleKind));
		}
		if (c.auraEnabled) {
			list.add(encode("aura", c.auraColor, c.auraParticleKind));
		}
		if (c.waveEnabled) {
			list.add(encode("wave", c.waveColor, c.waveParticleKind));
		}
		if (c.rainCloudEnabled) {
			list.add(encodeParticleOnly("rainCloud", c.rainCloudParticleKind));
		}
		if (c.fireRingEnabled) {
			list.add("fireRing");
		}
		if (c.starRainEnabled) {
			list.add("starRain");
		}
		if (c.sparkAuraEnabled) {
			list.add("sparkAura");
		}
		if (c.lissajousEnabled) {
			list.add(encode("lissajous", c.lissajousColor, c.lissajousParticleKind));
		}
		if (c.roseCurveEnabled) {
			list.add(encode("roseCurve", c.roseCurveColor, c.roseCurveParticleKind));
		}
		if (c.landingShockwaveEnabled) {
			list.add(encode("landingShockwave", c.landingShockwaveColor, c.landingShockwaveParticleKind));
		}
		if (c.fireworkBurstEnabled) {
			list.add("fireworkBurst");
		}
		if (c.frostAuraEnabled) {
			list.add(encode("frostAura", c.frostAuraColor, c.frostAuraParticleKind));
		}
		if (c.noteMelodyEnabled) {
			list.add("noteMelody");
		}
		if (c.portalVortexEnabled) {
			list.add(encode("portalVortex", c.portalVortexColor, c.portalVortexParticleKind));
		}
		if (c.heartTrailEnabled) {
			list.add("heartTrail");
		}
		if (c.spiralGalaxyEnabled) {
			list.add(encode("spiralGalaxy", c.spiralGalaxyColor, c.spiralGalaxyParticleKind));
		}
		if (c.jumpTrailEnabled) {
			list.add(encode("jumpTrail", c.jumpTrailColor, c.jumpTrailParticleKind));
		}
		if (c.totemFlashEnabled) {
			list.add("totemFlash");
		}
		if (c.sculkPulseEnabled) {
			list.add("sculkPulse");
		}
		if (c.omenAuraEnabled) {
			list.add("omenAura");
		}
		if (c.gustAuraEnabled) {
			list.add(encode("gustAura", c.gustAuraColor, c.gustAuraParticleKind));
		}
		if (c.ashFallEnabled) {
			list.add(encode("ashFall", c.ashFallColor, c.ashFallParticleKind));
		}
		if (c.campfireSmokeEnabled) {
			list.add(encodeParticleOnly("campfireSmoke", c.campfireSmokeParticleKind));
		}
		if (c.tornadoEnabled) {
			list.add(encode("tornado", c.tornadoColor, c.tornadoParticleKind));
		}
		if (c.blackHoleEnabled) {
			list.add(encode("blackHole", c.blackHoleColor, c.blackHoleParticleKind));
		}
		if (c.twinVortexEnabled) {
			list.add(encode("twinVortex", c.twinVortexColor, c.twinVortexParticleKind));
		}
		if (c.enchantedCritSparkleEnabled) {
			list.add("enchantedCritSparkle");
		}
		if (c.dustPlumeTrailEnabled) {
			list.add("dustPlumeTrail");
		}
		if (c.chargeUpEnabled) {
			list.add(encode("chargeUp", c.chargeUpColor, c.chargeUpParticleKind));
		}
		if (c.orbitRingsEnabled) {
			list.add(encode("orbitRings", c.orbitRingsColor, c.orbitRingsParticleKind));
		}
		if (c.lightningAuraEnabled) {
			list.add(encodeParticleOnly("lightningAura", c.lightningAuraParticle));
		}
		if (c.confettiBurstEnabled) {
			list.add(encodeParticleOnly("confettiBurst", c.confettiBurstParticleKind));
		}
		if (c.mothWingsEnabled) {
			list.add(encode("mothWings", c.mothWingsColor, c.mothWingsParticleKind));
		}
		if (c.phoenixWingsEnabled) {
			list.add(encode("phoenixWings", c.phoenixWingsColor, c.phoenixWingsParticleKind));
		}
		if (c.voidRiftEnabled) {
			list.add("voidRift");
		}
		if (c.starWeaveEnabled) {
			list.add(encodeParticleOnly("starWeave", c.starWeaveParticle));
		}
		if (c.ascendingSparklesEnabled) {
			list.add(encodeParticleOnly("ascendingSparkles", c.ascendingSparklesParticle));
		}
		if (c.cometTrailEnabled) {
			list.add(encodeParticleOnly("cometTrail", c.cometTrailParticle));
		}
		if (c.starVeilEnabled) {
			list.add(encodeParticleOnly("starVeil", c.starVeilParticle));
		}
		if (c.radiantPulseEnabled) {
			list.add(encodeParticleOnly("radiantPulse", c.radiantPulseParticle));
		}
		if (c.pulsingSphereEnabled) {
			list.add(encode("pulsingSphere", c.pulsingSphereColor, c.pulsingSphereParticleKind));
		}
		if (c.scannerEnabled) {
			list.add(encode("scanner", c.scannerColor, c.scannerParticleKind));
		}
		if (c.physicsCapeEnabled) {
			list.add(encode("physicsCape", c.physicsCapeColor, c.physicsCapeParticleKind));
		}
		if (c.cloakEnabled) {
			list.add(encode("cloak", c.cloakColor, c.cloakParticleKind));
		}
		return list;
	}
}
