package com.thaumcraft.fabric.aspect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.thaumcraft.fabric.ThaumcraftFabric;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * A Thaumcraft aspect. There are 48 fixed aspects: 6 primal ones and 42 compounds
 * that are built from two other aspects. They are created once, in the order of the
 * original mod, and looked up by their tag ("aer", "ordo", ...).
 *
 * Port of thaumcraft.api.aspects.Aspect (1.7.10).
 */
public final class Aspect {
	/** Must be declared before the constants below, they register themselves here. */
	private static final Map<String, Aspect> BY_TAG = new LinkedHashMap<>();

	/** How the aspect icon is drawn: additive glow, or normal alpha blending for the dark aspects. */
	public enum Blend { ADDITIVE, ALPHA }

	// ---- 6 primal aspects first, then 42 compounds, in the order of the original mod ----
	public static final Aspect AIR = primal("aer", 0xFFFF7E, 'e', Blend.ADDITIVE);
	public static final Aspect EARTH = primal("terra", 0x56C000, '2', Blend.ADDITIVE);
	public static final Aspect FIRE = primal("ignis", 0xFF5A01, 'c', Blend.ADDITIVE);
	public static final Aspect WATER = primal("aqua", 0x3CD4FC, '3', Blend.ADDITIVE);
	public static final Aspect ORDER = primal("ordo", 0xD5D4EC, '7', Blend.ADDITIVE);
	public static final Aspect ENTROPY = primal("perditio", 0x404040, '8', Blend.ALPHA);
	public static final Aspect VOID = compound("vacuos", 0x888888, AIR, ENTROPY, Blend.ALPHA);
	public static final Aspect LIGHT = compound("lux", 0xFFF663, AIR, FIRE, Blend.ADDITIVE);
	public static final Aspect WEATHER = compound("tempestas", 0xFFFFFF, AIR, WATER, Blend.ADDITIVE);
	public static final Aspect MOTION = compound("motus", 0xCDCCF4, AIR, ORDER, Blend.ADDITIVE);
	public static final Aspect COLD = compound("gelum", 0xE1FFFF, FIRE, ENTROPY, Blend.ADDITIVE);
	public static final Aspect CRYSTAL = compound("vitreus", 0x80FFFF, EARTH, ORDER, Blend.ADDITIVE);
	public static final Aspect LIFE = compound("victus", 0xDE0005, WATER, EARTH, Blend.ADDITIVE);
	public static final Aspect POISON = compound("venenum", 0x89F000, WATER, ENTROPY, Blend.ADDITIVE);
	public static final Aspect ENERGY = compound("potentia", 0xC0FFFF, ORDER, FIRE, Blend.ADDITIVE);
	public static final Aspect EXCHANGE = compound("permutatio", 0x578357, ENTROPY, ORDER, Blend.ADDITIVE);
	public static final Aspect METAL = compound("metallum", 0xB5B5CD, EARTH, CRYSTAL, Blend.ADDITIVE);
	public static final Aspect DEATH = compound("mortuus", 0x887788, LIFE, ENTROPY, Blend.ADDITIVE);
	public static final Aspect FLIGHT = compound("volatus", 0xE7E7D7, AIR, MOTION, Blend.ADDITIVE);
	public static final Aspect DARKNESS = compound("tenebrae", 0x222222, VOID, LIGHT, Blend.ADDITIVE);
	public static final Aspect SOUL = compound("spiritus", 0xEBEBFB, LIFE, DEATH, Blend.ADDITIVE);
	public static final Aspect HEAL = compound("sano", 0xFF2F34, LIFE, ORDER, Blend.ADDITIVE);
	public static final Aspect TRAVEL = compound("iter", 0xE0585B, MOTION, EARTH, Blend.ADDITIVE);
	public static final Aspect ELDRITCH = compound("alienis", 0x805080, VOID, DARKNESS, Blend.ADDITIVE);
	public static final Aspect MAGIC = compound("praecantatio", 0x9700C0, VOID, ENERGY, Blend.ADDITIVE);
	public static final Aspect AURA = compound("auram", 0xFFC0FF, MAGIC, AIR, Blend.ADDITIVE);
	public static final Aspect TAINT = compound("vitium", 0x800080, MAGIC, ENTROPY, Blend.ADDITIVE);
	public static final Aspect SLIME = compound("limus", 0x01F800, LIFE, WATER, Blend.ADDITIVE);
	public static final Aspect PLANT = compound("herba", 0x01AC00, LIFE, EARTH, Blend.ADDITIVE);
	public static final Aspect TREE = compound("arbor", 0x876531, AIR, PLANT, Blend.ADDITIVE);
	public static final Aspect BEAST = compound("bestia", 0x9F6409, MOTION, LIFE, Blend.ADDITIVE);
	public static final Aspect FLESH = compound("corpus", 0xEE478D, DEATH, BEAST, Blend.ADDITIVE);
	public static final Aspect UNDEAD = compound("exanimis", 0x3A4000, MOTION, DEATH, Blend.ADDITIVE);
	public static final Aspect MIND = compound("cognitio", 0xFFC2B3, FIRE, SOUL, Blend.ADDITIVE);
	public static final Aspect SENSES = compound("sensus", 0x0FD9FF, AIR, SOUL, Blend.ADDITIVE);
	public static final Aspect MAN = compound("humanus", 0xFFD7C0, BEAST, MIND, Blend.ADDITIVE);
	public static final Aspect CROP = compound("messis", 0xE1B371, PLANT, MAN, Blend.ADDITIVE);
	public static final Aspect MINE = compound("perfodio", 0xDCD2D8, MAN, EARTH, Blend.ADDITIVE);
	public static final Aspect TOOL = compound("instrumentum", 0x4040EE, MAN, ORDER, Blend.ADDITIVE);
	public static final Aspect HARVEST = compound("meto", 0xEEAD82, CROP, TOOL, Blend.ADDITIVE);
	public static final Aspect WEAPON = compound("telum", 0xC05050, TOOL, FIRE, Blend.ADDITIVE);
	public static final Aspect ARMOR = compound("tutamen", 0x00C0C0, TOOL, EARTH, Blend.ADDITIVE);
	public static final Aspect HUNGER = compound("fames", 0x9A0305, LIFE, VOID, Blend.ADDITIVE);
	public static final Aspect GREED = compound("lucrum", 0xE6BE44, MAN, HUNGER, Blend.ADDITIVE);
	public static final Aspect CRAFT = compound("fabrico", 0x809D80, MAN, TOOL, Blend.ADDITIVE);
	public static final Aspect CLOTH = compound("pannus", 0xEAEAC2, TOOL, BEAST, Blend.ADDITIVE);
	public static final Aspect MECHANISM = compound("machina", 0x8080A0, MOTION, TOOL, Blend.ADDITIVE);
	public static final Aspect TRAP = compound("vinculum", 0x9A8080, MOTION, ENTROPY, Blend.ADDITIVE);

