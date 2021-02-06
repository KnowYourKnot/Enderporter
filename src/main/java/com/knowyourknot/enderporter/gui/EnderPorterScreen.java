package com.knowyourknot.enderporter.gui;

import com.knowyourknot.enderporter.DimPos;
import com.knowyourknot.enderporter.Lang;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class EnderPorterScreen extends HandledScreen<ScreenHandler> {
    private static final Identifier TEXTURE = new Identifier("enderporter", "textures/gui/ender_porter.png");
    public static final int TEXTURE_WIDTH = 176;
    public static final int TEXTURE_HEIGHT = 205;
    public static final int INFO_SCREEN_X = 44;
    public static final int INFO_SCREEN_Y = 19;
    public static final int INFO_SCREEN_WIDTH = 123;
    public static final int INFO_SCREEN_HEIGHT = 68;

    private static final Identifier TEXTURE_MINIMAL = new Identifier("enderporter", "textures/gui/ender_porter_minimal.png");

    EnderPorterScreenHandler screenHandler;

    public EnderPorterScreen(ScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        screenHandler = (EnderPorterScreenHandler) handler;
        this.backgroundWidth =  TEXTURE_WIDTH;
        this.backgroundHeight = TEXTURE_HEIGHT;
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        if (this.screenHandler.freeTravelAllowed()) {
            client.getTextureManager().bindTexture(TEXTURE_MINIMAL);
        } else {
            client.getTextureManager().bindTexture(TEXTURE);
        }
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        drawTexture(matrices, x, y, 0, 0, backgroundWidth, backgroundHeight);
    }

    @Override
    protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
        this.textRenderer.draw(matrices, this.title, (float)this.titleX, (float)this.titleY, 4210752);
     }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        int x = (width - backgroundWidth) / 2 + INFO_SCREEN_X;
        int y = (height - backgroundHeight) / 2 + INFO_SCREEN_Y;
        super.render(matrices, mouseX, mouseY, delta);
        // write teleport location in gui
        ItemStack stack = screenHandler.getStacks().get(0);
        DimPos dimLoc = DimPos.getStackDimPos(stack);
        if (dimLoc != null) {
            MutableText dimensionName = Lang.teleportDimension(dimLoc);
            MutableText dimensionPos = Lang.teleportLocation(dimLoc);

            int nameX = x + (INFO_SCREEN_WIDTH - textRenderer.getWidth(dimensionName))/2;
            int nameY = y + 4;
            int posX = x + (INFO_SCREEN_WIDTH - textRenderer.getWidth(dimensionPos))/2;
            int posY = nameY + 12;
            
            textRenderer.draw(matrices, dimensionName, nameX, nameY, 4210752);
            textRenderer.draw(matrices, dimensionPos, posX, posY, 4210752);

            int blocksAway = screenHandler.getBlocksAway();
            if (blocksAway != -1) {
                MutableText distance = Lang.blocksAway(blocksAway);
                MutableText pearlsRequired = Lang.pearlsRequired(screenHandler.getPearlsRequired());
                if (!screenHandler.hasPearlsRequired()) {
                    pearlsRequired.setStyle(Style.EMPTY.withColor(Formatting.RED));
                }

                int distX = x + (INFO_SCREEN_WIDTH - textRenderer.getWidth(distance))/2;
                int distY = posY + 12;
                int pearlsX = x + (INFO_SCREEN_WIDTH - textRenderer.getWidth(pearlsRequired))/2;
                int pearlsY = distY + 12;

                textRenderer.draw(matrices, distance, distX, distY, 4210752);
                if (!screenHandler.freeTravelAllowed()) {
                    textRenderer.draw(matrices, pearlsRequired, pearlsX, pearlsY, 4210752);
                }
            } else {
                TranslatableText differentDimension = new TranslatableText(Lang.GUI_DIFFERENT_DIMENSION);
                int diffX = x + (INFO_SCREEN_WIDTH - textRenderer.getWidth(differentDimension))/2;
                int diffY = posY + 12;
                if (!screenHandler.hasPearlsRequired()) {
                    differentDimension.setStyle(Style.EMPTY.withColor(Formatting.RED));
                }
                textRenderer.draw(matrices, differentDimension, diffX, diffY, 4210752);
            }            
        } else {
            TranslatableText teleportNotSet = new TranslatableText(Lang.GUI_TELEPORT_NOT_SET);
            textRenderer.draw(matrices, teleportNotSet, x + (float) (INFO_SCREEN_WIDTH - textRenderer.getWidth(teleportNotSet)) / 2, y + 4f, 4210752);
        }
        drawMouseoverTooltip(matrices, mouseX, mouseY);
    }

    @Override
    protected void init() {
        super.init();
        // Center the title
        titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;
    }
}
