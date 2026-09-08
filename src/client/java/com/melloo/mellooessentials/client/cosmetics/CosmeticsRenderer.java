package com.melloo.mellooessentials.client.cosmetics;

import com.melloo.mellooessentials.client.config.EssentialsConfig;
import com.melloo.mellooessentials.client.social.PresenceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Purely cosmetic, client-side particle effects; also renders them on nearby opted-in players via PresenceManager.
public final class CosmeticsRenderer {
	private CosmeticsRenderer() {
	}

	public static void tick(Minecraft client) {
		EssentialsConfig config = EssentialsConfig.get();
		// Master switch - off means no cosmetic effect renders at all, not just "hide the tab while
		// effects keep running in the background".
		if (client.player == null || client.level == null || !config.cosmeticsEnabled) {
			return;
		}
		AbstractClientPlayer self = client.player;

		if (config.haloEnabled && !config.isSelfHidden("halo")) {
			renderHalo(client, self, rgb(config.haloColor), config.haloParticleKind);
		}
		if (config.cherryBlossomEnabled && !config.isSelfHidden("cherryBlossom")) {
			renderCherryBlossom(client, self);
		}
		if (config.rainbowHelixEnabled && !config.isSelfHidden("rainbowHelix")) {
			renderRainbowHelix(client, self, rgb(config.rainbowHelixColor), config.rainbowHelixParticleKind);
		}
		if (config.auraEnabled && !config.isSelfHidden("aura")) {
			renderAura(client, self, rgb(config.auraColor), config.auraParticleKind);
		}
		if (config.waveEnabled && !config.isSelfHidden("wave")) {
			renderWave(client, self, rgb(config.waveColor), config.waveParticleKind);
		}
		if (config.rainCloudEnabled && !config.isSelfHidden("rainCloud")) {
			renderRainCloud(client, self, config.rainCloudParticleKind);
		}
		if (config.fireRingEnabled && !config.isSelfHidden("fireRing")) {
			renderFireRing(client, self);
		}
		if (config.starRainEnabled && !config.isSelfHidden("starRain")) {
			renderStarRain(client, self);
		}
		if (config.sparkAuraEnabled && !config.isSelfHidden("sparkAura")) {
			renderSparkAura(client, self);
		}
		if (config.lissajousEnabled && !config.isSelfHidden("lissajous")) {
			renderLissajous(client, self, rgb(config.lissajousColor), config.lissajousParticleKind);
		}
		if (config.roseCurveEnabled && !config.isSelfHidden("roseCurve")) {
			renderRoseCurve(client, self, rgb(config.roseCurveColor), config.roseCurveParticleKind);
		}
		if (config.fireworkBurstEnabled && !config.isSelfHidden("fireworkBurst")) {
			renderFireworkBurst(client, self);
		}
		if (config.frostAuraEnabled && !config.isSelfHidden("frostAura")) {
			renderFrostAura(client, self, rgb(config.frostAuraColor), config.frostAuraParticleKind);
		}
		if (config.noteMelodyEnabled && !config.isSelfHidden("noteMelody")) {
			renderNoteMelody(client, self);
		}
		if (config.portalVortexEnabled && !config.isSelfHidden("portalVortex")) {
			renderPortalVortex(client, self, rgb(config.portalVortexColor), config.portalVortexParticleKind);
		}
		if (config.heartTrailEnabled && !config.isSelfHidden("heartTrail")) {
			renderHeartTrail(client, self);
		}
		if (config.spiralGalaxyEnabled && !config.isSelfHidden("spiralGalaxy")) {
			renderSpiralGalaxy(client, self, rgb(config.spiralGalaxyColor), config.spiralGalaxyParticleKind);
		}
		if (config.jumpTrailEnabled && !config.isSelfHidden("jumpTrail")) {
			renderJumpTrail(client, self, rgb(config.jumpTrailColor), config.jumpTrailParticleKind);
		}
		if (config.totemFlashEnabled && !config.isSelfHidden("totemFlash")) {
			renderTotemFlash(client, self);
		}
		if (config.sculkPulseEnabled && !config.isSelfHidden("sculkPulse")) {
			renderSculkPulse(client, self);
		}
		if (config.omenAuraEnabled && !config.isSelfHidden("omenAura")) {
			renderOmenAura(client, self);
		}
		if (config.gustAuraEnabled && !config.isSelfHidden("gustAura")) {
			renderGustAura(client, self, rgb(config.gustAuraColor), config.gustAuraParticleKind);
		}
		if (config.ashFallEnabled && !config.isSelfHidden("ashFall")) {
			renderAshFall(client, self, rgb(config.ashFallColor), config.ashFallParticleKind);
		}
		if (config.campfireSmokeEnabled && !config.isSelfHidden("campfireSmoke")) {
			renderCampfireSmoke(client, self, config.campfireSmokeParticleKind);
		}
		if (config.enchantedCritSparkleEnabled && !config.isSelfHidden("enchantedCritSparkle")) {
			renderEnchantedCritSparkle(client, self);
		}
		if (config.dustPlumeTrailEnabled && !config.isSelfHidden("dustPlumeTrail")) {
			renderDustPlumeTrail(client, self);
		}
		if (config.tornadoEnabled && !config.isSelfHidden("tornado")) {
			renderTornado(client, self, rgb(config.tornadoColor), config.tornadoParticleKind);
		}
		if (config.blackHoleEnabled && !config.isSelfHidden("blackHole")) {
			renderBlackHole(client, self, rgb(config.blackHoleColor), config.blackHoleParticleKind);
		}
		if (config.twinVortexEnabled && !config.isSelfHidden("twinVortex")) {
			renderTwinVortex(client, self, rgb(config.twinVortexColor), config.twinVortexParticleKind);
		}
		if (config.chargeUpEnabled && !config.isSelfHidden("chargeUp")) {
			renderChargeUp(client, self, rgb(config.chargeUpColor), config.chargeUpParticleKind);
		}
		if (config.orbitRingsEnabled && !config.isSelfHidden("orbitRings")) {
			renderOrbitRings(client, self, rgb(config.orbitRingsColor), config.orbitRingsParticleKind);
		}
		if (config.lightningAuraEnabled && !config.isSelfHidden("lightningAura")) {
			renderLightningAura(client, self, config.lightningAuraParticle);
		}
		if (config.confettiBurstEnabled && !config.isSelfHidden("confettiBurst")) {
			renderConfettiBurst(client, self, config.confettiBurstParticleKind);
		}
		if (config.mothWingsEnabled && !config.isSelfHidden("mothWings")) {
			renderMothWings(client, self, rgb(config.mothWingsColor), config.mothWingsParticleKind);
		}
		if (config.phoenixWingsEnabled && !config.isSelfHidden("phoenixWings")) {
			renderPhoenixWings(client, self, rgb(config.phoenixWingsColor), config.phoenixWingsParticleKind);
		}
		if (config.voidRiftEnabled && !config.isSelfHidden("voidRift")) {
			renderVoidRift(client, self);
		}
		if (config.starWeaveEnabled && !config.isSelfHidden("starWeave")) {
			renderStarWeave(client, self, config.starWeaveParticle);
		}
		if (config.ascendingSparklesEnabled && !config.isSelfHidden("ascendingSparkles")) {
			renderAscendingSparkles(client, self, config.ascendingSparklesParticle);
		}
		if (config.cometTrailEnabled && !config.isSelfHidden("cometTrail")) {
			renderCometTrail(client, self, config.cometTrailParticle);
		}
		if (config.starVeilEnabled && !config.isSelfHidden("starVeil")) {
			renderStarVeil(client, self, config.starVeilParticle);
		}
		if (config.radiantPulseEnabled && !config.isSelfHidden("radiantPulse")) {
			renderRadiantPulse(client, self, config.radiantPulseParticle);
		}
		if (config.pulsingSphereEnabled && !config.isSelfHidden("pulsingSphere")) {
			renderPulsingSphere(client, self, rgb(config.pulsingSphereColor), config.pulsingSphereParticleKind);
		}
		if (config.scannerEnabled && !config.isSelfHidden("scanner")) {
			renderScanner(client, self, rgb(config.scannerColor), config.scannerParticleKind);
		}
		if (config.physicsCapeEnabled && !config.isSelfHidden("physicsCape")) {
			renderPhysicsCape(client, self, rgb(config.physicsCapeColor), config.physicsCapeParticleKind);
		}
		if (config.cloakEnabled && !config.isSelfHidden("cloak")) {
			renderCloak(client, self, rgb(config.cloakColor), config.cloakParticleKind);
		}
		tickLandingShockwave(client, self, config.landingShockwaveEnabled && !config.isSelfHidden("landingShockwave"), rgb(config.landingShockwaveColor), config.landingShockwaveParticleKind);

		tickOthers(client);
	}

	private static int rgb(Color color) {
		return color.getRGB() & 0xFFFFFF;
	}

	// A "color" cosmetic's dust particle, or a fixed ParticleKind override if one is set - mutually exclusive.
	private static net.minecraft.core.particles.ParticleOptions colorOrParticle(String particleKindName, int rgb, float size) {
		ParticleKind override = ParticleKind.byNameOr(particleKindName, null);
		return override != null ? override.options : new DustParticleOptions(rgb, size);
	}

	// Same idea as colorOrParticle, but the "default" look is a fixed vanilla particle rather than colored dust.
	private static net.minecraft.core.particles.ParticleOptions particleOrDefault(String particleKindName, net.minecraft.core.particles.ParticleOptions defaultOptions) {
		ParticleKind override = ParticleKind.byNameOr(particleKindName, null);
		return override != null ? override.options : defaultOptions;
	}

	// Stable per-player animation offset derived from UUID, so players don't sync up by coincidence.
	private static long tickOffset(AbstractClientPlayer player) {
		return player.getUUID().hashCode() & 0xFFFF;
	}

	// Drift-free per-player rotation phase from world time + tickOffset. speed is in radians/tick.
	private static float phase(Minecraft client, AbstractClientPlayer player, float speed) {
		long gameTime = client.level.getGameTime() + tickOffset(player);
		double raw = (gameTime * (double) speed) % (Math.PI * 2);
		return (float) raw;
	}

