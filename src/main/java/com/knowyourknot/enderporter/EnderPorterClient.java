package com.knowyourknot.enderporter;

import com.knowyourknot.enderporter.gui.EnderPorterScreen;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;

@Environment(EnvType.CLIENT)
public class EnderPorterClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ScreenRegistry.register(EnderPorter.ENDER_PORTER_SCREEN_HANDLER, EnderPorterScreen::new);
    }
    
}
