package com.thaumcraft.fabric.registry;

import com.thaumcraft.fabric.ThaumcraftFabric;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/**
 * Items of the mod. Every item id must also be listed in tools/items.json
 * (tools/gen_item_assets.py generates its model and lang key from there).
 * The variants of the original ItemResource (meta 0-18) are separate items now.
 * Special behaviour (throwing, research, coins, ...) is added by later stages.
 */
public final class ModItems {
	private ModItems() {}

	/** Every registered item in registration order; used by the creative tab. */
	public static final List<Item> ALL = new ArrayList<>();

	public static final Item ALUMENTUM = register("alumentum");
	public static final Item NITOR = register("nitor");
	public static final Item THAUMIUM_INGOT = register("thaumium_ingot");
	public static final Item QUICKSILVER = register("quicksilver");
	public static final Item MAGIC_TALLOW = register("magic_tallow");
	public static final Item AMBER = register("amber");
	public static final Item ENCHANTED_FABRIC = register("enchanted_fabric");
	public static final Item VIS_FILTER = register("vis_filter");
	public static final Item KNOWLEDGE_FRAGMENT = register("knowledge_fragment");
	public static final Item MIRRORED_GLASS = register("mirrored_glass");
	public static final Item TAINTED_GOO = register("tainted_goo");
	public static final Item TAINT_TENDRIL = register("taint_tendril");
	public static final Item JAR_LABEL = register("jar_label");
	public static final Item SALIS_MUNDUS = register("salis_mundus");
	public static final Item PRIMAL_CHARM = register("primal_charm");
	public static final Item VOID_METAL_INGOT = register("void_metal_ingot");
	public static final Item VOID_SEED = register("void_seed");
	public static final Item GOLD_COIN = register("gold_coin");

	private static Item register(String name) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, ThaumcraftFabric.id(name));
		Item item = new Item(new Item.Properties().setId(key));
		Registry.register(BuiltInRegistries.ITEM, key, item);
		ALL.add(item);
		return item;
	}

	/** Forces class loading, which registers every item. */
	public static void init() {}
}