	private static void tickOthers(Minecraft client) {
		for (AbstractClientPlayer other : client.level.players()) {
			if (other == client.player) {
				continue;
			}
			UUID uuid = other.getUUID();
			if (!PresenceManager.isModUser(uuid)) {
				continue;
			}

			if (PresenceManager.hasCosmetic(uuid, "halo")) {
				renderHalo(client, other, PresenceManager.getCosmeticColor(uuid, "halo"), PresenceManager.getCosmeticParticleKind(uuid, "halo"));
			}
			if (PresenceManager.hasCosmetic(uuid, "cherryBlossom")) {
				renderCherryBlossom(client, other);
			}
			if (PresenceManager.hasCosmetic(uuid, "rainbowHelix")) {
				renderRainbowHelix(client, other, PresenceManager.getCosmeticColor(uuid, "rainbowHelix"), PresenceManager.getCosmeticParticleKind(uuid, "rainbowHelix"));
			}
			if (PresenceManager.hasCosmetic(uuid, "aura")) {
				renderAura(client, other, PresenceManager.getCosmeticColor(uuid, "aura"), PresenceManager.getCosmeticParticleKind(uuid, "aura"));
			}
			if (PresenceManager.hasCosmetic(uuid, "wave")) {
				renderWave(client, other, PresenceManager.getCosmeticColor(uuid, "wave"), PresenceManager.getCosmeticParticleKind(uuid, "wave"));
			}
			if (PresenceManager.hasCosmetic(uuid, "rainCloud")) {
				renderRainCloud(client, other, PresenceManager.getCosmeticParticleKind(uuid, "rainCloud"));
			}
			if (PresenceManager.hasCosmetic(uuid, "fireRing")) {
				renderFireRing(client, other);
			}
			if (PresenceManager.hasCosmetic(uuid, "starRain")) {
				renderStarRain(client, other);
			}
			if (PresenceManager.hasCosmetic(uuid, "sparkAura")) {
				renderSparkAura(client, other);
			}
			if (PresenceManager.hasCosmetic(uuid, "lissajous")) {
				renderLissajous(client, other, PresenceManager.getCosmeticColor(uuid, "lissajous"), PresenceManager.getCosmeticParticleKind(uuid, "lissajous"));
			}
			if (PresenceManager.hasCosmetic(uuid, "roseCurve")) {
				renderRoseCurve(client, other, PresenceManager.getCosmeticColor(uuid, "roseCurve"), PresenceManager.getCosmeticParticleKind(uuid, "roseCurve"));
			}
			if (PresenceManager.hasCosmetic(uuid, "fireworkBurst")) {
				renderFireworkBurst(client, other);
			}
			if (PresenceManager.hasCosmetic(uuid, "frostAura")) {
				renderFrostAura(client, other, PresenceManager.getCosmeticColor(uuid, "frostAura"), PresenceManager.getCosmeticParticleKind(uuid, "frostAura"));
			}
			if (PresenceManager.hasCosmetic(uuid, "noteMelody")) {
				renderNoteMelody(client, other);
			}
			if (PresenceManager.hasCosmetic(uuid, "portalVortex")) {
				renderPortalVortex(client, other, PresenceManager.getCosmeticColor(uuid, "portalVortex"), PresenceManager.getCosmeticParticleKind(uuid, "portalVortex"));
			}
			if (PresenceManager.hasCosmetic(uuid, "heartTrail")) {
				renderHeartTrail(client, other);
			}
			if (PresenceManager.hasCosmetic(uuid, "spiralGalaxy")) {
				renderSpiralGalaxy(client, other, PresenceManager.getCosmeticColor(uuid, "spiralGalaxy"), PresenceManager.getCosmeticParticleKind(uuid, "spiralGalaxy"));
			}
			if (PresenceManager.hasCosmetic(uuid, "jumpTrail")) {
				renderJumpTrail(client, other, PresenceManager.getCosmeticColor(uuid, "jumpTrail"), PresenceManager.getCosmeticParticleKind(uuid, "jumpTrail"));
			}
			if (PresenceManager.hasCosmetic(uuid, "totemFlash")) {
				renderTotemFlash(client, other);
			}
			if (PresenceManager.hasCosmetic(uuid, "sculkPulse")) {
				renderSculkPulse(client, other);
			}
			if (PresenceManager.hasCosmetic(uuid, "omenAura")) {
				renderOmenAura(client, other);
			}
			if (PresenceManager.hasCosmetic(uuid, "gustAura")) {
				renderGustAura(client, other, PresenceManager.getCosmeticColor(uuid, "gustAura"), PresenceManager.getCosmeticParticleKind(uuid, "gustAura"));
			}
			if (PresenceManager.hasCosmetic(uuid, "ashFall")) {
				renderAshFall(client, other, PresenceManager.getCosmeticColor(uuid, "ashFall"), PresenceManager.getCosmeticParticleKind(uuid, "ashFall"));
			}
			if (PresenceManager.hasCosmetic(uuid, "campfireSmoke")) {
				renderCampfireSmoke(client, other, PresenceManager.getCosmeticParticleKind(uuid, "campfireSmoke"));
			}
			if (PresenceManager.hasCosmetic(uuid, "enchantedCritSparkle")) {
				renderEnchantedCritSparkle(client, other);
			}
			if (PresenceManager.hasCosmetic(uuid, "dustPlumeTrail")) {
				renderDustPlumeTrail(client, other);
			}
			if (PresenceManager.hasCosmetic(uuid, "tornado")) {
				renderTornado(client, other, PresenceManager.getCosmeticColor(uuid, "tornado"), PresenceManager.getCosmeticParticleKind(uuid, "tornado"));
			}
			if (PresenceManager.hasCosmetic(uuid, "blackHole")) {
				renderBlackHole(client, other, PresenceManager.getCosmeticColor(uuid, "blackHole"), PresenceManager.getCosmeticParticleKind(uuid, "blackHole"));
			}
			if (PresenceManager.hasCosmetic(uuid, "twinVortex")) {
				renderTwinVortex(client, other, PresenceManager.getCosmeticColor(uuid, "twinVortex"), PresenceManager.getCosmeticParticleKind(uuid, "twinVortex"));
			}
			if (PresenceManager.hasCosmetic(uuid, "chargeUp")) {
				renderChargeUp(client, other, PresenceManager.getCosmeticColor(uuid, "chargeUp"), PresenceManager.getCosmeticParticleKind(uuid, "chargeUp"));
			}
			if (PresenceManager.hasCosmetic(uuid, "orbitRings")) {
				renderOrbitRings(client, other, PresenceManager.getCosmeticColor(uuid, "orbitRings"), PresenceManager.getCosmeticParticleKind(uuid, "orbitRings"));
			}
			if (PresenceManager.hasCosmetic(uuid, "lightningAura")) {
				renderLightningAura(client, other, PresenceManager.getCosmeticParticleKind(uuid, "lightningAura"));
			}
			if (PresenceManager.hasCosmetic(uuid, "confettiBurst")) {
				renderConfettiBurst(client, other, PresenceManager.getCosmeticParticleKind(uuid, "confettiBurst"));
			}
			if (PresenceManager.hasCosmetic(uuid, "mothWings")) {
				renderMothWings(client, other, PresenceManager.getCosmeticColor(uuid, "mothWings"), PresenceManager.getCosmeticParticleKind(uuid, "mothWings"));
			}
			if (PresenceManager.hasCosmetic(uuid, "phoenixWings")) {
				renderPhoenixWings(client, other, PresenceManager.getCosmeticColor(uuid, "phoenixWings"), PresenceManager.getCosmeticParticleKind(uuid, "phoenixWings"));
			}
			if (PresenceManager.hasCosmetic(uuid, "voidRift")) {
				renderVoidRift(client, other);
			}
			if (PresenceManager.hasCosmetic(uuid, "starWeave")) {
				renderStarWeave(client, other, PresenceManager.getCosmeticParticleKind(uuid, "starWeave"));
			}
			if (PresenceManager.hasCosmetic(uuid, "ascendingSparkles")) {
				renderAscendingSparkles(client, other, PresenceManager.getCosmeticParticleKind(uuid, "ascendingSparkles"));
			}
			if (PresenceManager.hasCosmetic(uuid, "cometTrail")) {
				renderCometTrail(client, other, PresenceManager.getCosmeticParticleKind(uuid, "cometTrail"));
			}
			if (PresenceManager.hasCosmetic(uuid, "starVeil")) {
				renderStarVeil(client, other, PresenceManager.getCosmeticParticleKind(uuid, "starVeil"));
			}
			if (PresenceManager.hasCosmetic(uuid, "radiantPulse")) {
				renderRadiantPulse(client, other, PresenceManager.getCosmeticParticleKind(uuid, "radiantPulse"));
			}
			if (PresenceManager.hasCosmetic(uuid, "pulsingSphere")) {
				renderPulsingSphere(client, other, PresenceManager.getCosmeticColor(uuid, "pulsingSphere"), PresenceManager.getCosmeticParticleKind(uuid, "pulsingSphere"));
			}
			if (PresenceManager.hasCosmetic(uuid, "scanner")) {
				renderScanner(client, other, PresenceManager.getCosmeticColor(uuid, "scanner"), PresenceManager.getCosmeticParticleKind(uuid, "scanner"));
			}
			if (PresenceManager.hasCosmetic(uuid, "physicsCape")) {
				renderPhysicsCape(client, other, PresenceManager.getCosmeticColor(uuid, "physicsCape"), PresenceManager.getCosmeticParticleKind(uuid, "physicsCape"));
			}
			if (PresenceManager.hasCosmetic(uuid, "cloak")) {
				renderCloak(client, other, PresenceManager.getCosmeticColor(uuid, "cloak"), PresenceManager.getCosmeticParticleKind(uuid, "cloak"));
			}
			tickLandingShockwave(client, other, PresenceManager.hasCosmetic(uuid, "landingShockwave"), PresenceManager.getCosmeticColor(uuid, "landingShockwave"), PresenceManager.getCosmeticParticleKind(uuid, "landingShockwave"));
		}
	}

