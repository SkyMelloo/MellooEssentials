package com.melloo.mellooessentials.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import net.fabricmc.loader.api.FabricLoader;

import java.awt.Color;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Plain Gson-persisted settings (no account, no cloud sync, no YACL) - saved to
 * {@code config/mellooessentials.json}. {@link Color} is stored as a plain ARGB int via a custom
 * adapter, since Gson can't serialize java.awt.Color's own fields sensibly by default.
 */
public final class EssentialsConfig {
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("mellooessentials.json");
	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeAdapter(Color.class, (JsonSerializer<Color>) (src, type, ctx) -> ctx.serialize(src.getRGB()))
			.registerTypeAdapter(Color.class, (JsonDeserializer<Color>) (json, type, ctx) -> new Color(json.getAsInt(), true))
			.create();

	private static EssentialsConfig instance;

	public static EssentialsConfig get() {
		if (instance == null) {
			instance = load();
		}
		return instance;
	}

	public static void save() {
		if (instance == null) {
			return;
		}
		try {
			Files.createDirectories(FILE.getParent());
			try (Writer writer = Files.newBufferedWriter(FILE)) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not save MellooEssentials config", e);
		}
	}

	private static EssentialsConfig load() {
		if (Files.exists(FILE)) {
			try (Reader reader = Files.newBufferedReader(FILE)) {
				EssentialsConfig loaded = GSON.fromJson(reader, EssentialsConfig.class);
				if (loaded != null) {
					return loaded;
				}
			} catch (IOException ignored) {
				// Falls through to a fresh default config below.
			}
		}
		return new EssentialsConfig();
	}

	// Party/staff highlighting and presence sync are always on - not user-togglable at all (see
	// HighlightManager/PresenceManager, which no longer gate on a config flag for these). Party is
	// always light blue, sky.melloo.me team members (any role - contributor/admin/moderator/etc) are
	// always pink and take priority over party when someone's both, so the meaning of a color never
	// depends on some other player's personal settings.

	// ---- Info HUD ----
	// Position defaults (-1) mean "not set yet, use each HUD's own sensible default corner" - the
	// same convention SkyMelloo's own hud*X/Y fields use. Draggable via SkyMelloo's own HUD layout
	// editor (key J) when it's installed, since that's the only positioning UI either mod has right
	// now - see that screen's own doc comment.

	public boolean playerInfoHudEnabled = false;
	public int hudPlayerInfoX = -1;
	public int hudPlayerInfoY = -1;

	public boolean connectionStatusHudEnabled = true;
	public int hudConnectionStatusX = -1;
	public int hudConnectionStatusY = -1;

	// ---- Friend Highlighting ----
	// Moved here from SkyMelloo (its own "Player Highlighting"/"SkyMelloo Friend Color"/"Player Glow
	// Outline" settings) - staff/party highlighting live here already and are fixed, not user-
	// adjustable; friend highlighting stays configurable since "which color represents MY friends to
	// ME" is a legitimate personal preference, unlike staff/party which are shared facts nobody should
	// be able to fake/hide. See highlight.HighlightManager.

	public boolean friendHighlightEnabled = false;
	public java.awt.Color friendHighlightColor = new java.awt.Color(0xFF55FFFF, true);
	// Off by default, same reasoning SkyMelloo's own version had: forcing the glow-outline (visible
	// through walls) on every friend can hide cosmetic layers from mods like Lunar Client (capes/
	// wings) for some players - the colored nametag marker alone already gives a see-through
	// indicator on its own.
	public boolean friendGlowOutlineEnabled = false;

	// ---- Cloud Sync ----
	// Same reasoning/architecture as SkyMelloo's own cloudSyncEnabled - see CloudSyncManager's doc
	// comment. Off by default now, same privacy-first bar as presenceSharingEnabled above - a linked
	// account is no longer enough on its own to start syncing HUD positions/cosmetics, this needs its
	// own explicit opt-in too.

	public boolean cloudSyncEnabled = false;

