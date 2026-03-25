package aekeylegacy.api.stacks;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;
import java.util.WeakHashMap;
import javax.annotation.Nullable;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.CapabilityDispatcher;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import aekeylegacy.AELog;
import aekeylegacy.api.storage.AEKeyFilter;

public final class AEItemKey extends AEKey {
    private static final Logger LOG = LogManager.getLogger(AEItemKey.class);

    private static final MethodHandle CAPABILITIES_GETTER;
    private static final MethodHandle SERIALIZE_NBT_HANDLE;
    static {
        try {
            var lookup = MethodHandles.lookup();
            var capabilitiesField = ItemStack.class.getDeclaredField("capabilities");
            capabilitiesField.setAccessible(true);
            CAPABILITIES_GETTER = lookup.unreflectGetter(capabilitiesField);
            var method = CapabilityDispatcher.class.getDeclaredMethod("serializeNBT");
            method.setAccessible(true);
            SERIALIZE_NBT_HANDLE = lookup.unreflect(method);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to create serializeCaps method handle", exception);
        }
    }

    @Nullable
    private static NBTTagCompound serializeStackCaps(ItemStack stack) {
        try {
            var dispatcher = (CapabilityDispatcher) CAPABILITIES_GETTER.invokeExact(stack);
            if (dispatcher == null) {
                return null;
            }

            var caps = (NBTTagCompound) SERIALIZE_NBT_HANDLE.invokeExact(dispatcher);
            // Ensure stacks with no serializable cap providers are treated the same as stacks with no caps!
            return caps == null || caps.isEmpty() ? null : caps;
        } catch (Throwable ex) {
            throw new RuntimeException("Failed to call serializeCaps", ex);
        }
    }

    private final Item item;
    private final int metadata;
    private final InternedTag internedTag;
    private final InternedTag internedCaps;
    private final int hashCode;
    /**
     * A lazily initialized itemstack used for display and ingredient testing purposes. This should never be modified
     * and will always have amount 1.
     */
    @Nullable
    private ItemStack readOnlyStack;

    /**
     * Max stack size cache, or {@code -1} if not initialized.
     */
    private int maxStackSize = -1;

    private AEItemKey(Item item, int metadata, InternedTag internedTag, InternedTag internedCaps) {
        this.item = item;
        this.metadata = metadata;
        this.internedTag = internedTag;
        this.internedCaps = internedCaps;
        this.hashCode = Objects.hash(item, internedTag, internedCaps);
    }

    @Nullable
    public static AEItemKey of(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        var ret = of(stack.getItem(), stack.getMetadata(), stack.getTagCompound(), serializeStackCaps(stack));
        // Cache max stack size since we already have an ItemStack.
        ret.maxStackSize = stack.getMaxStackSize();
        return ret;
    }

    public static boolean matches(AEKey what, ItemStack itemStack) {
        return what instanceof AEItemKey itemKey && itemKey.matches(itemStack);
    }

    public static boolean is(AEKey what) {
        return what instanceof AEItemKey;
    }

    public static AEKeyFilter filter() {
        return AEItemKey::is;
    }

    @Override
    public AEKeyType getType() {
        return AEKeyType.items();
    }

