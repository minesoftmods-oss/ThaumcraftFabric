package com.thaumcraft.fabric.aspect;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * An ordered set of aspects with amounts, for example "5 Terra + 3 Aqua".
 * The order of insertion is kept because it decides how aspects are shown.
 *
 * Port of thaumcraft.api.aspects.AspectList (1.7.10). The methods keep the
 * names and the behaviour of the original so recipe code can be ported one to one.
 * The list is mutable and the mutating methods return the list itself, so
 * {@code new AspectList().add(Aspect.AIR, 2).add(Aspect.FIRE, 1)} works as before.
 */
public final class AspectList {
	/** One line of a list. This is also the saved and the network form. */
	public record Entry(Aspect aspect, int amount) {
		public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Aspect.CODEC.fieldOf("aspect").forGetter(Entry::aspect),
				Codec.INT.fieldOf("amount").forGetter(Entry::amount)
		).apply(instance, Entry::new));

		public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
				Aspect.STREAM_CODEC, Entry::aspect,
				ByteBufCodecs.VAR_INT, Entry::amount,
				Entry::new);
	}

	/** Saved form: [{"aspect": "aer", "amount": 3}, ...] */
	public static final Codec<AspectList> CODEC = Entry.CODEC.listOf().xmap(AspectList::fromEntries, AspectList::entries);

	public static final StreamCodec<ByteBuf, AspectList> STREAM_CODEC =
			Entry.STREAM_CODEC.apply(ByteBufCodecs.list()).map(AspectList::fromEntries, AspectList::entries);

	private final LinkedHashMap<Aspect, Integer> aspects = new LinkedHashMap<>();

	public AspectList() {}

	public AspectList copy() {
		AspectList out = new AspectList();
		out.aspects.putAll(aspects);
		return out;
	}

	/** Number of different aspects. */
	public int size() {
		return aspects.size();
	}

	public boolean isEmpty() {
		return aspects.isEmpty();
	}

	/** Sum of all amounts. */
	public int visSize() {
		int total = 0;
		for (int amount : aspects.values()) total += amount;
		return total;
	}

	/** The aspects in insertion order, as a copy. */
	public List<Aspect> getAspects() {
		return new ArrayList<>(aspects.keySet());
	}

	public List<Aspect> getPrimalAspects() {
		List<Aspect> result = new ArrayList<>();
		for (Aspect aspect : aspects.keySet()) {
			if (aspect.isPrimal()) result.add(aspect);
		}
		return result;
	}

	/** Alphabetical by tag. */
	public List<Aspect> getAspectsSorted() {
		List<Aspect> result = getAspects();
		result.sort((a, b) -> a.getTag().compareTo(b.getTag()));
		return result;
	}

	/** Largest amount first; aspects with the same amount keep their insertion order. */
	public List<Aspect> getAspectsSortedAmount() {
		List<Aspect> result = getAspects();
		result.sort((a, b) -> Integer.compare(getAmount(b), getAmount(a)));
		return result;
	}

	public int getAmount(Aspect key) {
		return aspects.getOrDefault(key, 0);
	}

	/** Takes {@code amount} away if there is enough. Returns false and changes nothing if not. */
	public boolean reduce(Aspect key, int amount) {
		int current = getAmount(key);
		if (current >= amount) {
			aspects.put(key, current - amount);
			return true;
		}
		return false;
	}

	/** Takes {@code amount} away and removes the aspect once it reaches zero. */
	public AspectList remove(Aspect key, int amount) {
		int result = getAmount(key) - amount;
		if (result <= 0) {
			aspects.remove(key);
		} else {
			aspects.put(key, result);
		}
		return this;
	}

	public AspectList remove(Aspect key) {
		aspects.remove(key);
		return this;
	}

	/** Adds to the existing amount. */
	public AspectList add(Aspect aspect, int amount) {
		aspects.merge(aspect, amount, Integer::sum);
		return this;
	}

	public AspectList add(AspectList other) {
		for (Map.Entry<Aspect, Integer> e : other.aspects.entrySet()) {
			add(e.getKey(), e.getValue());
		}
		return this;
	}

	/** Keeps the larger of the existing and the new amount. */
	public AspectList merge(Aspect aspect, int amount) {
		aspects.merge(aspect, amount, Math::max);
		return this;
	}

	public AspectList merge(AspectList other) {
		for (Map.Entry<Aspect, Integer> e : other.aspects.entrySet()) {
			merge(e.getKey(), e.getValue());
		}
		return this;
	}

	public List<Entry> entries() {
		List<Entry> result = new ArrayList<>(aspects.size());
		for (Map.Entry<Aspect, Integer> e : aspects.entrySet()) {
			result.add(new Entry(e.getKey(), e.getValue()));
		}
		return result;
	}

	public static AspectList fromEntries(List<Entry> entries) {
		AspectList list = new AspectList();
		for (Entry entry : entries) {
			list.add(entry.aspect(), entry.amount());
		}
		return list;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof AspectList list && aspects.equals(list.aspects);
	}

	@Override
	public int hashCode() {
		return aspects.hashCode();
	}

	@Override
	public String toString() {
		return aspects.toString();
	}

	/**
	 * Saves and loads a sample list through both codecs and throws if anything differs.
	 * Called once at startup so a broken codec is reported in the log immediately.
	 */
	public static void verifyCodecs() {
		AspectList sample = new AspectList().add(Aspect.AIR, 3).add(Aspect.MAGIC, 5).add(Aspect.TAINT, 1);

		JsonElement json = CODEC.encodeStart(JsonOps.INSTANCE, sample).result().orElseThrow();
		AspectList fromJson = CODEC.parse(JsonOps.INSTANCE, json).result().orElseThrow();

		ByteBuf buffer = Unpooled.buffer();
		STREAM_CODEC.encode(buffer, sample);
		AspectList fromBuffer = STREAM_CODEC.decode(buffer);

		if (!sample.equals(fromJson) || !sample.equals(fromBuffer)) {
			throw new IllegalStateException("AspectList codec round trip failed: " + sample + " -> " + fromJson + " / " + fromBuffer);
		}
	}
}
