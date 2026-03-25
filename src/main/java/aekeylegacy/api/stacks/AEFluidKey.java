package aekeylegacy.api.stacks;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import aekeylegacy.AELog;
import aekeylegacy.api.storage.AEKeyFilter;

public final class AEFluidKey extends AEKey {
    public static final int AMOUNT_BUCKET = 1000;
    public static final int AMOUNT_BLOCK = 1000;

    private final Fluid fluid;
    @Nullable
    private final NBTTagCompound tag;
    private final int hashCode;

    private AEFluidKey(Fluid fluid, @Nullable NBTTagCompound tag) {
        this.fluid = fluid;
        this.tag = tag;
        this.hashCode = Objects.hash(fluid, tag);
    }

    public static AEFluidKey of(Fluid fluid, @Nullable NBTTagCompound tag) {
        // Do a defensive copy of the tag if we're not sure that we can take ownership
        return new AEFluidKey(fluid, tag != null ? tag.copy() : null);
    }

    public static AEFluidKey of(Fluid fluid) {
        return of(fluid, null);
    }

    @Nullable
    public static AEFluidKey of(FluidStack fluidVariant) {
        if (fluidStackIsEmpty(fluidVariant)) {
            return null;
        }
        return of(fluidVariant.getFluid(), fluidVariant.tag);
    }

    public static boolean matches(AEKey what, FluidStack fluid) {
        return what instanceof AEFluidKey fluidKey && fluidKey.matches(fluid);
    }

    public static boolean is(AEKey what) {
        return what instanceof AEFluidKey;
    }

    public static AEKeyFilter filter() {
        return AEFluidKey::is;
    }

    public boolean matches(FluidStack variant) {
        return !fluidStackIsEmpty(variant) && fluid == variant.getFluid() && Objects.equals(tag, variant.tag);
    }

    public static boolean fluidStackIsEmpty(FluidStack stack) {
        return stack == null || stack.amount <= 0;
    }

    @Override
    public AEKeyType getType() {
        return AEKeyType.fluids();
    }

    @Override
    public AEFluidKey dropSecondary() {
        return of(fluid, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AEFluidKey aeFluidKey = (AEFluidKey) o;
        // The hash code comparison is a fast-fail for two objects with different NBT or fluid
        return hashCode == aeFluidKey.hashCode && fluid == aeFluidKey.fluid && Objects.equals(tag, aeFluidKey.tag);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    public static AEFluidKey fromTag(NBTTagCompound tag) {
        try {
            var fluid = FluidRegistry.getFluid(tag.getString("id"));
            if (fluid == null) {
                return null;
            }
            var extraTag = tag.hasKey("tag") ? tag.getCompoundTag("tag") : null;
            return of(fluid, extraTag);
        } catch (Exception e) {
            AELog.debug("Tried to load an invalid fluid key from NBT: %s", tag, e);
            return null;
        }
    }

    @Override
    public NBTTagCompound toTag() {
        NBTTagCompound result = new NBTTagCompound();
        result.setString("id", FluidRegistry.getFluidName(fluid));

        if (tag != null) {
            result.setTag("tag", tag.copy());
        }

        return result;
    }

    @Override
    public Object getPrimaryKey() {
        return fluid;
    }

    @Override
    public ResourceLocation getId() {
        return new ResourceLocation(FluidRegistry.getDefaultFluidName(fluid));
    }

    @Override
    public void addDrops(long amount, List<ItemStack> drops, World world, BlockPos pos) {
        // Fluids are voided
    }

    @Override
    protected String computeDisplayName() {
        return fluid.getLocalizedName(toStack());
    }

    public FluidStack toStack() {
        return toStack(1000);
    }

    public FluidStack toStack(int amount) {
        return new FluidStack(fluid, amount, tag);
    }

    public Fluid getFluid() {
        return fluid;
    }

    /**
     * @return <strong>NEVER MODIFY THE RETURNED TAG</strong>
     */
    @Nullable
    public NBTTagCompound getTag() {
        return tag;
    }

    @Nullable
    public NBTTagCompound copyTag() {
        return tag != null ? tag.copy() : null;
    }

    public boolean hasTag() {
        return tag != null;
    }

    @Override
    public void writeToPacket(PacketBuffer buffer) {
        buffer.writeString(fluid.getName());
        buffer.writeCompoundTag(tag);
    }

    public static AEFluidKey fromPacket(PacketBuffer buffer) {
        try {
            var fluid = FluidRegistry.getFluid(buffer.readString(32767));
            var tag = buffer.readCompoundTag();
            if (fluid == null) {
                return null;
            }
            return new AEFluidKey(fluid, tag);
        } catch (Exception e) {
            AELog.error("Failed to read AEFluidKey from packet", e);
            return null;
        }
    }

    public static boolean is(@Nullable GenericStack stack) {
        return stack != null && stack.what() instanceof AEFluidKey;
    }

    @Override
    public String toString() {
        var id = FluidRegistry.getFluidName(fluid);
        String idString = id != null ? id : fluid.getClass().getName() + "(unregistered)";
        return tag == null ? idString : idString + " (+tag)";
    }
}