	// ---- Sharing & Privacy ----
	// Master switch for presence reporting itself - everything else this mod (or SkyMelloo, which
	// hooks into the same report) shares about you (online status, location, cosmetics) depends on
	// this being on. Off means no presence report is sent at all, not just a reduced one. Moved here
	// from SkyMelloo (was presenceSharingEnabled there, same field/semantics) since it's a general
	// account-privacy setting, not SkyBlock/dungeon-specific.
	public boolean presenceSharingEnabled = false;

	// ---- Cosmetics ----
	// Same effect set/defaults as SkyMelloo's CosmeticsRenderer. Visible to other Hypixel
	// Essentials (or SkyMelloo) users nearby via presence sync (see presenceSharingEnabled above).

	public boolean cosmeticsEnabled = true;

	// Per-effect: the effect still gets reported to others (they still see it via presence sync,
	// same as always) - only the LOCAL self-render call is skipped, for effects a player finds
	// distracting or view-blocking up close on their own screen. Keyed by the same effect key
	// strings PresenceManager/CosmeticsRenderer#tickOthers already use (e.g. "halo", "cherryBlossom").
	public java.util.Set<String> hiddenSelfEffects = new java.util.HashSet<>();

	public boolean isSelfHidden(String effectKey) {
		return hiddenSelfEffects.contains(effectKey);
	}

	public void setSelfHidden(String effectKey, boolean hidden) {
		if (hidden) {
			hiddenSelfEffects.add(effectKey);
		} else {
			hiddenSelfEffects.remove(effectKey);
		}
	}

	/** Bulk-applies one color to every color-capable cosmetic at once - see BulkCosmeticScreen. */
	public void setAllColors(Color color) {
		haloColor = color;
		rainbowHelixColor = color;
		auraColor = color;
		waveColor = color;
		lissajousColor = color;
		roseCurveColor = color;
		landingShockwaveColor = color;
		frostAuraColor = color;
		portalVortexColor = color;
		spiralGalaxyColor = color;
		jumpTrailColor = color;
		gustAuraColor = color;
		ashFallColor = color;
		tornadoColor = color;
		blackHoleColor = color;
		twinVortexColor = color;
		chargeUpColor = color;
		orbitRingsColor = color;
		mothWingsColor = color;
		phoenixWingsColor = color;
		pulsingSphereColor = color;
		scannerColor = color;
		physicsCapeColor = color;
		cloakColor = color;
	}

	/** Bulk-applies one particle-kind choice (or null to reset everything back to its own default look) to literally every cosmetic that has a particle-kind option at all - see BulkCosmeticScreen. */
	public void setAllParticleKinds(String kindName) {
		haloParticleKind = kindName;
		rainbowHelixParticleKind = kindName;
		auraParticleKind = kindName;
		waveParticleKind = kindName;
		lissajousParticleKind = kindName;
		roseCurveParticleKind = kindName;
		landingShockwaveParticleKind = kindName;
		frostAuraParticleKind = kindName;
		portalVortexParticleKind = kindName;
		spiralGalaxyParticleKind = kindName;
		jumpTrailParticleKind = kindName;
		gustAuraParticleKind = kindName;
		ashFallParticleKind = kindName;
		tornadoParticleKind = kindName;
		blackHoleParticleKind = kindName;
		twinVortexParticleKind = kindName;
		chargeUpParticleKind = kindName;
		orbitRingsParticleKind = kindName;
		mothWingsParticleKind = kindName;
		phoenixWingsParticleKind = kindName;
		pulsingSphereParticleKind = kindName;
		physicsCapeParticleKind = kindName;
		cloakParticleKind = kindName;
		scannerParticleKind = kindName;
		rainCloudParticleKind = kindName;
		campfireSmokeParticleKind = kindName;
		confettiBurstParticleKind = kindName;
		if (kindName == null) {
			// The "concrete default" cosmetics never had a null state to begin with - resetting them
			// means putting back their own original kind, not leaving them null (ParticleKind.byNameOr
			// only falls back to that default when the string is null OR unrecognized, so either works,
			// but writing the real name keeps the config file self-explanatory).
			cherryBlossomParticle = "CHERRY_BLOSSOM";
			fireRingParticle = "FLAME";
			starRainParticle = "SPARKLE";
			sparkAuraParticle = "SPARK";
			fireworkBurstParticle = "FIREWORK";
			noteMelodyParticle = "NOTE";
			totemFlashParticle = "TOTEM";
			sculkPulseParticle = "SCULK";
			omenAuraParticle = "OMEN";
			enchantedCritSparkleParticle = "ENCHANTED_CRIT";
			dustPlumeTrailParticle = "DUST_PLUME";
			voidRiftParticle = "VOID_RIFT";
			heartTrailParticle = "HEART";
			lightningAuraParticle = "SPARK";
			starWeaveParticle = "SPARKLE";
			ascendingSparklesParticle = "SPARKLE";
			cometTrailParticle = "SPARKLE";
			starVeilParticle = "SPARKLE";
			radiantPulseParticle = "SPARKLE";
		} else {
			cherryBlossomParticle = kindName;
			fireRingParticle = kindName;
			starRainParticle = kindName;
			sparkAuraParticle = kindName;
			fireworkBurstParticle = kindName;
			noteMelodyParticle = kindName;
			totemFlashParticle = kindName;
			sculkPulseParticle = kindName;
			omenAuraParticle = kindName;
			enchantedCritSparkleParticle = kindName;
			dustPlumeTrailParticle = kindName;
			voidRiftParticle = kindName;
			heartTrailParticle = kindName;
			lightningAuraParticle = kindName;
			starWeaveParticle = kindName;
			ascendingSparklesParticle = kindName;
			cometTrailParticle = kindName;
			starVeilParticle = kindName;
			radiantPulseParticle = kindName;
		}
	}

