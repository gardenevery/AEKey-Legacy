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
public final class AEKeyRenderer {
    private AEKeyRenderer() {
    }

    public static void render(AEKey what, long amount, boolean craftable, int x, int y, AmountFormat format) {
        if (what == null) {
            return;
        }

        boolean hasAmount = amount > 0;

        if (what instanceof AEItemKey itemKey) {
            drawItemIcon(itemKey.getReadOnlyStack(), x, y);
            if (hasAmount) {
                renderAmountText(AEKeyType.items(), amount, x, y, format);
            }

            if (craftable) {
                renderCraftableIndicator(hasAmount, x, y);
            }
        } else if (what instanceof AEFluidKey fluidKey) {
            drawFluidIcon(fluidKey, x, y);
            if (hasAmount) {
                renderAmountText(AEKeyType.fluids(), amount, x, y, format);
            }

            if (craftable) {
                renderCraftableIndicator(hasAmount, x, y);
            }
        }
    }

    public static void render(AEKey what, long amount, int x, int y, AmountFormat format) {
        if (what == null) {
            return;
        }

        if (what instanceof AEItemKey itemKey) {
            drawItemIcon(itemKey.getReadOnlyStack(), x, y);
            if (amount > 0) {
                renderAmountText(AEKeyType.items(), amount, x, y, format);
            }
        } else if (what instanceof AEFluidKey fluidKey) {
            drawFluidIcon(fluidKey, x, y);
            if (amount > 0) {
                renderAmountText(AEKeyType.fluids(), amount, x, y, format);
            }
        }
    }

    public static void render(AEKey what, boolean craftable, int x, int y, AmountFormat format) {
        if (what == null) {
            return;
        }

        if (what instanceof AEItemKey itemKey) {
            drawItemIcon(itemKey.getReadOnlyStack(), x, y);
            if (craftable) {
                renderCraftableIndicator(false, x, y);
            }
        } else if (what instanceof AEFluidKey fluidKey) {
            drawFluidIcon(fluidKey, x, y);
            if (craftable) {
                renderCraftableIndicator(false, x, y);
            }
        }
    }

    public static void renderIcon(AEKey what, int x, int y) {
        if (what == null) {
            return;
        }

        if (what instanceof AEItemKey aeItemKey) {
            drawItemIcon(aeItemKey.getReadOnlyStack(), x, y);
        } else if (what instanceof AEFluidKey aeFluidKey) {
            drawFluidIcon(aeFluidKey, x, y);
        }
    }

    public static void renderItemAmount(long amount, boolean craftable, int x, int y, AmountFormat format) {
        boolean hasAmount = amount > 0;
        if (hasAmount) {
            renderAmountText(AEKeyType.items(), amount, x, y, format);
        }

        if (craftable) {
            renderCraftableIndicator(hasAmount, x, y);
        }
    }

    public static void renderItemAmount(long amount, int x, int y, AmountFormat format) {
        if (amount > 0) {
            renderAmountText(AEKeyType.items(), amount, x, y, format);
        }
    }

    public static void renderFluidAmount(long amount, boolean craftable, int x, int y, AmountFormat format) {
        boolean hasAmount = amount > 0;
        if (hasAmount) {
            renderAmountText(AEKeyType.fluids(), amount, x, y, format);
        }

        if (craftable) {
            renderCraftableIndicator(hasAmount, x, y);
        }
    }

    public static void renderFluidAmount(long amount, int x, int y, AmountFormat format) {
        if (amount > 0) {
            renderAmountText(AEKeyType.fluids(), amount, x, y, format);
        }
    }

    private static void renderAmountText(AEKeyType type, long amount, int x, int y, AmountFormat format) {
        var fontRenderer = Minecraft.getMinecraft().fontRenderer;
        var text = type.formatAmount(amount, format);
        renderText(fontRenderer, text, x, y, false, 0.5f);
    }

    private static void renderCraftableIndicator(boolean topLeft, int x, int y) {
        var fontRenderer = Minecraft.getMinecraft().fontRenderer;
        renderText(fontRenderer, "+", x, y, topLeft, 0.6f);
    }

    private static void renderText(FontRenderer fontRenderer, String text, int x, int y, boolean topLeft, float scale) {
        boolean unicodeFlag = fontRenderer.getUnicodeFlag();
        fontRenderer.setUnicodeFlag(false);

        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.disableBlend();
        GlStateManager.pushMatrix();
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

    private static void drawItemIcon(ItemStack stack, int x, int y) {
        var mc = Minecraft.getMinecraft();
        GlStateManager.enableDepth();
        RenderHelper.enableGUIStandardItemLighting();
        mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableDepth();
    }

    private static void drawFluidIcon(AEFluidKey key, int x, int y) {
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