	private static void renderHalo(Minecraft client, AbstractClientPlayer player, int rgb, String particleKindName) {
		float haloAngle = phase(client, player, 0.2F);

		double radius = 0.7;
		double x = player.getX() + Math.cos(haloAngle) * radius;
		double z = player.getZ() + Math.sin(haloAngle) * radius;
		double y = player.getY() + player.getBbHeight() + 0.3;

		ParticleKind kindOverride = ParticleKind.byNameOr(particleKindName, null);
		glowyDust(client, rgb, x, y, z, 1.0F, EssentialsConfig.get().haloGlow, kindOverride);
	}

	// Colored dust, or vanilla's GLOW if glowing - GLOW ignores the chosen color, it isn't colorable.
	private static void glowyDust(Minecraft client, int rgb, double x, double y, double z, float size, boolean glowing) {
		glowyDust(client, rgb, x, y, z, size, glowing, null);
	}

	// Same as the 6-arg overload, but kindOverride (if set) wins over both color and glow.
	private static void glowyDust(Minecraft client, int rgb, double x, double y, double z, float size, boolean glowing, ParticleKind kindOverride) {
		if (kindOverride != null) {
			client.level.addParticle(kindOverride.options, x, y, z, 0, 0, 0);
			return;
		}
		if (glowing) {
			client.level.addParticle(ParticleTypes.GLOW, x, y, z, 0, 0, 0);
			return;
		}
		client.level.addParticle(new DustParticleOptions(rgb, size), x, y, z, 0, 0, 0);
	}

	private static void renderCherryBlossom(Minecraft client, AbstractClientPlayer player) {
		RandomSource random = player.getRandom();
		if (random.nextFloat() > 0.3F) {
			return;
		}

		double x = player.getRandomX(1.5);
		double y = player.getY() + player.getBbHeight() + 0.5 + random.nextDouble();
		double z = player.getRandomZ(1.5);
		ParticleKind kind = ParticleKind.byNameOr(EssentialsConfig.get().cherryBlossomParticle, ParticleKind.CHERRY_BLOSSOM);
		client.level.addParticle(kind.options, x, y, z, 0, -0.05, 0);
	}

	private static final int HELIX_STRANDS = 2;

	// Colored particle strands spiraling around your whole body like a DNA helix.
	private static void renderRainbowHelix(Minecraft client, AbstractClientPlayer player, int rgb, String particleKindName) {
		float helixAngle = phase(client, player, 0.12F);

		double radius = 0.65;
		float bodyHeight = player.getBbHeight();

		for (int strand = 0; strand < HELIX_STRANDS; strand++) {
			float angle = helixAngle + strand * (float) (Math.PI * 2 / HELIX_STRANDS);
			double x = player.getX() + Math.cos(angle) * radius;
			double z = player.getZ() + Math.sin(angle) * radius;

			// Each strand's height phase is offset so together they cover the whole body top-to-bottom.
			float heightPhase = helixAngle * 1.5F + strand * (float) (Math.PI / 2);
			double y = player.getY() + (Math.sin(heightPhase) * 0.5 + 0.5) * bodyHeight;

			client.level.addParticle(colorOrParticle(particleKindName, rgb, 1.1F), x, y, z, 0, 0, 0);
		}
	}

	// A slow, wide double ring of particles orbiting around your body at chest height.
	private static void renderAura(Minecraft client, AbstractClientPlayer player, int rgb, String particleKindName) {
		float auraAngle = phase(client, player, 0.1F);

		double radius = 0.9;
		float bodyMid = player.getBbHeight() * 0.5F;

		boolean glowing = EssentialsConfig.get().auraGlow;
		ParticleKind kindOverride = ParticleKind.byNameOr(particleKindName, null);
		for (int ring = 0; ring < 2; ring++) {
			float angle = auraAngle * (ring == 0 ? 1F : -1F) + ring * (float) Math.PI;
			double x = player.getX() + Math.cos(angle) * radius;
			double z = player.getZ() + Math.sin(angle) * radius;
			double y = player.getY() + bodyMid + Math.sin(angle * 2F) * 0.3;
			glowyDust(client, rgb, x, y, z, 1.0F, glowing, kindOverride);
		}
	}

	private static final class Pulse {
		final double originX, originZ, originY;
		int age = 0;

		Pulse(double originX, double originY, double originZ) {
			this.originX = originX;
			this.originY = originY;
			this.originZ = originZ;
		}
	}

	private static final List<Pulse> wavePulses = new ArrayList<>();
	private static final Map<UUID, Integer> waveSpawnTimers = new HashMap<>();
	private static final int WAVE_SPAWN_INTERVAL_TICKS = 25;
	private static final int WAVE_DURATION_TICKS = 22;
	private static final float WAVE_MAX_RADIUS = 2.6F;

	// Shockwave rings expand from a fixed spawn point, not the player's current position, so they don't drag while moving.
	private static void renderWave(Minecraft client, AbstractClientPlayer player, int rgb, String particleKindName) {
		UUID uuid = player.getUUID();
		int timer = waveSpawnTimers.getOrDefault(uuid, 0) + 1;
		if (timer >= WAVE_SPAWN_INTERVAL_TICKS) {
			timer = 0;
			wavePulses.add(new Pulse(player.getX(), player.getY(), player.getZ()));
		}
		waveSpawnTimers.put(uuid, timer);

		int points = 28;
		wavePulses.removeIf(pulse -> {
			pulse.age++;
			float progress = (float) pulse.age / WAVE_DURATION_TICKS;
			float radius = progress * WAVE_MAX_RADIUS;
			double bounce = Math.sin(progress * Math.PI) * 0.4;

			for (int i = 0; i < points; i++) {
				float angle = (float) (Math.PI * 2 * i / points);
				double x = pulse.originX + Math.cos(angle) * radius;
				double z = pulse.originZ + Math.sin(angle) * radius;
				double y = pulse.originY + 0.1 + bounce;
				client.level.addParticle(colorOrParticle(particleKindName, rgb, 1.2F), x, y, z, 0, 0, 0);
			}
			return pulse.age >= WAVE_DURATION_TICKS;
		});
	}

	// A volumetric cloud hovering above the player's head, raining from inside its own footprint.
	private static void renderRainCloud(Minecraft client, AbstractClientPlayer player, String particleKindName) {
		RandomSource random = player.getRandom();
		double cloudY = player.getY() + player.getBbHeight() + 1.3;
		double cloudRadius = 1.8;

		for (int i = 0; i < 3; i++) {
			if (random.nextFloat() < 0.6F) {
				double angle = random.nextDouble() * Math.PI * 2;
				double dist = random.nextDouble() * cloudRadius;
				double cx = player.getX() + Math.cos(angle) * dist;
				double cz = player.getZ() + Math.sin(angle) * dist;
				double cy = cloudY + (random.nextDouble() - 0.5) * 0.6;
				client.level.addParticle(particleOrDefault(particleKindName, ParticleTypes.CLOUD), cx, cy, cz, 0, 0, 0);
			}
		}
		// Rain scattered throughout the cloud's own footprint, not a separate small area below it.
		for (int i = 0; i < 2; i++) {
			if (random.nextFloat() < 0.6F) {
				double angle = random.nextDouble() * Math.PI * 2;
				double dist = random.nextDouble() * cloudRadius * 0.9;
				double rx = player.getX() + Math.cos(angle) * dist;
				double rz = player.getZ() + Math.sin(angle) * dist;
				double ry = cloudY + (random.nextDouble() - 0.5) * 0.5;
				client.level.addParticle(particleOrDefault(particleKindName, ParticleTypes.RAIN), rx, ry, rz, 0, -0.2, 0);
			}
		}
	}

	// A rotating ring of flame particles at your feet.
	private static void renderFireRing(Minecraft client, AbstractClientPlayer player) {
		float fireRingAngle = phase(client, player, 0.25F);

		double radius = 0.6;
		int flames = 6;
		ParticleKind kind = ParticleKind.byNameOr(EssentialsConfig.get().fireRingParticle, ParticleKind.FLAME);
		for (int i = 0; i < flames; i++) {
			float angle = fireRingAngle + i * (float) (Math.PI * 2 / flames);
			double x = player.getX() + Math.cos(angle) * radius;
			double z = player.getZ() + Math.sin(angle) * radius;
			client.level.addParticle(kind.options, x, player.getY() + 0.1, z, 0, 0.01, 0);
		}
	}

	// Sparkling particles drifting slowly down from above your head.
	private static void renderStarRain(Minecraft client, AbstractClientPlayer player) {
		RandomSource random = player.getRandom();
		if (random.nextFloat() > 0.5F) {
			return;
		}
		double x = player.getRandomX(1.0);
		double y = player.getY() + player.getBbHeight() + 1.5;
		double z = player.getRandomZ(1.0);
		ParticleKind kind = ParticleKind.byNameOr(EssentialsConfig.get().starRainParticle, ParticleKind.SPARKLE);
		client.level.addParticle(kind.options, x, y, z, 0, -0.03, 0);
	}

	// Occasional electric sparks crackling randomly around your body.
	private static void renderSparkAura(Minecraft client, AbstractClientPlayer player) {
		RandomSource random = player.getRandom();
		if (random.nextFloat() > 0.4F) {
			return;
		}
		double x = player.getRandomX(2.2);
		double y = player.getY() + random.nextDouble() * player.getBbHeight();
		double z = player.getRandomZ(2.2);
		ParticleKind kind = ParticleKind.byNameOr(EssentialsConfig.get().sparkAuraParticle, ParticleKind.SPARK);
		client.level.addParticle(kind.options, x, y, z, 0, 0, 0);
	}

