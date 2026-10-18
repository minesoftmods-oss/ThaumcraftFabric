package com.thaumcraft.fabric;

import com.thaumcraft.fabric.registry.ModCreativeTabs;
import com.thaumcraft.fabric.registry.ModItems;
import com.thaumcraft.fabric.registry.ModSounds;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThaumcraftFabric implements ModInitializer {
	public static final String MOD_ID = "thaumcraft";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Thaumcraft is initializing");

		// Order matters: the creative tab icon refers to an item.
		ModSounds.init();
		ModItems.init();
		ModCreativeTabs.init();

		LOGGER.info("Registered {} items", ModItems.ALL.size());
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