	/** Resets every cosmetic-related field (master switch, enabled flags, colors, particle kinds, hidden-self set) back to its shipped default - leaves playerInfoHudEnabled untouched. */
	public void resetAllCosmetics() {
		EssentialsConfig fresh = new EssentialsConfig();
		cosmeticsEnabled = fresh.cosmeticsEnabled;
		hiddenSelfEffects = fresh.hiddenSelfEffects;

		haloEnabled = fresh.haloEnabled;
		haloColor = fresh.haloColor;
		haloGlow = fresh.haloGlow;
		haloParticleKind = fresh.haloParticleKind;
		cherryBlossomEnabled = fresh.cherryBlossomEnabled;
		cherryBlossomParticle = fresh.cherryBlossomParticle;
		rainbowHelixEnabled = fresh.rainbowHelixEnabled;
		rainbowHelixColor = fresh.rainbowHelixColor;
		rainbowHelixParticleKind = fresh.rainbowHelixParticleKind;
		auraEnabled = fresh.auraEnabled;
		auraColor = fresh.auraColor;
		auraGlow = fresh.auraGlow;
		auraParticleKind = fresh.auraParticleKind;
		waveEnabled = fresh.waveEnabled;
		waveColor = fresh.waveColor;
		waveParticleKind = fresh.waveParticleKind;
		rainCloudEnabled = fresh.rainCloudEnabled;
		rainCloudParticleKind = fresh.rainCloudParticleKind;
		fireRingEnabled = fresh.fireRingEnabled;
		fireRingParticle = fresh.fireRingParticle;
		starRainEnabled = fresh.starRainEnabled;
		starRainParticle = fresh.starRainParticle;
		sparkAuraEnabled = fresh.sparkAuraEnabled;
		sparkAuraParticle = fresh.sparkAuraParticle;
		lissajousEnabled = fresh.lissajousEnabled;
		lissajousColor = fresh.lissajousColor;
		lissajousParticleKind = fresh.lissajousParticleKind;
		roseCurveEnabled = fresh.roseCurveEnabled;
		roseCurveColor = fresh.roseCurveColor;
		roseCurveParticleKind = fresh.roseCurveParticleKind;
		landingShockwaveEnabled = fresh.landingShockwaveEnabled;
		landingShockwaveColor = fresh.landingShockwaveColor;
		landingShockwaveParticleKind = fresh.landingShockwaveParticleKind;
		fireworkBurstEnabled = fresh.fireworkBurstEnabled;
		fireworkBurstParticle = fresh.fireworkBurstParticle;
		frostAuraEnabled = fresh.frostAuraEnabled;
		frostAuraColor = fresh.frostAuraColor;
		frostAuraParticleKind = fresh.frostAuraParticleKind;
		noteMelodyEnabled = fresh.noteMelodyEnabled;
		noteMelodyParticle = fresh.noteMelodyParticle;
		portalVortexEnabled = fresh.portalVortexEnabled;
		portalVortexColor = fresh.portalVortexColor;
		portalVortexParticleKind = fresh.portalVortexParticleKind;
		heartTrailEnabled = fresh.heartTrailEnabled;
		heartTrailParticle = fresh.heartTrailParticle;
		spiralGalaxyEnabled = fresh.spiralGalaxyEnabled;
		spiralGalaxyColor = fresh.spiralGalaxyColor;
		spiralGalaxyParticleKind = fresh.spiralGalaxyParticleKind;
		jumpTrailEnabled = fresh.jumpTrailEnabled;
		jumpTrailColor = fresh.jumpTrailColor;
		jumpTrailParticleKind = fresh.jumpTrailParticleKind;
		totemFlashEnabled = fresh.totemFlashEnabled;
		totemFlashParticle = fresh.totemFlashParticle;
		sculkPulseEnabled = fresh.sculkPulseEnabled;
		sculkPulseParticle = fresh.sculkPulseParticle;
		omenAuraEnabled = fresh.omenAuraEnabled;
		omenAuraParticle = fresh.omenAuraParticle;
		gustAuraEnabled = fresh.gustAuraEnabled;
		gustAuraColor = fresh.gustAuraColor;
		gustAuraParticleKind = fresh.gustAuraParticleKind;
		ashFallEnabled = fresh.ashFallEnabled;
		ashFallColor = fresh.ashFallColor;
		ashFallParticleKind = fresh.ashFallParticleKind;
		campfireSmokeEnabled = fresh.campfireSmokeEnabled;
		campfireSmokeParticleKind = fresh.campfireSmokeParticleKind;
		tornadoEnabled = fresh.tornadoEnabled;
		tornadoColor = fresh.tornadoColor;
		tornadoParticleKind = fresh.tornadoParticleKind;
		blackHoleEnabled = fresh.blackHoleEnabled;
		blackHoleColor = fresh.blackHoleColor;
		blackHoleParticleKind = fresh.blackHoleParticleKind;
		twinVortexEnabled = fresh.twinVortexEnabled;
		twinVortexColor = fresh.twinVortexColor;
		twinVortexParticleKind = fresh.twinVortexParticleKind;
		enchantedCritSparkleEnabled = fresh.enchantedCritSparkleEnabled;
		enchantedCritSparkleParticle = fresh.enchantedCritSparkleParticle;
		dustPlumeTrailEnabled = fresh.dustPlumeTrailEnabled;
		dustPlumeTrailParticle = fresh.dustPlumeTrailParticle;
		chargeUpEnabled = fresh.chargeUpEnabled;
		chargeUpColor = fresh.chargeUpColor;
		chargeUpParticleKind = fresh.chargeUpParticleKind;
		orbitRingsEnabled = fresh.orbitRingsEnabled;
		orbitRingsColor = fresh.orbitRingsColor;
		orbitRingsParticleKind = fresh.orbitRingsParticleKind;
		lightningAuraEnabled = fresh.lightningAuraEnabled;
		lightningAuraParticle = fresh.lightningAuraParticle;
		confettiBurstEnabled = fresh.confettiBurstEnabled;
		confettiBurstParticleKind = fresh.confettiBurstParticleKind;
		mothWingsEnabled = fresh.mothWingsEnabled;
		mothWingsColor = fresh.mothWingsColor;
		mothWingsParticleKind = fresh.mothWingsParticleKind;
		phoenixWingsEnabled = fresh.phoenixWingsEnabled;
		phoenixWingsColor = fresh.phoenixWingsColor;
		phoenixWingsParticleKind = fresh.phoenixWingsParticleKind;
		voidRiftEnabled = fresh.voidRiftEnabled;
		voidRiftParticle = fresh.voidRiftParticle;
		starWeaveEnabled = fresh.starWeaveEnabled;
		starWeaveParticle = fresh.starWeaveParticle;
		ascendingSparklesEnabled = fresh.ascendingSparklesEnabled;
		ascendingSparklesParticle = fresh.ascendingSparklesParticle;
		cometTrailEnabled = fresh.cometTrailEnabled;
		cometTrailParticle = fresh.cometTrailParticle;
		starVeilEnabled = fresh.starVeilEnabled;
		starVeilParticle = fresh.starVeilParticle;
		radiantPulseEnabled = fresh.radiantPulseEnabled;
		radiantPulseParticle = fresh.radiantPulseParticle;
		pulsingSphereEnabled = fresh.pulsingSphereEnabled;
		pulsingSphereColor = fresh.pulsingSphereColor;
		pulsingSphereParticleKind = fresh.pulsingSphereParticleKind;
		scannerEnabled = fresh.scannerEnabled;
		scannerColor = fresh.scannerColor;
		scannerParticleKind = fresh.scannerParticleKind;
		physicsCapeEnabled = fresh.physicsCapeEnabled;
		physicsCapeColor = fresh.physicsCapeColor;
		physicsCapeParticleKind = fresh.physicsCapeParticleKind;
		cloakEnabled = fresh.cloakEnabled;
		cloakColor = fresh.cloakColor;
		cloakParticleKind = fresh.cloakParticleKind;
	}