	// A 3D Lissajous curve - three sine waves on different axes weave a constantly-shifting knot.
	private static void renderLissajous(Minecraft client, AbstractClientPlayer player, int rgb, String particleKindName) {
		float lissajousT = phase(client, player, 0.05F);

		double radiusXZ = 1.1;
		double radiusY = 0.9;
		float bodyMid = player.getBbHeight() * 0.5F;

		// Two points traced simultaneously (offset phase) on x=sin(3t), y=sin(2t), z=sin(4t) - a
		// classic Lissajous knot - for a denser, more intricate weave than a single trace point.
		for (int i = 0; i < 2; i++) {
			float t = lissajousT + i * (float) Math.PI;
			double x = player.getX() + Math.sin(3 * t) * radiusXZ;
			double z = player.getZ() + Math.sin(4 * t + Math.PI / 2) * radiusXZ;
			double y = player.getY() + bodyMid + Math.sin(2 * t) * radiusY;

			client.level.addParticle(colorOrParticle(particleKindName, rgb, 1.2F), x, y, z, 0, 0, 0);
		}
	}

	private static final double ROSE_K_PERIOD_TICKS = 5.0 / 0.003; // matches the old "roseK += 0.003F, wraps 2..7" ramp

	// A rose/rhodonea curve (r = radius * cos(k * theta)), petal count (k) slowly morphing while it spins.
	private static void renderRoseCurve(Minecraft client, AbstractClientPlayer player, int rgb, String particleKindName) {
		float roseAngle = phase(client, player, 0.04F);
		long gameTime = client.level.getGameTime() + tickOffset(player);
		float roseK = (float) (2.0 + (gameTime % ROSE_K_PERIOD_TICKS) / ROSE_K_PERIOD_TICKS * 5.0);

		double radius = 1.6;
		float bodyMid = player.getBbHeight() * 0.5F;
		int points = 60;

		// Keep the chosen color's hue/saturation but vary its brightness per-point for a subtle
		// shimmer instead of a completely flat wash of one color.
		float[] hsb = new float[3];
		Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, hsb);
		float baseBrightness = Math.max(0.6F, hsb[2]);