    @Override
    public AEItemKey dropSecondary() {
        return of(item, getHasSubtypes() ? metadata : 0, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AEItemKey aeItemKey = (AEItemKey) o;
        return item == aeItemKey.item && metadata == aeItemKey.metadata && internedTag == aeItemKey.internedTag
                && internedCaps == aeItemKey.internedCaps;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    public static AEItemKey of(Item item) {
        return of(item, 0);
    }

    public static AEItemKey of(Item item, int metadata) {
        return of(item, metadata, null);
    }

    public static AEItemKey of(Item item, int metadata, @Nullable NBTTagCompound tag) {
        return of(item, metadata, tag, null);
    }

    private static AEItemKey of(Item item, int metadata, @Nullable NBTTagCompound tag, @Nullable NBTTagCompound caps) {
        return new AEItemKey(item, metadata, InternedTag.of(tag, false), InternedTag.of(caps, false));
    }

    public boolean matches(ItemStack stack) {
        // TODO: remove or optimize cap check if it becomes too slow >:-(
        return !stack.isEmpty() && stack.getItem() ==item && stack.getMetadata() == metadata
                && Objects.equals(stack.getTagCompound(), internedTag.tag) && Objects.equals(serializeStackCaps(stack), internedCaps.tag);
    }

    public boolean matches(Ingredient ingredient) {
        return ingredient.test(getReadOnlyStack());
    }

    /**
     * @return The ItemStack represented by this key. <strong>NEVER MUTATE THIS</strong>
     */
    public ItemStack getReadOnlyStack() {
        if (readOnlyStack == null) {
            readOnlyStack = new ItemStack(item , 1, metadata, internedCaps.tag);
            readOnlyStack.setTagCompound(internedTag.tag);
        } else {
            if (readOnlyStack.isEmpty()) {
                LOG.error("Something destroyed the read-only itemstack of {}", this);
                readOnlyStack = null;
                return getReadOnlyStack();
            }
        }
        return readOnlyStack;
    }

    public ItemStack toStack() {
        return toStack(1);
    }

    public ItemStack toStack(int count) {
        if (count <= 0) {
            return ItemStack.EMPTY;
        }

        var result = new ItemStack(item, count, metadata, internedCaps.tag);
        result.setTagCompound(copyTag());
        return result;
    }

    public Item getItem() {
        return item;
    }

    public int getMetadata() {
        return metadata;
    }

    @Nullable
    public static AEItemKey fromTag(NBTTagCompound tag) {
        try {
            var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(tag.getString("id")));
            if (item == null || item == Items.AIR) {
                return null;
            }

            int metadata = tag.getInteger("metadata");
            var extraTag = tag.hasKey("tag") ? tag.getCompoundTag("tag") : null;
            var extraCaps = tag.hasKey("caps") ? tag.getCompoundTag("caps") : null;
            return of(item, metadata, extraTag, extraCaps);
        } catch (Exception e) {
            AELog.debug("Tried to load an invalid item key from NBT: %s", tag, e);
            return null;
        }
    }

    @Override
    public NBTTagCompound toTag() {
        NBTTagCompound result = new NBTTagCompound();
        var itemId = ForgeRegistries.ITEMS.getKey(item);

        if (itemId != null) {
            result.setString("id", itemId.toString());
        } else {
            result.setString("id", "minecraft:air");
        }

        result.setInteger("metadata", metadata);
        if (internedTag.tag != null) {
            result.setTag("tag", internedTag.tag.copy());
        }
        if (internedCaps.tag != null) {
            result.setTag("caps", internedCaps.tag.copy());
        }

        return result;
    }

    @Override
    public Object getPrimaryKey() {
        return item;
    }

    /**
     * @see ItemStack#getMaxDamage()
     */
    @Override
    public int getFuzzySearchValue() {
        return item.isDamageable() ? metadata : 0;
    }

    @Override
    public int getFuzzySearchMaxValue() {
        return getReadOnlyStack().getMaxDamage();
    }

    @Override
    public ResourceLocation getId() {
        return ForgeRegistries.ITEMS.getKey(item);
    }

    /**
     * @return <strong>NEVER MODIFY THE RETURNED TAG</strong>
     */
    @Nullable
    public NBTTagCompound getTag() {
        return internedTag.tag;
    }

    @Nullable
    public NBTTagCompound copyTag() {
        return internedTag.tag != null ? internedTag.tag.copy() : null;
    }

    public boolean hasTag() {
        return internedTag.tag != null;
    }

    @Override
    public ItemStack wrapForDisplayOrFilter() {
        return toStack();
    }

    @Override
    public void addDrops(long amount, List<ItemStack> drops, World world, BlockPos pos) {
        while (amount > 0) {
            if (drops.size() > 1000) {
                AELog.warn("Tried dropping an excessive amount of items, ignoring %s %ss", amount, item);
                break;
            }

            var taken = Math.min(amount, getMaxStackSize());
            amount -= taken;
            drops.add(toStack((int) taken));
        }
    }

    @Override
    protected String computeDisplayName() {
        return getReadOnlyStack().getDisplayName();
    }

    public boolean getHasSubtypes() {
        return item.getHasSubtypes();
    }

    @Override
    public boolean isTagged(String tag) {
        if (!OreDictionary.doesOreNameExist(tag)) {
            return false;
        }

        int tagId = OreDictionary.getOreID(tag);
        var stack = new ItemStack(item, 1, metadata);
        int[] oreIds = OreDictionary.getOreIDs(stack);

        for (int id : oreIds) {
            if (id == tagId) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return True if the item represented by this key is damaged.
     */
    public boolean isDamaged() {
        return item.isDamageable() && metadata > 0;
    }

    public int getMaxStackSize() {
        int ret = maxStackSize;

        if (ret == -1) {
            maxStackSize = ret = getReadOnlyStack().getMaxStackSize();
        }

        return ret;
    }

    @Override
    public void writeToPacket(PacketBuffer data) {
        data.writeVarInt(Item.getIdFromItem(item));
        data.writeVarInt(metadata);
        data.writeCompoundTag(internedTag.tag);
        data.writeCompoundTag(internedCaps.tag);
    }

    public static AEItemKey fromPacket(PacketBuffer data) {
        try {
            int i = data.readVarInt();
            var item = Item.getItemById(i);
            int metadata = data.readVarInt();
            var tag = data.readCompoundTag();
            var caps = data.readCompoundTag();
            return new AEItemKey(item, metadata, InternedTag.of(tag, true), InternedTag.of(caps, true));
        } catch (Exception e) {
            AELog.error("Tried to load an invalid item key from packet: %s", data, e);
            return null;
        }
    }

    @Override
    public String toString() {
        var id = ForgeRegistries.ITEMS.getKey(item);
        String idString = id != null ? id.toString() : item.getClass().getName() + "(unregistered)";
        String metaString = idString + " metadata:" + metadata;
        return internedTag.tag == null ? metaString : metaString + " (+tag)";
    }

    private static final class InternedTag {
        private static final InternedTag EMPTY = new InternedTag(null);

        private static final WeakHashMap<InternedTag, WeakReference<InternedTag>> INTERNED = new WeakHashMap<>();

        private final NBTTagCompound tag;
        private final int hashCode;

        InternedTag(NBTTagCompound tag) {
            this.tag = tag;
            this.hashCode = Objects.hashCode(tag);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            InternedTag internedTag = (InternedTag) o;
            return Objects.equals(tag, internedTag.tag);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        public static InternedTag of(@Nullable NBTTagCompound tag, boolean giveOwnership) {
            if (tag == null) {
                return EMPTY;
            }

            synchronized (AEItemKey.class) {
                var searchHolder = new InternedTag(tag);
                var weakRef = INTERNED.get(searchHolder);
                InternedTag ret = null;

                if (weakRef != null) {
                    ret = weakRef.get();
                }

                if (ret == null) {
                    // Copy the tag if we don't get to have ownership of it
                    if (giveOwnership) {
                        ret = searchHolder;
                    } else {
                        ret = new InternedTag(tag.copy());
                    }
                    INTERNED.put(ret, new WeakReference<>(ret));
                }

                return ret;
            }
        }
    }
}
