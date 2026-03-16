package aekeylegacy.init;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;

import aekeylegacy.api.stacks.AEKeyType;
import aekeylegacy.api.stacks.AEKeyTypes;
import aekeylegacy.api.stacks.AEKeyTypesInternal;

public final class InitKeyTypes {
    private static final ResourceLocation REGISTRY_ID = new ResourceLocation("ae2", "keytypes");

    private InitKeyTypes() {
    }

    public static void init() {
        var registry = (ForgeRegistry<AEKeyType>) new RegistryBuilder<AEKeyType>()
                .setType(AEKeyType.class)
                .setMaxID(127)
                .setName(REGISTRY_ID)
                .create();

        AEKeyTypesInternal.setRegistry(registry);

        AEKeyTypes.register(AEKeyType.items());
        AEKeyTypes.register(AEKeyType.fluids());
    }
}