	public static final Codec<Aspect> CODEC = Codec.STRING.comapFlatMap(Aspect::decode, Aspect::getTag);

	public static final StreamCodec<ByteBuf, Aspect> STREAM_CODEC =
			ByteBufCodecs.STRING_UTF8.map(Aspect::byTagOrThrow, Aspect::getTag);

	private final String tag;
	private final int color;
	private final List<Aspect> components;
	private final ChatFormatting chatFormatting;
	private final Blend blend;
	private final Identifier texture;

	private Aspect(String tag, int color, List<Aspect> components, ChatFormatting chatFormatting, Blend blend) {
		if (BY_TAG.containsKey(tag)) {
			throw new IllegalArgumentException(tag + " already registered!");
		}
		this.tag = tag;
		this.color = color;
		this.components = components;
		this.chatFormatting = chatFormatting;
		this.blend = blend;
		this.texture = ThaumcraftFabric.id("textures/aspects/" + tag + ".png");
		BY_TAG.put(tag, this);
	}

	private static Aspect primal(String tag, int color, char chatCode, Blend blend) {
		ChatFormatting format = ChatFormatting.getByCode(chatCode);
		return new Aspect(tag, color, List.of(), format == null ? ChatFormatting.WHITE : format, blend);
	}

	private static Aspect compound(String tag, int color, Aspect first, Aspect second, Blend blend) {
		return new Aspect(tag, color, List.of(first, second), ChatFormatting.WHITE, blend);
	}

	public String getTag() {
		return tag;
	}

	/** RGB color without alpha. */
	public int getColor() {
		return color;
	}

	/** The two aspects this one is made of; empty for primal aspects. */
	public List<Aspect> getComponents() {
		return components;
	}

	public boolean isPrimal() {
		return components.isEmpty();
	}

	public Blend getBlend() {
		return blend;
	}

	/** Chat color for tooltips. Only primal aspects have their own, compounds use white. */
	public ChatFormatting getChatFormatting() {
		return chatFormatting;
	}

	/** Full texture path, for example thaumcraft:textures/aspects/aer.png. */
	public Identifier getTexture() {
		return texture;
	}

	/** Display name, for example "Aer". Translation key aspect.thaumcraft.<tag>. */
	public Component getName() {
		return Component.translatable("aspect.thaumcraft." + tag);
	}

	/** Long description, for example "Air". Translation key aspect.thaumcraft.<tag>.description. */
	public Component getDescription() {
		return Component.translatable("aspect.thaumcraft." + tag + ".description");
	}

	private static DataResult<Aspect> decode(String tag) {
		Aspect aspect = BY_TAG.get(tag);
		if (aspect == null) {
			return DataResult.error(() -> "Unknown aspect: " + tag);
		}
		return DataResult.success(aspect);
	}

	public static Optional<Aspect> byTag(String tag) {
		return Optional.ofNullable(BY_TAG.get(tag));
	}

	public static Aspect byTagOrThrow(String tag) {
		Aspect aspect = BY_TAG.get(tag);
		if (aspect == null) {
			throw new IllegalArgumentException("Unknown aspect: " + tag);
		}
		return aspect;
	}

	/** All aspects in registration order (primal first). */
	public static Collection<Aspect> getAspects() {
		return Collections.unmodifiableCollection(BY_TAG.values());
	}

	public static List<Aspect> getPrimalAspects() {
		List<Aspect> result = new ArrayList<>();
		for (Aspect aspect : BY_TAG.values()) {
			if (aspect.isPrimal()) result.add(aspect);
		}
		return result;
	}

	public static List<Aspect> getCompoundAspects() {
		List<Aspect> result = new ArrayList<>();
		for (Aspect aspect : BY_TAG.values()) {
			if (!aspect.isPrimal()) result.add(aspect);
		}
		return result;
	}

	@Override
	public String toString() {
		return tag;
	}
}
