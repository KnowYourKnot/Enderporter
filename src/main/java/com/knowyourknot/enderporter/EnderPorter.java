package com.knowyourknot.enderporter;

import java.util.Random;

import com.knowyourknot.enderporter.block.BlockEnderPorter;
import com.knowyourknot.enderporter.entity.EntityEnderPorter;
import com.knowyourknot.enderporter.gui.EnderPorterScreenHandler;
import com.knowyourknot.enderporter.item.DebugItem;
import com.knowyourknot.enderporter.item.PortablePorter;
import com.knowyourknot.enderporter.item.StablePearl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ModInitializer;
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
	
	public static final Random RANDOM = new Random();

	public static final Block ENDER_PORTER = new BlockEnderPorter(FabricBlockSettings.of(Material.METAL).hardness(4.0f));

	public static final Item STABLE_PEARL = new StablePearl(new FabricItemSettings().group(ItemGroup.TRANSPORTATION).maxCount(1));
	public static final Item PORTABLE_PORTER = new PortablePorter(new FabricItemSettings().group(ItemGroup.TRANSPORTATION).maxCount(1));
	public static final Item DEBUG_ITEM = new DebugItem(new FabricItemSettings().group(ItemGroup.TRANSPORTATION).maxCount(1));
	public static final Item UPGRADE_RANGE = new Item(new FabricItemSettings().group(ItemGroup.TRANSPORTATION).maxCount(16));
	public static final Item UPGRADE_DIM = new Item(new FabricItemSettings().group(ItemGroup.TRANSPORTATION).maxCount(1));

	public static final BlockEntityType<EntityEnderPorter> ENTITY_ENDER_PORTER = BlockEntityType.Builder.create(EntityEnderPorter::new, ENDER_PORTER).build(null);
	public static final ScreenHandlerType<EnderPorterScreenHandler> ENDER_PORTER_SCREEN_HANDLER;
	
	static {
		ENDER_PORTER_SCREEN_HANDLER = ScreenHandlerRegistry.registerSimple(new Identifier(MOD_ID, "ender_porter_screen_handler"), EnderPorterScreenHandler::new);
	}
	
	// TODO add tp to dimension command
	@Override
	public void onInitialize() {
		LOGGER.info("Initialising Enderporter...");

		Registry.register(Registry.BLOCK, new Identifier(MOD_ID, "ender_porter"), ENDER_PORTER);
		Registry.register(Registry.ITEM, new Identifier(MOD_ID, "ender_porter"), new BlockItem(ENDER_PORTER, new Item.Settings().group(ItemGroup.TRANSPORTATION)));
		Registry.register(Registry.BLOCK_ENTITY_TYPE, new Identifier(MOD_ID, "ender_porter_entity"), ENTITY_ENDER_PORTER);

		Registry.register(Registry.ITEM, new Identifier(MOD_ID, "stable_pearl"), STABLE_PEARL);
		Registry.register(Registry.ITEM, new Identifier(MOD_ID, "portable_porter"), PORTABLE_PORTER);
		Registry.register(Registry.ITEM, new Identifier(MOD_ID, "debug_item"), DEBUG_ITEM);
		Registry.register(Registry.ITEM, new Identifier(MOD_ID, "upgrade_range"), UPGRADE_RANGE);
		Registry.register(Registry.ITEM, new Identifier(MOD_ID, "upgrade_dim"), UPGRADE_DIM);

		LOGGER.info("Enderporter initialised!");
	}
}