	public boolean haloEnabled = false;
	public Color haloColor = new Color(0xFFAA33FF, true);
	public boolean haloGlow = false;
	public String haloParticleKind = null;

	public boolean cherryBlossomEnabled = false;
	public String cherryBlossomParticle = "CHERRY_BLOSSOM";

	public boolean rainbowHelixEnabled = false;
	public Color rainbowHelixColor = new Color(0xFFAA33FF, true);
	public String rainbowHelixParticleKind = null;

	public boolean auraEnabled = false;
	public Color auraColor = new Color(0xFFAA33FF, true);
	public boolean auraGlow = false;
	public String auraParticleKind = null;

	public boolean waveEnabled = false;
	public Color waveColor = new Color(0xFFAA33FF, true);
	public String waveParticleKind = null;

	public boolean rainCloudEnabled = false;
	// null = the original cloud-puffs+rain-drops look; non-null = every particle uses this one kind instead.
	public String rainCloudParticleKind = null;

	public boolean fireRingEnabled = false;
	public String fireRingParticle = "FLAME";

	public boolean starRainEnabled = false;
	public String starRainParticle = "SPARKLE";

	public boolean sparkAuraEnabled = false;
	public String sparkAuraParticle = "SPARK";

	public boolean lissajousEnabled = false;
	public Color lissajousColor = new Color(0xFFAA33FF, true);
	public String lissajousParticleKind = null;

