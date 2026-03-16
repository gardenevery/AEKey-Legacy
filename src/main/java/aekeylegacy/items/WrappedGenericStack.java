/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2018, AlgorithmX2, All rights reserved.
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

package aekeylegacy.items;

import java.util.Objects;
import javax.annotation.Nullable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import aekeylegacy.aekeylegacy.Tags;
import aekeylegacy.api.stacks.AEKey;
import aekeylegacy.api.stacks.GenericStack;

/**
 * Wraps a {@link GenericStack} in an {@link ItemStack}. Even stacks that actually represent vanilla {@link Item items}
 * will be wrapped in this item, to allow items with amount 0 to be represented as itemstacks without becoming the empty
 * item.
 */
public class WrappedGenericStack extends Item {
    private static Item wrappedItem;
    private static final String NBT_AMOUNT = "#";

    public static ItemStack wrap(GenericStack stack) {
        Objects.requireNonNull(stack, "stack");
        return wrap(stack.what(), stack.amount());
    }

    public static ItemStack wrap(AEKey what, long amount) {
        Objects.requireNonNull(what, "what");

        var item = getItem();
        var result = new ItemStack(item);

        var tag = what.toTagGeneric();
        if (amount != 0) {
            tag.setLong(NBT_AMOUNT, amount);
        }
        result.setTagCompound(tag);
        return result;
    }

    public WrappedGenericStack() {
        super();
        this.setMaxStackSize(1);
    }

    @Nullable
    public AEKey unwrapWhat(ItemStack stack) {
        if (stack.getItem() != this) {
            return null;
        }

        var tag = stack.getTagCompound();
        if (tag == null) {
            return null;
        }

        return AEKey.fromTagGeneric(tag);
    }

    public long unwrapAmount(ItemStack stack) {
        if (stack.getItem() != this) {
            return 0;
        }

        long amount = 0;
        if (stack.getTagCompound() != null && stack.getTagCompound().hasKey(NBT_AMOUNT, 4)) {
            amount = stack.getTagCompound().getLong(NBT_AMOUNT);
        }

        return amount;
    }

    public static Item getItem() {
        if (wrappedItem == null) {
            wrappedItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(Tags.MOD_ID, "wrapped_generic_stack"));
        }
        return wrappedItem;
    }
}
