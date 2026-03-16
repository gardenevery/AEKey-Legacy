package aekeylegacy;

import java.util.Objects;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import aekeylegacy.aekeylegacy.Tags;
import aekeylegacy.init.InitKeyTypes;
import aekeylegacy.items.WrappedGenericStack;

@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION)
public class AEKeyLegacy {

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        InitKeyTypes.init();
    }

    @Mod.EventBusSubscriber(modid = Tags.MOD_ID)
    public static class RegistrationHandler {
        @SubscribeEvent
        public static void registerItems(RegistryEvent.Register<Item> event) {
            var item = new WrappedGenericStack()
                    .setRegistryName(Tags.MOD_ID, "wrapped_generic_stack")
                    .setTranslationKey(Tags.MOD_ID + ".wrapped_generic_stack");
            event.getRegistry().register(item);
        }

        @SideOnly(Side.CLIENT)
        @SubscribeEvent
        public static void registerModels(ModelRegistryEvent event) {
            var item = aekeylegacy.items.WrappedGenericStack.getItem();
            ModelLoader.setCustomModelResourceLocation(item, 0,
                    new ModelResourceLocation(Objects.requireNonNull(item.getRegistryName()), "inventory"));
        }
    }
}
