package com.thaumcraft.fabric.registry;

import com.thaumcraft.fabric.ThaumcraftFabric;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class ModCreativeTabs {
	private ModCreativeTabs() {}

	public static final ResourceKey<CreativeModeTab> MAIN =
			ResourceKey.create(Registries.CREATIVE_MODE_TAB, ThaumcraftFabric.id("main"));

	static {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, MAIN, FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup.thaumcraft"))
				// The original tab icon is the casting wand, which is ported in a later stage.
				.icon(() -> new ItemStack(ModItems.THAUMIUM_INGOT))
				.displayItems((parameters, output) -> ModItems.ALL.forEach(item -> output.accept(item)))
				.build());
	}

	/** Forces class loading, which registers the tab. */
	public static void init() {}
}