	public boolean roseCurveEnabled = false;
	public Color roseCurveColor = new Color(0xFFAA33FF, true);
	public String roseCurveParticleKind = null;

	public boolean landingShockwaveEnabled = false;
	public Color landingShockwaveColor = new Color(0xFFAA33FF, true);
	public String landingShockwaveParticleKind = null;

	public boolean fireworkBurstEnabled = false;
	public String fireworkBurstParticle = "FIREWORK";

	public boolean frostAuraEnabled = false;
	public Color frostAuraColor = new Color(0xFFAA33FF, true);
	public String frostAuraParticleKind = null;

	public boolean noteMelodyEnabled = false;
	public String noteMelodyParticle = "NOTE";

	public boolean portalVortexEnabled = false;
	public Color portalVortexColor = new Color(0xFFAA33FF, true);
	public String portalVortexParticleKind = null;

	public boolean heartTrailEnabled = false;
	public String heartTrailParticle = "HEART";

	public boolean spiralGalaxyEnabled = false;
	public Color spiralGalaxyColor = new Color(0xFFAA33FF, true);
	public String spiralGalaxyParticleKind = null;

	public boolean jumpTrailEnabled = false;
	public Color jumpTrailColor = new Color(0xFFAA33FF, true);
	public String jumpTrailParticleKind = null;

