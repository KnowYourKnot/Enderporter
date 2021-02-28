package com.knowyourknot.enderporter;

import java.util.List;
import java.util.Random;

import com.knowyourknot.enderporter.block.BlockEnderPorter;
import com.knowyourknot.enderporter.entity.EntityEnderPorter;
import com.knowyourknot.enderporter.gui.EnderPorterScreenHandler;
import com.knowyourknot.enderporter.item.PortablePorter;
import com.knowyourknot.enderporter.item.StablePearl;
import com.oroarmor.config.ConfigItem;
import com.oroarmor.config.command.ConfigCommand;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class EnderPorter implements ModInitializer {
	public static final String MOD_ID = "enderporter";
	public static final Logger LOGGER = LogManager.getFormatterLogger("Enderporter");
	public static final EnderPorterConfig CONFIG = new EnderPorterConfig();

	// this could be better but there are so few config options I don't think it makes much of a difference
	public static Object getConfig(String key) {
		List<ConfigItem<?>> root = CONFIG.getConfigs().get(0).getConfigs();
		for (int i = 0; i < root.size(); i++) {
			if (root.get(i).getName().equals(key)) {
				return root.get(i).getValue();
			}
		}
		return null;
	}

	public static int getConfigInt(String key) {
		Object result = getConfig(key);
		if (result instanceof Integer) {
			return ((Integer)result).intValue();
		} else {
			throw new NullPointerException("Config entry for " + key + " either null or not an integer.");
		}
	}

	public static boolean getConfigBool(String key) {
		Object result = getConfig(key);
		if (result instanceof Boolean) {
			return ((Boolean)result).booleanValue();
		} else {
			throw new NullPointerException("Config entry for " + key + " either null or not a boolean.");
		}
	}

	public static final Random RANDOM = new Random();

	public static final Block ENDER_PORTER = new BlockEnderPorter(FabricBlockSettings.of(Material.METAL).hardness(4.0f));

	public static final Item STABLE_PEARL = new StablePearl(new FabricItemSettings().group(ItemGroup.TRANSPORTATION).maxCount(1));
	public static final Item PORTABLE_PORTER = new PortablePorter(new FabricItemSettings().group(ItemGroup.TRANSPORTATION).maxCount(1));
	public static final Item UPGRADE_RANGE = new Item(new FabricItemSettings().group(ItemGroup.TRANSPORTATION).maxCount(16));
	public static final Item UPGRADE_DIM = new Item(new FabricItemSettings().group(ItemGroup.TRANSPORTATION).maxCount(1));

	public static final BlockEntityType<EntityEnderPorter> ENTITY_ENDER_PORTER = BlockEntityType.Builder.create(EntityEnderPorter::new, ENDER_PORTER).build(null);
	public static final ScreenHandlerType<EnderPorterScreenHandler> ENDER_PORTER_SCREEN_HANDLER;
	
	static {
		ENDER_PORTER_SCREEN_HANDLER = ScreenHandlerRegistry.registerExtended(new Identifier(MOD_ID, "ender_porter_screen_handler"), EnderPorterScreenHandler::new);
	}
	
	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register(new ConfigCommand(CONFIG)::register);
		EnderPorter.LOGGER.info("Config loaded. Initialising Enderporter...");

		Registry.register(Registry.BLOCK, new Identifier(MOD_ID, "ender_porter"), ENDER_PORTER);
		Registry.register(Registry.ITEM, new Identifier(MOD_ID, "ender_porter"), new BlockItem(ENDER_PORTER, new Item.Settings().group(ItemGroup.TRANSPORTATION)));
		Registry.register(Registry.BLOCK_ENTITY_TYPE, new Identifier(MOD_ID, "ender_porter_entity"), ENTITY_ENDER_PORTER);

		Registry.register(Registry.ITEM, new Identifier(MOD_ID, "stable_pearl"), STABLE_PEARL);
		Registry.register(Registry.ITEM, new Identifier(MOD_ID, "portable_porter"), PORTABLE_PORTER);
		Registry.register(Registry.ITEM, new Identifier(MOD_ID, "upgrade_range"), UPGRADE_RANGE);
		Registry.register(Registry.ITEM, new Identifier(MOD_ID, "upgrade_dim"), UPGRADE_DIM);

		EnderPorter.LOGGER.info("Enderporter initialised!");
	}
}
