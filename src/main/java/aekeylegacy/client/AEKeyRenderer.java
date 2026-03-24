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

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import aekeylegacy.api.stacks.AEKey;
import aekeylegacy.api.stacks.AEFluidKey;
import aekeylegacy.api.stacks.AEItemKey;
import aekeylegacy.api.stacks.AEKeyType;
import aekeylegacy.api.stacks.AmountFormat;

@SideOnly(Side.CLIENT)
public final class AEKeyRenderer {
    private static final String CRAFTABLE_TEXT = "+";
    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final Tessellator TESSELLATOR = Tessellator.getInstance();

    private AEKeyRenderer() {
    }

    public static void render(AEKey what, long amount, boolean craftable, int x, int y, AmountFormat format) {
        if (what instanceof AEItemKey itemKey) {
            drawItemIcon(itemKey.getReadOnlyStack(), x, y);
            renderAmountAndCraftable(AEKeyType.items(), amount, craftable, x, y, format);
        } else if (what instanceof AEFluidKey fluidKey) {
            drawFluidIcon(fluidKey.getFluid(), x, y);
            renderAmountAndCraftable(AEKeyType.fluids(), amount, craftable, x, y, format);
        }
    }

    public static void renderIcon(AEKey what, int x, int y) {
        if (what instanceof AEItemKey itemKey) {
            drawItemIcon(itemKey.getReadOnlyStack(), x, y);
        } else if (what instanceof AEFluidKey fluidKey) {
            drawFluidIcon(fluidKey.getFluid(), x, y);
        }
    }

    public static void renderItemAmount(long amount, boolean craftable, int x, int y, AmountFormat format) {
        renderAmountAndCraftable(AEKeyType.items(), amount, craftable, x, y, format);
    }

    public static void renderFluidAmount(long amount, boolean craftable, int x, int y, AmountFormat format) {
        renderAmountAndCraftable(AEKeyType.fluids(), amount, craftable, x, y, format);
    }

    private static void renderAmountAndCraftable(AEKeyType type, long amount, boolean craftable, int x, int y, AmountFormat format) {
        int state = ((amount > 0 ? 2 : 0) + (craftable ? 1 : 0));
        if (state == 0) {
            return;
        }

        var fontRenderer = MC.fontRenderer;
        boolean unicodeFlag = fontRenderer.getUnicodeFlag();
        fontRenderer.setUnicodeFlag(false);

        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.disableBlend();

        switch (state) {
            case 3 -> {
                drawTextOnly(fontRenderer, type.formatAmount(amount, format), x, y, 0.5f, false);
                drawTextOnly(fontRenderer, CRAFTABLE_TEXT, x, y, 0.6f, true);
            }
            case 2 -> drawTextOnly(fontRenderer, type.formatAmount(amount, format), x, y, 0.5f, false);
            case 1 -> drawTextOnly(fontRenderer, CRAFTABLE_TEXT, x, y, 0.6f, false);
        }

        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
        GlStateManager.enableBlend();
        fontRenderer.setUnicodeFlag(unicodeFlag);
    }

    private static void drawTextOnly(FontRenderer fontRenderer, String text, int x, int y, float scale, boolean topLeft) {
        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, scale);

        float inverseScale = 1.0f / scale;
        int renderX, renderY;

        if (topLeft) {
            renderX = (int) ((x + 1) * inverseScale);
            renderY = (int) ((y + 1) * inverseScale);
        } else {
            renderX = (int) (((float) x + 15.0f - fontRenderer.getStringWidth(text) * scale) * inverseScale);
            renderY = (int) (((float) y + 15.0f - 7.0f * scale) * inverseScale);
        }

        fontRenderer.drawStringWithShadow(text, renderX, renderY, 0xFFFFFF);
        GlStateManager.popMatrix();
    }

    private static void drawItemIcon(ItemStack stack, int x, int y) {
        GlStateManager.enableDepth();
        RenderHelper.enableGUIStandardItemLighting();
        MC.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableDepth();
    }

    private static void drawFluidIcon(Fluid fluid, int x, int y) {
        GlStateManager.disableLighting();
        MC.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        var sprite = MC.getTextureMapBlocks().getAtlasSprite(fluid.getStill().toString());

        int color = fluid.getColor();
        float r = (color >> 16 & 0xFF) / 255.0f;
        float g = (color >> 8 & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        GlStateManager.color(r, g, b, 1.0f);

        var buffer = TESSELLATOR.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(x, y + 16, 0).tex(sprite.getMinU(), sprite.getMaxV()).endVertex();
        buffer.pos(x + 16, y + 16, 0).tex(sprite.getMaxU(), sprite.getMaxV()).endVertex();
        buffer.pos(x + 16, y, 0).tex(sprite.getMaxU(), sprite.getMinV()).endVertex();
        buffer.pos(x, y, 0).tex(sprite.getMinU(), sprite.getMinV()).endVertex();
        TESSELLATOR.draw();

        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.enableLighting();
    }
}
