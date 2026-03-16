/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2015, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package aekeylegacy.client;

import java.text.DecimalFormat;
import java.math.RoundingMode;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import aekeylegacy.api.stacks.AEKey;
import aekeylegacy.api.stacks.AEFluidKey;
import aekeylegacy.api.stacks.AEItemKey;
import aekeylegacy.api.stacks.AEKeyType;
import aekeylegacy.api.stacks.AmountFormat;

@SideOnly(Side.CLIENT)
public class AEKeyRenderer {
    private static final DecimalFormat TRUNCATED_FORMAT = new DecimalFormat("#.##");

    static {
        TRUNCATED_FORMAT.setRoundingMode(RoundingMode.DOWN);
        TRUNCATED_FORMAT.setDecimalSeparatorAlwaysShown(false);
    }

    private AEKeyRenderer() {
    }

    public static void render(AEKey what, int x, int y) {
        if (what instanceof AEItemKey aeItemKey) {
            drawItem(aeItemKey.getReadOnlyStack(), x, y);
        } else if (what instanceof AEFluidKey aeFluidKey) {
            drawFluid(aeFluidKey, x, y);
        }
    }

    public static void render(AEItemKey key, int x, int y) {
        if (key != null) {
            drawItem(key.getReadOnlyStack(), x, y);
        }
    }

    public static void render(AEFluidKey key, int x, int y) {
        if (key != null) {
            drawFluid(key, x, y);
        }
    }

    public static void renderItemAmount(long amount, boolean craftable, int x, int y, AmountFormat format) {
        if (amount > 0) {
            renderAmountText(AEKeyType.items(), amount, x, y, format);
        }
        renderCraftableIndicator(craftable, amount, x, y);
    }

    public static void renderFluidAmount(long amount, boolean craftable, int x, int y, AmountFormat format) {
        if (amount > 0) {
            renderAmountText(AEKeyType.fluids(), amount, x, y, format);
        }
        renderCraftableIndicator(craftable, amount, x, y);
    }

    private static void renderAmountText(AEKeyType keyType, long amount, int x, int y, AmountFormat format) {
        var fontRenderer = Minecraft.getMinecraft().fontRenderer;
        String text = keyType.formatAmount(amount, format);
        renderText(fontRenderer, text, x, y, false);
    }

    private static void renderCraftableIndicator(boolean craftable, long amount, int x, int y) {
        if (!craftable) {
            return;
        }

        var fontRenderer = Minecraft.getMinecraft().fontRenderer;
        boolean topLeft = amount != 0;
        renderText(fontRenderer, "+", x, y, topLeft);
    }

    private static void renderText(FontRenderer fontRenderer, String text, int x, int y, boolean topLeft) {
        boolean unicodeFlag = fontRenderer.getUnicodeFlag();
        fontRenderer.setUnicodeFlag(false);

        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.disableBlend();
        GlStateManager.pushMatrix();
        float scale = 0.5f;
        GlStateManager.scale(scale, scale, scale);

        float inverseScale = 1.0f / scale;
        int renderX, renderY;
        int offset = -1;

        if (topLeft) {
            renderX = (int) ((x + offset + 2) * inverseScale);
            renderY = (int) ((y + offset + 2) * inverseScale);
        } else {
            renderX = (int) (((float) x + offset + 16.0f - fontRenderer.getStringWidth(text) * scale) * inverseScale);
            renderY = (int) (((float) y + offset + 16.0f - 7.0f * scale) * inverseScale);
        }

        fontRenderer.drawStringWithShadow(text, renderX, renderY, 16777215);
        GlStateManager.popMatrix();
        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
        GlStateManager.enableBlend();
        fontRenderer.setUnicodeFlag(unicodeFlag);
    }

    private static void drawItem(ItemStack stack, int x, int y) {
        var mc = Minecraft.getMinecraft();
        GlStateManager.enableDepth();
        RenderHelper.enableGUIStandardItemLighting();
        mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableDepth();
    }

    private static void drawFluid(AEFluidKey key, int x, int y) {
        GlStateManager.disableLighting();
        var mc = Minecraft.getMinecraft();
        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        var sprite = mc.getTextureMapBlocks().getAtlasSprite(key.getFluid().getStill().toString());

        int color = key.getFluid().getColor();
        float r = (color >> 16 & 0xFF) / 255.0f;
        float g = (color >> 8 & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        GlStateManager.color(r, g, b, 1.0f);

        var gui = mc.currentScreen;
        if (gui != null) {
            gui.drawTexturedModalRect(x, y, sprite, 16, 16);
        }

        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.enableLighting();
    }
}
