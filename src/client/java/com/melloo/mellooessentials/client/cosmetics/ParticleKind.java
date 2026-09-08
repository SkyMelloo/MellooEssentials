package com.melloo.mellooessentials.client.cosmetics;

import com.melloo.mellooessentials.client.util.Lang;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

// Curated vanilla particle types the player can pick between for particle-spawning cosmetics.
public enum ParticleKind {
	HEART("heart", ParticleTypes.HEART),
	NOTE("note", ParticleTypes.NOTE),
	SPARKLE("sparkle", ParticleTypes.END_ROD),
	FLAME("flame", ParticleTypes.SMALL_FLAME),
	SPARK("spark", ParticleTypes.ELECTRIC_SPARK),
	CHERRY_BLOSSOM("cherry_blossom", ParticleTypes.CHERRY_LEAVES),
	SNOWFLAKE("snowflake", ParticleTypes.SNOWFLAKE),
	SOUL("soul", ParticleTypes.SOUL),
	ENCHANTED_CRIT("enchanted_crit", ParticleTypes.ENCHANTED_HIT),
	TOTEM("totem", ParticleTypes.TOTEM_OF_UNDYING),
	SCULK("sculk", ParticleTypes.SCULK_CHARGE_POP),
	OMEN("omen", ParticleTypes.RAID_OMEN),
	DUST_PLUME("dust_plume", ParticleTypes.DUST_PLUME),
	FIREWORK("firework", ParticleTypes.FIREWORK),
	VOID_RIFT("void", ParticleTypes.REVERSE_PORTAL),
	HAPPY_VILLAGER("happy_villager", ParticleTypes.HAPPY_VILLAGER),
	POOF("poof", ParticleTypes.POOF),
	CLOUD("cloud", ParticleTypes.CLOUD),
	SMOKE("smoke", ParticleTypes.SMOKE),
	LARGE_SMOKE("large_smoke", ParticleTypes.LARGE_SMOKE),
	EXPLOSION("explosion", ParticleTypes.EXPLOSION),
	WITCH("witch", ParticleTypes.WITCH),
	CRIT("crit", ParticleTypes.CRIT),
	ANGRY_VILLAGER("angry_villager", ParticleTypes.ANGRY_VILLAGER),
	PORTAL("portal", ParticleTypes.PORTAL),
	GLOW("glow", ParticleTypes.GLOW),
	GUST("gust", ParticleTypes.GUST),
	SONIC_BOOM("sonic_boom", ParticleTypes.SONIC_BOOM),
	SQUID_INK("squid_ink", ParticleTypes.SQUID_INK),
	GLOW_SQUID_INK("glow_squid_ink", ParticleTypes.GLOW_SQUID_INK),
	SCRAPE("scrape", ParticleTypes.SCRAPE),
	WAX("wax", ParticleTypes.WAX_ON),
	TRIAL_OMEN("trial_omen", ParticleTypes.TRIAL_OMEN),
	OMINOUS("ominous", ParticleTypes.OMINOUS_SPAWNING),
	FIREFLY("firefly", ParticleTypes.FIREFLY),
	SPORE("spore", ParticleTypes.SPORE_BLOSSOM_AIR);

	private final String translationKey;
	public final ParticleOptions options;

	ParticleKind(String key, ParticleOptions options) {
		this.translationKey = "mellooessentials.particle." + key;
		this.options = options;
	}

	public String label() {
		return Lang.s(translationKey);
	}

	// Config stores the enum name as a plain string; falls back cleanly if missing/unrecognized.
	public static ParticleKind byNameOr(String name, ParticleKind fallback) {
		if (name == null) {
			return fallback;
		}
		for (ParticleKind kind : values()) {
			if (kind.name().equals(name)) {
				return kind;
			}
		}
		return fallback;
	}
}