	public boolean totemFlashEnabled = false;
	public String totemFlashParticle = "TOTEM";

	public boolean sculkPulseEnabled = false;
	public String sculkPulseParticle = "SCULK";

	public boolean omenAuraEnabled = false;
	public String omenAuraParticle = "OMEN";

	public boolean gustAuraEnabled = false;
	public Color gustAuraColor = new Color(0xFFAA33FF, true);
	public String gustAuraParticleKind = null;

	public boolean ashFallEnabled = false;
	public Color ashFallColor = new Color(0xFFAA33FF, true);
	public String ashFallParticleKind = null;

	public boolean campfireSmokeEnabled = false;
	// null = the original cosy/signal smoke mix; non-null = every wisp uses this one kind instead.
	public String campfireSmokeParticleKind = null;

	public boolean tornadoEnabled = false;
	public Color tornadoColor = new Color(0xFFAA33FF, true);
	public String tornadoParticleKind = null;

	public boolean blackHoleEnabled = false;
	public Color blackHoleColor = new Color(0xFFAA33FF, true);
	public String blackHoleParticleKind = null;

	public boolean twinVortexEnabled = false;
	public Color twinVortexColor = new Color(0xFFAA33FF, true);
	public String twinVortexParticleKind = null;

	public boolean enchantedCritSparkleEnabled = false;
	public String enchantedCritSparkleParticle = "ENCHANTED_CRIT";

	public boolean dustPlumeTrailEnabled = false;
	public String dustPlumeTrailParticle = "DUST_PLUME";

	public boolean chargeUpEnabled = false;
	public Color chargeUpColor = new Color(0xFFAA33FF, true);
	public String chargeUpParticleKind = null;

	public boolean orbitRingsEnabled = false;
	public Color orbitRingsColor = new Color(0xFFAA33FF, true);
	public String orbitRingsParticleKind = null;

	public boolean lightningAuraEnabled = false;
	public String lightningAuraParticle = "SPARK";

	public boolean confettiBurstEnabled = false;
	// null = the original rainbow-confetti look; non-null = every piece uses this one kind instead.
	public String confettiBurstParticleKind = null;

	public boolean mothWingsEnabled = false;
	public Color mothWingsColor = new Color(0xFFFF8800, true);
	public String mothWingsParticleKind = null;

	public boolean phoenixWingsEnabled = false;
	public Color phoenixWingsColor = new Color(0xFFCC3300, true);
	public String phoenixWingsParticleKind = null;

	public boolean voidRiftEnabled = false;
	public String voidRiftParticle = "VOID_RIFT";

	public boolean starWeaveEnabled = false;
	public String starWeaveParticle = "SPARKLE";

	public boolean ascendingSparklesEnabled = false;
	public String ascendingSparklesParticle = "SPARKLE";

	public boolean cometTrailEnabled = false;
	public String cometTrailParticle = "SPARKLE";

	public boolean starVeilEnabled = false;
	public String starVeilParticle = "SPARKLE";

	public boolean radiantPulseEnabled = false;
	public String radiantPulseParticle = "SPARKLE";

	public boolean pulsingSphereEnabled = false;
	public Color pulsingSphereColor = new Color(0xFFAA33FF, true);
	public String pulsingSphereParticleKind = null;

	public boolean scannerEnabled = false;
	public Color scannerColor = new Color(0xFFAA33FF, true);
	public String scannerParticleKind = null;

	// A real simulated cloth cape - see CapeSimulator. Reacts to your own movement, water, and the
	// ground beneath it, unlike every other cosmetic here.
	public boolean physicsCapeEnabled = false;
	public Color physicsCapeColor = new Color(0xFF3355AA, true);
	public String physicsCapeParticleKind = null;

	// A simpler cape with a fixed, deterministic billow animation - no simulation at all.
	public boolean cloakEnabled = false;
	public Color cloakColor = new Color(0xFF552288, true);
	public String cloakParticleKind = null;
}