		for (int i = 0; i < points; i++) {
			float theta = (float) (Math.PI * 2 * i / points) + roseAngle;
			double r = radius * Math.cos(roseK * theta);
			double x = player.getX() + r * Math.cos(theta);
			double z = player.getZ() + r * Math.sin(theta);
			double y = player.getY() + bodyMid + Math.sin(theta * 2 + roseAngle * 3) * 0.8;

			float brightness = baseBrightness * (0.75F + 0.25F * (float) Math.sin(theta * 3 + roseAngle * 2));
			int pointRgb = Color.HSBtoRGB(hsb[0], hsb[1], brightness) & 0xFFFFFF;
			client.level.addParticle(colorOrParticle(particleKindName, pointRgb, 1.1F), x, y, z, 0, 0, 0);
		}
	}

	private static final Map<UUID, Boolean> wasOnGround = new HashMap<>();
	private static final Map<UUID, Integer> landingShockwaveAge = new HashMap<>();
	private static final Map<UUID, double[]> landingOrigin = new HashMap<>();
	private static final int LANDING_SHOCKWAVE_DURATION_TICKS = 14;
	private static final float LANDING_SHOCKWAVE_MAX_RADIUS = 3.5F;

	// One-shot expanding ring on landing, anchored to the landing spot rather than the current position.
	private static void tickLandingShockwave(Minecraft client, AbstractClientPlayer player, boolean enabled, int rgb, String particleKindName) {
		UUID uuid = player.getUUID();
		boolean onGround = player.onGround();
		boolean previouslyOnGround = wasOnGround.getOrDefault(uuid, true);
		if (enabled && onGround && !previouslyOnGround) {
			landingShockwaveAge.put(uuid, 0);
			landingOrigin.put(uuid, new double[]{player.getX(), player.getY(), player.getZ()});
		}
		wasOnGround.put(uuid, onGround);

		Integer age = landingShockwaveAge.get(uuid);
		if (age == null || age < 0) {
			return;
		}
		double[] origin = landingOrigin.get(uuid);

		float progress = (float) age / LANDING_SHOCKWAVE_DURATION_TICKS;
		float radius = progress * LANDING_SHOCKWAVE_MAX_RADIUS;
		int points = 28;
		for (int i = 0; i < points; i++) {
			float angle = (float) (Math.PI * 2 * i / points);
			double x = origin[0] + Math.cos(angle) * radius;
			double z = origin[2] + Math.sin(angle) * radius;
			client.level.addParticle(colorOrParticle(particleKindName, rgb, 1.4F * (1F - progress) + 0.4F), x, origin[1] + 0.1, z, 0, 0, 0);
		}

		age++;
		landingShockwaveAge.put(uuid, age > LANDING_SHOCKWAVE_DURATION_TICKS ? -1 : age);
	}

	// Occasional sparkle bursts overhead, like distant celebratory fireworks.
	private static void renderFireworkBurst(Minecraft client, AbstractClientPlayer player) {
		RandomSource random = player.getRandom();
		if (random.nextFloat() > 0.03F) {
			return;
		}
		double cx = player.getX();
		double cy = player.getY() + player.getBbHeight() + 2.0;
		double cz = player.getZ();
		ParticleKind kind = ParticleKind.byNameOr(EssentialsConfig.get().fireworkBurstParticle, ParticleKind.FIREWORK);
		for (int i = 0; i < 16; i++) {
			double yaw = random.nextDouble() * Math.PI * 2;
			double pitch = random.nextDouble() * Math.PI;
			double speed = 0.15 + random.nextDouble() * 0.2;
			double vx = Math.cos(yaw) * Math.sin(pitch) * speed;
			double vy = Math.cos(pitch) * speed;
			double vz = Math.sin(yaw) * Math.sin(pitch) * speed;
			client.level.addParticle(kind.options, cx, cy, cz, vx, vy, vz);
		}
	}

	// Colored particles swirling tight and close around your body, like a personal cold aura.
	private static void renderFrostAura(Minecraft client, AbstractClientPlayer player, int rgb, String particleKindName) {
		float frostAngle = phase(client, player, 0.22F);
		double radius = 0.5;
		int flakes = 5;
		for (int i = 0; i < flakes; i++) {
			float angle = frostAngle + i * (float) (Math.PI * 2 / flakes);
			double x = player.getX() + Math.cos(angle) * radius;
			double z = player.getZ() + Math.sin(angle) * radius;
			double y = player.getY() + ((frostAngle * 0.3 + i) % player.getBbHeight());
			client.level.addParticle(colorOrParticle(particleKindName, rgb, 1.0F), x, y, z, 0, -0.01, 0);
		}
	}

	// Musical notes floating up above your head occasionally.
	private static void renderNoteMelody(Minecraft client, AbstractClientPlayer player) {
		RandomSource random = player.getRandom();
		if (random.nextFloat() > 0.08F) {
			return;
		}
		double x = player.getRandomX(0.5);
		double y = player.getY() + player.getBbHeight() + 0.3;
		double z = player.getRandomZ(0.5);
		ParticleKind kind = ParticleKind.byNameOr(EssentialsConfig.get().noteMelodyParticle, ParticleKind.NOTE);
		client.level.addParticle(kind.options, x, y, z, 0, 0, 0);
	}

	// A fast-spinning double vortex of colored particles around your whole body.
	private static void renderPortalVortex(Minecraft client, AbstractClientPlayer player, int rgb, String particleKindName) {
		float portalAngle = phase(client, player, 0.5F);
		double radius = 0.8;
		float bodyHeight = player.getBbHeight();
		for (int i = 0; i < 2; i++) {
			float angle = portalAngle + i * (float) Math.PI;
			double x = player.getX() + Math.cos(angle) * radius;
			double z = player.getZ() + Math.sin(angle) * radius;
			double y = player.getY() + ((portalAngle * 0.4 + i * 0.5) % 1.0) * bodyHeight;
			client.level.addParticle(colorOrParticle(particleKindName, rgb, 1.1F), x, y, z, 0, 0, 0);
		}
	}

	// Floating hearts drifting up occasionally - a playful, wholesome cosmetic.
	private static void renderHeartTrail(Minecraft client, AbstractClientPlayer player) {
		RandomSource random = player.getRandom();
		if (random.nextFloat() > 0.06F) {
			return;
		}
		double x = player.getRandomX(0.6);
		double y = player.getY() + player.getBbHeight() + 0.3;
		double z = player.getRandomZ(0.6);
		ParticleKind kind = ParticleKind.byNameOr(EssentialsConfig.get().heartTrailParticle, ParticleKind.HEART);
		client.level.addParticle(kind.options, x, y, z, 0, 0.05, 0);
	}

	private static final int GALAXY_ARMS = 3;
	private static final int GALAXY_POINTS_PER_ARM = 10;

	// Multi-arm spiral, radius growing linearly from feet to head - angle must stay bounded, an unwrapped angle loses float precision over time.
	private static void renderSpiralGalaxy(Minecraft client, AbstractClientPlayer player, int rgb, String particleKindName) {
		float galaxyRot = phase(client, player, 0.12F);

		float bodyHeight = player.getBbHeight();
		double maxRadius = 1.3;

		for (int arm = 0; arm < GALAXY_ARMS; arm++) {
			float armPhase = arm * (float) (Math.PI * 2 / GALAXY_ARMS);
			for (int p = 0; p < GALAXY_POINTS_PER_ARM; p++) {
				float t = (float) p / (GALAXY_POINTS_PER_ARM - 1);
				double angle = galaxyRot + armPhase + t * Math.PI * 2.5;
				double r = maxRadius * t;
				double x = player.getX() + r * Math.cos(angle);
				double z = player.getZ() + r * Math.sin(angle);
				double y = player.getY() + t * bodyHeight;

				client.level.addParticle(colorOrParticle(particleKindName, rgb, 1.1F), x, y, z, 0, 0, 0);
			}
		}
	}

	// While airborne (jumping/falling), leaves a particle trail along the player's actual arc.
	private static void renderJumpTrail(Minecraft client, AbstractClientPlayer player, int rgb, String particleKindName) {
		if (!player.onGround()) {
			client.level.addParticle(colorOrParticle(particleKindName, rgb, 1.0F), player.getX(), player.getY() + 0.1, player.getZ(), 0, 0, 0);
		}
	}

	// Occasional Totem-of-Undying-style flash burst above your head.
	private static void renderTotemFlash(Minecraft client, AbstractClientPlayer player) {
		RandomSource random = player.getRandom();
		if (random.nextFloat() > 0.02F) {
			return;
		}
		double cx = player.getX();
		double cy = player.getY() + player.getBbHeight() + 1.5;
		double cz = player.getZ();
		ParticleKind kind = ParticleKind.byNameOr(EssentialsConfig.get().totemFlashParticle, ParticleKind.TOTEM);
		for (int i = 0; i < 10; i++) {
			double vx = (random.nextDouble() - 0.5) * 0.3;
			double vy = random.nextDouble() * 0.3;
			double vz = (random.nextDouble() - 0.5) * 0.3;
			client.level.addParticle(kind.options, cx, cy, cz, vx, vy, vz);
		}
	}

	// A dark, spooky pulsing ring of sculk particles at your feet.
	private static void renderSculkPulse(Minecraft client, AbstractClientPlayer player) {
		float sculkAngle = phase(client, player, 0.15F);
		double radius = 0.7 + Math.sin(sculkAngle) * 0.2;
		int points = 8;
		ParticleKind kind = ParticleKind.byNameOr(EssentialsConfig.get().sculkPulseParticle, ParticleKind.SCULK);
		for (int i = 0; i < points; i++) {
			float angle = i * (float) (Math.PI * 2 / points);
			double x = player.getX() + Math.cos(angle) * radius;
			double z = player.getZ() + Math.sin(angle) * radius;
			client.level.addParticle(kind.options, x, player.getY() + 0.1, z, 0, 0, 0);
		}
	}

	// An ominous swirling aura, like the Raid/Trial Omen status effects.
	private static void renderOmenAura(Minecraft client, AbstractClientPlayer player) {
		RandomSource random = player.getRandom();
		if (random.nextFloat() > 0.3F) {
			return;
		}
		double x = player.getRandomX(0.9);
		double y = player.getY() + random.nextDouble() * player.getBbHeight();
		double z = player.getRandomZ(0.9);
		ParticleKind kind = ParticleKind.byNameOr(EssentialsConfig.get().omenAuraParticle, ParticleKind.OMEN);
		client.level.addParticle(kind.options, x, y, z, 0, 0.02, 0);
	}

	// Colored wind gusts swirling around your body, like you're standing in your own personal breeze.
	private static void renderGustAura(Minecraft client, AbstractClientPlayer player, int rgb, String particleKindName) {
		float gustAngle = phase(client, player, 0.35F);
		double radius = 1.0;
		float bodyMid = player.getBbHeight() * 0.5F;
		for (int i = 0; i < 3; i++) {
			float angle = gustAngle + i * (float) (Math.PI * 2 / 3);
			double x = player.getX() + Math.cos(angle) * radius;
			double z = player.getZ() + Math.sin(angle) * radius;
			client.level.addParticle(colorOrParticle(particleKindName, rgb, 1.2F), x, player.getY() + bodyMid, z, 0, 0, 0);
		}
	}

	// Colored fine particles gently falling around you, like ash near a volcano.
	private static void renderAshFall(Minecraft client, AbstractClientPlayer player, int rgb, String particleKindName) {
		RandomSource random = player.getRandom();
		if (random.nextFloat() > 0.4F) {
			return;
		}
		double x = player.getRandomX(1.6);
		double y = player.getY() + player.getBbHeight() + 0.8 + random.nextDouble();
		double z = player.getRandomZ(1.6);
		client.level.addParticle(colorOrParticle(particleKindName, rgb, 0.9F), x, y, z, 0, -0.03, 0);
	}

	// Cozy smoke wisps trailing gently from your feet.
	private static void renderCampfireSmoke(Minecraft client, AbstractClientPlayer player, String particleKindName) {
		RandomSource random = player.getRandom();
		if (random.nextFloat() > 0.2F) {
			return;
		}
		double x = player.getRandomX(0.4);
		double y = player.getY() + 0.1;
		double z = player.getRandomZ(0.4);
		var defaultType = random.nextBoolean() ? ParticleTypes.CAMPFIRE_COSY_SMOKE : ParticleTypes.CAMPFIRE_SIGNAL_SMOKE;
		client.level.addParticle(particleOrDefault(particleKindName, defaultType), x, y, z, 0, 0.03, 0);
	}

	// Enchanted-hit sparkles bursting around you occasionally, like a magic critical hit.
	private static void renderEnchantedCritSparkle(Minecraft client, AbstractClientPlayer player) {
		RandomSource random = player.getRandom();
		if (random.nextFloat() > 0.12F) {
			return;
		}
		double x = player.getRandomX(1.0);
		double y = player.getY() + random.nextDouble() * player.getBbHeight();
		double z = player.getRandomZ(1.0);
		ParticleKind kind = ParticleKind.byNameOr(EssentialsConfig.get().enchantedCritSparkleParticle, ParticleKind.ENCHANTED_CRIT);
		client.level.addParticle(kind.options, x, y, z, 0, 0, 0);
	}

	// A trailing plume of dust drifting up behind you, like sneaking through smoke.
	private static void renderDustPlumeTrail(Minecraft client, AbstractClientPlayer player) {
		RandomSource random = player.getRandom();
		if (random.nextFloat() > 0.15F) {
			return;
		}
		double x = player.getRandomX(0.5);
		double y = player.getY() + 0.1;
		double z = player.getRandomZ(0.5);
		ParticleKind kind = ParticleKind.byNameOr(EssentialsConfig.get().dustPlumeTrailParticle, ParticleKind.DUST_PLUME);
		client.level.addParticle(kind.options, x, y, z, 0, 0.03, 0);
	}

	// A widening funnel of particles from your feet up to above your head, like a personal tornado.
	private static void renderTornado(Minecraft client, AbstractClientPlayer player, int rgb, String particleKindName) {
		float tornadoAngle = phase(client, player, 0.4F);
		float bodyHeight = player.getBbHeight();
		int layers = 8;
		for (int layer = 0; layer < layers; layer++) {
			float t = (float) layer / (layers - 1);
			double radius = 0.15 + t * 0.9;
			float angle = tornadoAngle + t * 10F;
			double x = player.getX() + Math.cos(angle) * radius;
			double z = player.getZ() + Math.sin(angle) * radius;
			double y = player.getY() + t * bodyHeight * 1.3;
			client.level.addParticle(colorOrParticle(particleKindName, rgb, 1.0F), x, y, z, 0, 0, 0);
		}
	}

	private static final int BLACK_HOLE_RINGS = 4;

	// A black particle sphere at the center, with colored rings orbiting it - inner rings spin faster, like an accretion disk.
	private static void renderBlackHole(Minecraft client, AbstractClientPlayer player, int rgb, String particleKindName) {
		float bodyMid = player.getBbHeight() * 0.5F;
		double centerY = player.getY() + bodyMid;

		int coreLatitudes = 4;
		int corePointsPerLat = 5;
		double coreRadius = 0.32;
		for (int lat = 0; lat < coreLatitudes; lat++) {
			double latAngle = Math.PI * (lat + 0.5) / coreLatitudes - Math.PI / 2;
			double ringRadius = coreRadius * Math.cos(latAngle);
			double yOffset = coreRadius * Math.sin(latAngle);
			for (int i = 0; i < corePointsPerLat; i++) {
				double angle = i * (Math.PI * 2 / corePointsPerLat) + lat * 0.4;
				double x = player.getX() + Math.cos(angle) * ringRadius;
				double z = player.getZ() + Math.sin(angle) * ringRadius;
				client.level.addParticle(colorOrParticle(particleKindName, 0x0A0A0A, 1.1F), x, centerY + yOffset, z, 0, 0, 0);
			}
		}

		for (int ring = 0; ring < BLACK_HOLE_RINGS; ring++) {
			double radius = 0.55 + ring * 0.3;
			// Slow enough that the rotation is actually visible frame-to-frame instead of blurring
			// into a static ring - inner rings still noticeably faster than outer ones.
			float speed = 0.05F / (float) radius;
			float ringAngle = phase(client, player, speed);
			int pointsInRing = 3 + ring;
			for (int i = 0; i < pointsInRing; i++) {
				float angle = ringAngle + i * (float) (Math.PI * 2 / pointsInRing);
				double x = player.getX() + Math.cos(angle) * radius;
				double z = player.getZ() + Math.sin(angle) * radius;
				double y = centerY + Math.sin(angle * 2F + ring) * 0.08;
				client.level.addParticle(colorOrParticle(particleKindName, rgb, 1.0F), x, y, z, 0, 0, 0);
			}
		}
	}

	// Two tight, contra-rotating spirals weaving around your whole body.
	private static void renderTwinVortex(Minecraft client, AbstractClientPlayer player, int rgb, String particleKindName) {
		float twinVortexAngle = phase(client, player, 0.45F);
		float bodyHeight = player.getBbHeight();
		int pointsPerArm = 6;
		for (int arm = 0; arm < 2; arm++) {
			float direction = arm == 0 ? 1F : -1F;
			for (int p = 0; p < pointsPerArm; p++) {
				float t = (float) p / (pointsPerArm - 1);
				float angle = twinVortexAngle * direction + t * (float) Math.PI * 3;
				double radius = 0.3 + t * 0.4;
				double x = player.getX() + Math.cos(angle) * radius;
				double z = player.getZ() + Math.sin(angle) * radius;
				double y = player.getY() + t * bodyHeight;

				client.level.addParticle(colorOrParticle(particleKindName, rgb, 1.0F), x, y, z, 0, 0, 0);
			}
		}
	}

	private static final Map<UUID, Integer> chargeUpCycleTicks = new HashMap<>();
	private static final int CHARGE_PULL_TICKS = 36;
	private static final int CHARGE_BURST_TICKS = 8;
	private static final int CHARGE_SETTLE_TICKS = 24;
	private static final int CHARGE_CYCLE_TICKS = CHARGE_PULL_TICKS + CHARGE_BURST_TICKS + CHARGE_SETTLE_TICKS;

	// A ring of particles that pulls in tight to your chest, bursts back out, wobbles briefly, then repeats - like charging up energy.
	private static void renderChargeUp(Minecraft client, AbstractClientPlayer player, int rgb, String particleKindName) {
		float chargeUpAngle = phase(client, player, 0.2F);
		int cycleTick = chargeUpCycleTicks.getOrDefault(player.getUUID(), 0);
		chargeUpCycleTicks.put(player.getUUID(), (cycleTick + 1) % CHARGE_CYCLE_TICKS);

		int points = 8;
		double maxRadius = 1.1;
		double footY = player.getY() + 0.1;
		double chestY = player.getY() + player.getBbHeight() * 0.6;

		if (cycleTick < CHARGE_PULL_TICKS) {
			// Contract: ring shrinks from full radius down to the player's center, rising from feet to chest.
			float progress = (float) cycleTick / CHARGE_PULL_TICKS;
			double radius = maxRadius * (1.0 - progress);
			double y = footY + (chestY - footY) * progress;
			for (int i = 0; i < points; i++) {
				float angle = chargeUpAngle + i * (float) (Math.PI * 2 / points);
				double x = player.getX() + Math.cos(angle) * radius;
				double z = player.getZ() + Math.sin(angle) * radius;
				double vx = (player.getX() - x) * 0.35;
				double vz = (player.getZ() - z) * 0.35;
				client.level.addParticle(colorOrParticle(particleKindName, rgb, 1.1F), x, y, z, vx, 0.02, vz);
			}
		} else if (cycleTick < CHARGE_PULL_TICKS + CHARGE_BURST_TICKS) {
			// Burst: rapid outward release from the chest, much faster than the contraction was.
			int burstTick = cycleTick - CHARGE_PULL_TICKS;
			float progress = (float) burstTick / CHARGE_BURST_TICKS;
			double radius = maxRadius * progress;
			for (int i = 0; i < points; i++) {
				float angle = chargeUpAngle * 2F + i * (float) (Math.PI * 2 / points);
				double x = player.getX() + Math.cos(angle) * radius;
				double z = player.getZ() + Math.sin(angle) * radius;
				double vx = Math.cos(angle) * 0.35;
				double vz = Math.sin(angle) * 0.35;
				client.level.addParticle(colorOrParticle(particleKindName, rgb, 1.3F), x, chestY, z, vx, 0.05, vz);
			}
		} else {
			// Settle: gentle wobble at full radius before the next contraction begins.
			int settleTick = cycleTick - CHARGE_PULL_TICKS - CHARGE_BURST_TICKS;
			double radius = maxRadius + Math.sin(settleTick * 0.9) * 0.12;
			for (int i = 0; i < points; i++) {
				float angle = chargeUpAngle + i * (float) (Math.PI * 2 / points);
				double x = player.getX() + Math.cos(angle) * radius;
				double z = player.getZ() + Math.sin(angle) * radius;
				client.level.addParticle(colorOrParticle(particleKindName, rgb, 1.0F), x, footY, z, 0, 0, 0);
			}
		}
	}

	// Two tilted, counter-rotating rings of particles around your waist, like Saturn's rings seen from an angle.
	private static void renderOrbitRings(Minecraft client, AbstractClientPlayer player, int rgb, String particleKindName) {
		float orbitRingsAngle = phase(client, player, 0.08F);
		float ringHeight = player.getBbHeight() * 0.45F;
		float tilt = 0.35F;
		renderTiltedRing(client, player, rgb, particleKindName, orbitRingsAngle, 1.3, ringHeight, tilt, 24, 0.9F);
		renderTiltedRing(client, player, rgb, particleKindName, -orbitRingsAngle * 1.4F, 0.85, ringHeight, tilt, 16, 0.7F);
	}

	private static void renderTiltedRing(Minecraft client, AbstractClientPlayer player, int rgb, String particleKindName, float baseAngle, double radius, float height, float tilt, int points, float particleSize) {
		for (int i = 0; i < points; i++) {
			float angle = baseAngle + i * (float) (Math.PI * 2 / points);
			double x = player.getX() + Math.cos(angle) * radius;
			double z = player.getZ() + Math.sin(angle) * radius;
			double y = player.getY() + height + Math.sin(angle) * radius * tilt;
			client.level.addParticle(colorOrParticle(particleKindName, rgb, particleSize), x, y, z, 0, 0, 0);
		}
	}

	// A lightning strike from well above down to the feet, densely segmented with occasional branching forks.
	private static void renderLightningAura(Minecraft client, AbstractClientPlayer player, String particleKindName) {
		RandomSource random = player.getRandom();
		if (random.nextFloat() > 0.05F) {
			return;
		}
		var kind = ParticleKind.byNameOr(particleKindName, ParticleKind.SPARK).options;
		double angle = random.nextDouble() * Math.PI * 2;
		double radius = 0.4 + random.nextDouble() * 0.5;
		double startX = player.getX() + Math.cos(angle) * radius;
		double startZ = player.getZ() + Math.sin(angle) * radius;
		double startY = player.getY() + 8.0 + random.nextDouble() * 4.0; // struck from well above, not just over the head
		double endX = player.getX();
		double endZ = player.getZ();
		double endY = player.getY();

		int segments = 16;
		double prevX = startX;
		double prevY = startY;
		double prevZ = startZ;
		for (int i = 1; i <= segments; i++) {
			double t = (double) i / segments;
			double x = startX + (endX - startX) * t + (random.nextDouble() - 0.5) * 0.35;
			double z = startZ + (endZ - startZ) * t + (random.nextDouble() - 0.5) * 0.35;
			double y = startY + (endY - startY) * t;
			client.level.addParticle(kind, x, y, z, 0, 0, 0);
			// A second particle halfway to the previous node - a denser main bolt, not just one point per segment.
			client.level.addParticle(kind, (prevX + x) / 2, (prevY + y) / 2, (prevZ + z) / 2, 0, 0, 0);

			if (t > 0.2 && t < 0.8 && random.nextFloat() < 0.12F) {
				double branchAngle = random.nextDouble() * Math.PI * 2;
				double bx = x;
				double by = y;
				double bz = z;
				int branchLength = 3 + random.nextInt(3);
				for (int b = 0; b < branchLength; b++) {
					bx += Math.cos(branchAngle) * 0.3 + (random.nextDouble() - 0.5) * 0.2;
					bz += Math.sin(branchAngle) * 0.3 + (random.nextDouble() - 0.5) * 0.2;
					by -= 0.25 + random.nextDouble() * 0.15;
					client.level.addParticle(kind, bx, by, bz, 0, 0, 0);
				}
			}
			prevX = x;
			prevY = y;
			prevZ = z;
		}
	}

	private static final int[] CONFETTI_COLORS = {0xFFFF5555, 0xFF55FF55, 0xFF5599FF, 0xFFFFFF55, 0xFFFF55FF, 0xFF55FFFF};

	// Occasional multi-colored confetti burst - rainbow-ish by default unless overridden to a named particle kind.
	private static void renderConfettiBurst(Minecraft client, AbstractClientPlayer player, String particleKindName) {
		RandomSource random = player.getRandom();
		if (random.nextFloat() > 0.08F) {
			return;
		}
		ParticleKind override = ParticleKind.byNameOr(particleKindName, null);
		// Random point in a sphere-ish volume around the player instead of always dead-center
		// above the head, so bursts feel like they're happening all around you.
		double burstAngle = random.nextDouble() * Math.PI * 2;
		double burstRadius = random.nextDouble() * 1.5;
		double cx = player.getX() + Math.cos(burstAngle) * burstRadius;
		double cy = player.getY() + 0.3 + random.nextDouble() * (player.getBbHeight() + 1.5);
		double cz = player.getZ() + Math.sin(burstAngle) * burstRadius;
		for (int i = 0; i < 30; i++) {
			int color = CONFETTI_COLORS[random.nextInt(CONFETTI_COLORS.length)] & 0xFFFFFF;
			double vx = (random.nextDouble() - 0.5) * 0.4;
			double vy = random.nextDouble() * 0.18;
			double vz = (random.nextDouble() - 0.5) * 0.4;
			var particle = override != null ? override.options : new DustParticleOptions(color, 1.0F);
			client.level.addParticle(particle, cx, cy, cz, vx, vy, vz);
		}
	}

	// Broad rounded wing planform with a traveling-wave flap - span points lag behind the root for a whip shape.
	private static void renderMothWings(Minecraft client, AbstractClientPlayer player, int rgb, String particleKindName) {
		float mothWingFlap = phase(client, player, 0.15F);
		float bodyMid = player.getBbHeight() * 0.78F;
		// yBodyRot, not getYRot() - the latter follows look direction and would twist the wings with the head.
		float yaw = player.yBodyRot * (float) (Math.PI / 180F);
		double backX = Math.sin(yaw);
		double backZ = -Math.cos(yaw);
		double sideX = -backZ;
		double sideZ = backX;

		int spanSteps = 12;
		int chordSteps = 5;
		float[] phaseOffsetBySide = {0F, 0.35F};
		float waveLagPerT = 1.1F; // radians the tip's oscillation trails the root's - 0 would be a rigid fan
		double maxChord = 0.85; // widest part of the wing, near the root

		for (int sideIndex = 0; sideIndex < 2; sideIndex++) {
			int side = sideIndex == 0 ? -1 : 1;

			for (int i = 0; i <= spanSteps; i++) {
				double t = (double) i / spanSteps;
				// Each span point oscillates at its own phase, not a shared angle - produces the traveling wave.
				float localPhase = mothWingFlap + phaseOffsetBySide[sideIndex] - (float) (t * waveLagPerT);
				float localFlap = (float) Math.sin(localPhase) * 0.6F;
				double hingeAmount = localFlap * t;
				double leadingLateral = t * 1.1; // shorter span than a full arm's length
				double leadingVertical = -t * t * 0.22 + hingeAmount * 0.25; // shallow droop, small flap component
				double leadingBack = 0.3 + t * 0.55 + hingeAmount * 0.9; // flap mainly sweeps back/forward

				double chordWidth = maxChord * (1 - t * t); // broad near the root, tapering to a point at the tip

				for (int cStep = 0; cStep <= chordSteps; cStep++) {
					double cFrac = (double) cStep / chordSteps; // 0 = leading edge, 1 = trailing edge
					double camber = Math.sin(cFrac * Math.PI * 0.5); // trailing edge curves down/back, not a flat fan blade
					double lateral = leadingLateral - camber * chordWidth * 0.15;
					double vertical = leadingVertical - camber * chordWidth * 0.9;
					double back = leadingBack + camber * chordWidth * 0.55;
					float size = 0.9F + 0.3F * (float) t;
					spawnWingParticle(client, player, backX, backZ, sideX, sideZ, bodyMid, lateral, vertical, back, side, rgb, particleKindName, size);
				}
			}
		}
	}

	private static void spawnWingParticle(Minecraft client, AbstractClientPlayer player, double backX, double backZ, double sideX, double sideZ, float bodyMid, double lateral, double vertical, double back, int side, int rgb, String particleKindName, float size) {
		double x = player.getX() + backX * back + sideX * (lateral * side);
		double z = player.getZ() + backZ * back + sideZ * (lateral * side);
		double y = player.getY() + bodyMid + vertical;
		client.level.addParticle(colorOrParticle(particleKindName, rgb, size), x, y, z, 0, 0, 0);
	}

	// Pointed swept-back wings: a chord-swept membrane (sharper taper than renderMothWings), straight feather-tip spikes, and a random ember flicker off the tip.
	private static void renderPhoenixWings(Minecraft client, AbstractClientPlayer player, int rgb, String particleKindName) {
		float flap = phase(client, player, 0.15F);
		float bodyMid = player.getBbHeight() * 0.8F;
		float yaw = player.yBodyRot * (float) (Math.PI / 180F);
		double backX = Math.sin(yaw);
		double backZ = -Math.cos(yaw);
		double sideX = -backZ;
		double sideZ = backX;
		RandomSource random = player.getRandom();
		float[] phaseOffsetBySide = {0F, 0.35F};

		int spanSteps = 12;
		int chordSteps = 4;
		double maxChord = 0.5; // narrower than Moth Wings - a sharper, less bulky wing
		double spanLen = 1.2;

		for (int sideIndex = 0; sideIndex < 2; sideIndex++) {
			int side = sideIndex == 0 ? -1 : 1;
			float localFlap = (float) Math.sin(flap + phaseOffsetBySide[sideIndex]) * 0.6F;

			double[] trailLat = new double[spanSteps + 1];
			double[] trailVert = new double[spanSteps + 1];
			double[] trailBack = new double[spanSteps + 1];

			for (int i = 0; i <= spanSteps; i++) {
				double t = (double) i / spanSteps;
				double hinge = localFlap * t;
				double leadingLateral = t * spanLen;
				double leadingVertical = -t * t * 0.15 + hinge * 0.3;
				double leadingBack = 0.25 + t * 0.85 + hinge * 0.7;

				double chordWidth = maxChord * Math.pow(1 - t, 1.3); // fuller through the middle, real point at the tip
				for (int cStep = 0; cStep <= chordSteps; cStep++) {
					double cFrac = (double) cStep / chordSteps;
					double camber = Math.sin(cFrac * Math.PI * 0.5);
					double lateral = leadingLateral - camber * chordWidth * 0.15;
					double vertical = leadingVertical - camber * chordWidth * 0.9;
					double back = leadingBack + camber * chordWidth * 0.55;
					if (cStep == chordSteps) {
						trailLat[i] = lateral;
						trailVert[i] = vertical;
						trailBack[i] = back;
					}
					float size = 0.75F + 0.25F * (float) (1 - t);
					spawnWingParticle(client, player, backX, backZ, sideX, sideZ, bodyMid, lateral, vertical, back, side, rgb, particleKindName, size);
				}
			}

			// Straight (not curved) spikes past the trailing edge, outer half of the span only.
			int featherStart = spanSteps / 2;
			int featherPoints = 5;
			for (int i = featherStart; i <= spanSteps; i++) {
				double t = (double) i / spanSteps;
				double reach = 0.15 + (t - 0.5) * 0.9; // longer toward the tip
				double fanAngle = (t - 0.5) * 0.5; // feathers splay apart slightly toward the tip
				double dirLat = Math.sin(fanAngle);
				double dirBack = Math.cos(fanAngle);
				for (int p = 1; p <= featherPoints; p++) {
					double u = (double) p / featherPoints;
					double lateral = trailLat[i] + dirLat * reach * u;
					double vertical = trailVert[i] - 0.05 * u;
					double back = trailBack[i] + dirBack * reach * u;
					float size = 0.5F + 0.3F * (float) u;
					spawnWingParticle(client, player, backX, backZ, sideX, sideZ, bodyMid, lateral, vertical, back, side, rgb, particleKindName, size);
				}
			}

			// --- EMBER FLICKER: a sparse, randomized spark off the wingtip - stochastic, not a fixed
			// curve at all, the "on fire" signature of a phoenix. ---
			if (random.nextFloat() < 0.6F) {
				double tipLat = trailLat[spanSteps];
				double tipVert = trailVert[spanSteps];
				double tipBack = trailBack[spanSteps];
				double emberFan = (random.nextDouble() - 0.5) * 1.0;
				double emberReach = 0.3 + random.nextDouble() * 0.5;
				double lateral = tipLat + Math.sin(emberFan) * emberReach;
				double back = tipBack + Math.cos(emberFan) * emberReach;
				double vertical = tipVert - random.nextDouble() * 0.3;
				double x = player.getX() + backX * back + sideX * (lateral * side);
				double z = player.getZ() + backZ * back + sideZ * (lateral * side);
				double y = player.getY() + bodyMid + vertical;
				client.level.addParticle(colorOrParticle(particleKindName, rgb, 0.6F), x, y, z, 0, 0.02, 0);
			}
		}
	}

	// A jagged, slowly writhing zigzag crack off the player's back, pulling nearby particles in with occasional smoke puffs escaping out.
	private static void renderVoidRift(Minecraft client, AbstractClientPlayer player) {
		float voidRiftPhase = phase(client, player, 0.06F);
		RandomSource random = player.getRandom();
		ParticleKind kind = ParticleKind.byNameOr(EssentialsConfig.get().voidRiftParticle, ParticleKind.VOID_RIFT);

		// Slowly orbits all the way around the player instead of sitting glued directly behind their
		// facing direction - a real drifting tear, not a fixed decal.
		float orbitAngle = voidRiftPhase * 0.3F;
		double orbitX = Math.cos(orbitAngle);
		double orbitZ = Math.sin(orbitAngle);
		double perpX = -orbitZ;
		double perpZ = orbitX;

		double riftHeight = player.getBbHeight() + 0.4;
		int points = 19;
		Vec3[] line = new Vec3[points];
		for (int i = 0; i < points; i++) {
			double t = (double) i / (points - 1);
			// Jagged on two horizontal axes plus vertical jitter, different frequencies per axis so it reads as a fracture, not one sine wave.
			double sideJag = Math.sin(t * Math.PI * 3 + voidRiftPhase * 2) * 0.22;
			double backJag = Math.cos(t * Math.PI * 2.3 + voidRiftPhase * 1.6) * 0.18;
			double verticalJitter = Math.sin(t * Math.PI * 5 + voidRiftPhase * 2.5) * 0.12;
			double back = 0.9 + backJag;
			double y = player.getY() + t * riftHeight + verticalJitter;
			double x = player.getX() + orbitX * back + perpX * sideJag;
			double z = player.getZ() + orbitZ * back + perpZ * sideJag;
			line[i] = new Vec3(x, y, z);
			client.level.addParticle(kind.options, x, y, z, 0, 0, 0);
			// A dim haze particle just off to the side of the crack at every other point, so the rift
			// reads as an actual opening with some width/depth instead of a hairline.
			if (i % 2 == 0) {
				double hazeSide = 0.08 + random.nextDouble() * 0.06;
				double hx = x + perpX * hazeSide;
				double hz = z + perpZ * hazeSide;
				client.level.addParticle(ParticleTypes.SQUID_INK, hx, y, hz, 0, 0, 0);
			}
		}

		// Nearby ambient particles visibly get sucked into the slit - now up to 2 pulls per tick
		// instead of a single 50%-chance one, so the pull actually reads as continuous.
		int pulls = random.nextFloat() < 0.7F ? 2 : 1;
		for (int p = 0; p < pulls; p++) {
			Vec3 target = line[1 + random.nextInt(points - 2)];
			double angle = random.nextDouble() * Math.PI * 2;
			double radius = 1.3 + random.nextDouble() * 1.2;
			double sx = target.x + Math.cos(angle) * radius;
			double sz = target.z + Math.sin(angle) * radius;
			double sy = target.y + (random.nextDouble() - 0.5) * 0.6;
			double vx = (target.x - sx) * 0.12;
			double vy = (target.y - sy) * 0.12;
			double vz = (target.z - sz) * 0.12;
			client.level.addParticle(ParticleTypes.SQUID_INK, sx, sy, sz, vx, vy, vz);
		}

		// Unstable puff escaping outward, like the rift briefly losing containment.
		if (random.nextFloat() < 0.06F) {
			Vec3 origin = line[random.nextInt(points)];
			for (int i = 0; i < 3; i++) {
				double vx = (random.nextDouble() - 0.5) * 0.15;
				double vz = (random.nextDouble() - 0.5) * 0.15;
				client.level.addParticle(ParticleTypes.LARGE_SMOKE, origin.x, origin.y, origin.z, vx, 0.05, vz);
			}
		}
	}

	// A 3-strand braid of sparkles from feet to head - strands orbit 120° apart, crossing over/under as they rise.
	private static void renderStarWeave(Minecraft client, AbstractClientPlayer player, String particleKindName) {
		float starWeaveAngle = phase(client, player, 0.1F);
		var kind = ParticleKind.byNameOr(particleKindName, ParticleKind.SPARKLE).options;
		float bodyHeight = player.getBbHeight();
		int strands = 3;
		int pointsPerStrand = 16;
		double radius = 0.45;
		// How many full crossings the braid makes from feet to head - the actual "weave" density.
		double turnsOverHeight = 2.0;

		for (int strand = 0; strand < strands; strand++) {
			float strandPhase = strand * (float) (Math.PI * 2 / strands);
			for (int p = 0; p < pointsPerStrand; p++) {
				double t = (double) p / (pointsPerStrand - 1);
				double y = player.getY() + t * bodyHeight;
				float angle = starWeaveAngle + strandPhase + (float) (t * Math.PI * 2 * turnsOverHeight);
				double x = player.getX() + Math.cos(angle) * radius;
				double z = player.getZ() + Math.sin(angle) * radius;
				client.level.addParticle(kind, x, y, z, 0, 0, 0);
			}
		}
	}

	// A steady stream of sparkles spawned at the feet - vanilla's own particle drift carries them up past the head.
	private static void renderAscendingSparkles(Minecraft client, AbstractClientPlayer player, String particleKindName) {
		RandomSource random = player.getRandom();
		if (random.nextFloat() > 0.5F) {
			return;
		}
		double angle = random.nextDouble() * Math.PI * 2;
		double radius = 0.15 + random.nextDouble() * 0.25;
		double x = player.getX() + Math.cos(angle) * radius;
		double z = player.getZ() + Math.sin(angle) * radius;
		var kind = ParticleKind.byNameOr(particleKindName, ParticleKind.SPARKLE).options;
		client.level.addParticle(kind, x, player.getY(), z, 0, 0.06, 0);
	}

	// A stretched-out tail of sparkles streaming behind you - only while actually moving, tied to real movement direction/speed rather than always-on.
	private static void renderCometTrail(Minecraft client, AbstractClientPlayer player, String particleKindName) {
		Vec3 velocity = player.getDeltaMovement();
		double speed = velocity.horizontalDistance();
		if (speed < 0.02) {
			return;
		}
		RandomSource random = player.getRandom();
		Vec3 back = velocity.normalize().scale(-1);
		var kind = ParticleKind.byNameOr(particleKindName, ParticleKind.SPARKLE).options;
		for (int i = 0; i < 3; i++) {
			double dist = 0.2 + random.nextDouble() * 0.8;
			double spread = (random.nextDouble() - 0.5) * 0.2;
			double x = player.getX() + back.x * dist + spread;
			double y = player.getY() + 0.3 + player.getBbHeight() * 0.5 * random.nextDouble();
			double z = player.getZ() + back.z * dist + spread;
			client.level.addParticle(kind, x, y, z, 0, 0, 0);
		}
	}

	// A slow-drifting sparkle cloud at varying radii/heights, unlike Aura's tight fixed-radius orbit.
	private static void renderStarVeil(Minecraft client, AbstractClientPlayer player, String particleKindName) {
		float starVeilAngle = phase(client, player, 0.03F);
		RandomSource random = player.getRandom();
		if (random.nextFloat() > 0.4F) {
			return;
		}
		float angle = starVeilAngle + random.nextFloat() * (float) (Math.PI * 2);
		double radius = 0.6 + random.nextDouble() * 0.8;
		double height = random.nextDouble() * (player.getBbHeight() + 0.5);
		double x = player.getX() + Math.cos(angle) * radius;
		double z = player.getZ() + Math.sin(angle) * radius;
		var kind = ParticleKind.byNameOr(particleKindName, ParticleKind.SPARKLE).options;
		client.level.addParticle(kind, x, player.getY() + height, z, 0, 0.01, 0);
	}

	private static final int RADIANT_PULSE_PERIOD_TICKS = 30;

	// A ring of sparkles that pulses outward from your chest every ~1.5 seconds.
	private static void renderRadiantPulse(Minecraft client, AbstractClientPlayer player, String particleKindName) {
		if ((client.level.getGameTime() + tickOffset(player)) % RADIANT_PULSE_PERIOD_TICKS != 0) {
			return;
		}
		var kind = ParticleKind.byNameOr(particleKindName, ParticleKind.SPARKLE).options;
		double y = player.getY() + player.getBbHeight() * 0.5;
		int points = 24;
		for (int i = 0; i < points; i++) {
			double angle = Math.PI * 2 * i / points;
			double vx = Math.cos(angle) * 0.15;
			double vz = Math.sin(angle) * 0.15;
			client.level.addParticle(kind, player.getX(), y, player.getZ(), vx, 0, vz);
		}
	}

	// A pulsing sphere of particles centered on the body, dense latitude rings, radius breathing like a heartbeat.
	private static void renderPulsingSphere(Minecraft client, AbstractClientPlayer player, int rgb, String particleKindName) {
		float raw = phase(client, player, 0.08F);
		float pulse = (1F - (float) Math.cos(raw)) * 0.5F; // smooth 0..1..0
		double baseRadius = 1.8 * (0.75 + pulse * 0.5); // much bigger than a body-hugging aura - a real presence around you
		float bodyMid = player.getBbHeight() * 0.5F;
		double centerY = player.getY() + bodyMid;

		int latitudes = 12;
		int pointsPerLat = 18;
		for (int lat = 0; lat < latitudes; lat++) {
			double latAngle = Math.PI * (lat + 0.5) / latitudes - Math.PI / 2;
			double ringRadius = baseRadius * Math.cos(latAngle);
			double yOffset = baseRadius * Math.sin(latAngle);
			for (int i = 0; i < pointsPerLat; i++) {
				double angle = i * (Math.PI * 2 / pointsPerLat) + lat * 0.3;
				double x = player.getX() + Math.cos(angle) * ringRadius;
				double z = player.getZ() + Math.sin(angle) * ringRadius;
				client.level.addParticle(colorOrParticle(particleKindName, rgb, 1.1F), x, centerY + yOffset, z, 0, 0, 0);
			}
		}
	}

	// A ring of particles sweeping up and down the body on a smooth cycle, like a sci-fi scanner.
	private static void renderScanner(Minecraft client, AbstractClientPlayer player, int rgb, String particleKindName) {
		float raw = phase(client, player, 0.06F);
		float sweep = (1F - (float) Math.cos(raw)) * 0.5F; // smooth 0..1..0
		float bodyHeight = player.getBbHeight();
		double y = player.getY() + sweep * bodyHeight;
		double radius = 0.8;
		int points = 20;
		for (int i = 0; i < points; i++) {
			float angle = (float) (Math.PI * 2 * i / points);
			double x = player.getX() + Math.cos(angle) * radius;
			double z = player.getZ() + Math.sin(angle) * radius;
			client.level.addParticle(colorOrParticle(particleKindName, rgb, 1.0F), x, y, z, 0, 0, 0);
		}
	}

	// A real Verlet cloth simulation (see CapeSimulator), unlike every other fixed-procedural cosmetic here.
	private static void renderPhysicsCape(Minecraft client, AbstractClientPlayer player, int rgb, String particleKindName) {
		Vec3[][] nodes = CapeSimulator.tick(client, player);
		var particle = colorOrParticle(particleKindName, rgb, 1.1F);
		int rows = nodes.length;
		int cols = nodes[0].length;
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				Vec3 node = nodes[r][c];
				client.level.addParticle(particle, node.x, node.y, node.z, 0, 0, 0);
				if (c + 1 < cols) {
					Vec3 mid = node.add(nodes[r][c + 1]).scale(0.5);
					client.level.addParticle(particle, mid.x, mid.y, mid.z, 0, 0, 0);
				}
				if (r + 1 < rows) {
					Vec3 mid = node.add(nodes[r + 1][c]).scale(0.5);
					client.level.addParticle(particle, mid.x, mid.y, mid.z, 0, 0, 0);
				}
			}
		}
	}

	// A fixed trapezoid cape silhouette, unlike renderPhysicsCape's real simulation - layers two sine waves so the billow doesn't read as one mechanical ripple.
	private static void renderCloak(Minecraft client, AbstractClientPlayer player, int rgb, String particleKindName) {
		float wave1 = phase(client, player, 0.11F);
		float wave2 = phase(client, player, 0.19F);
		float puffWave = phase(client, player, 0.07F);
		float bodyMid = player.getBbHeight() * 0.85F;
		float yaw = player.yBodyRot * (float) (Math.PI / 180F);
		double backX = Math.sin(yaw);
		double backZ = -Math.cos(yaw);
		double sideX = -backZ;
		double sideZ = backX;

		int rows = 8;
		int cols = 6;
		double length = 1.9; // longer than the player - the hem trails past the feet
		for (int r = 0; r < rows; r++) {
			double t = (double) r / (rows - 1);
			double halfWidth = 0.15 + t * 0.35; // trapezoid: narrow at the shoulders, wide at the hem
			double vert = -t * length * 0.5 + Math.sin(wave1 + t * 2F) * 0.05 * (0.3 + t);
			double backOffset = 0.25 + t * length * 0.85;
			// Front-back "puff" - the cloak billows out and settles back in, not just swaying sideways.
			double puff = Math.sin(puffWave * 1.5F + t * 3F) * 0.16 * t;
			for (int c = 0; c < cols; c++) {
				double u = (double) c / (cols - 1) - 0.5; // -0.5..0.5 across the width
				// Two overlapping waves, phase-shifted across the width (the -u/+u terms), so the ripple travels sideways too.
				double flutter = Math.sin(wave1 * 2F + t * 5F - u * 1.5) * 0.14 * t
						+ Math.sin(wave2 * 3F + t * 8F + u * 2.5) * 0.06 * t;
				double lateral = u * halfWidth * 2 + flutter;
				double x = player.getX() + backX * (backOffset + puff) + sideX * lateral;
				double z = player.getZ() + backZ * (backOffset + puff) + sideZ * lateral;
				double y = player.getY() + bodyMid + vert;
				client.level.addParticle(colorOrParticle(particleKindName, rgb, 1.0F), x, y, z, 0, 0, 0);
			}
		}
	}
}
