package aekeylegacy;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

import net.minecraft.item.Item;
import net.minecraftforge.oredict.OreDictionary;

public class OreDictUtil {
    private static final MethodHandle STACK_TO_ID_HANDLE;
    private static final MethodHandle ID_TO_NAME_HANDLE;
    static {
        try {
            var stackToIdField = OreDictionary.class.getDeclaredField("stackToId");
            stackToIdField.setAccessible(true);
            STACK_TO_ID_HANDLE = MethodHandles.lookup().unreflectGetter(stackToIdField);
            var idToNameField = OreDictionary.class.getDeclaredField("idToName");
            idToNameField.setAccessible(true);
            ID_TO_NAME_HANDLE = MethodHandles.lookup().unreflectGetter(idToNameField);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to access OreDictionary private fields", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<Integer, List<Integer>> getStackToId() {
        try {
            return (Map<Integer, List<Integer>>) STACK_TO_ID_HANDLE.invokeExact();
        } catch (Throwable e) {
            throw new RuntimeException("Failed to get stackToId", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> getIdToName() {
        try {
            return (List<String>) ID_TO_NAME_HANDLE.invokeExact();
        } catch (Throwable e) {
            throw new RuntimeException("Failed to get idToName", e);
        }
    }

    private OreDictUtil() {
    }

    public static Stream<String> getOreNames() {
        return getIdToName().stream();
    }

    public static boolean hasTag(@Nonnull Item item, int metadata, @Nonnull String tag) {
        int itemId = Item.REGISTRY.getIDForObject(item);
        int effectiveMeta = item.getHasSubtypes() ? metadata : 0;
        var idToName = getIdToName();
        int tagId = -1;

        for (int i = 0; i < idToName.size(); i++) {
            if (tag.equals(idToName.get(i))) {
                tagId = i;
                break;
            }
        }

        if (tagId == -1) {
            return false;
        }

        var stackToId = getStackToId();
        var wildcardIds = stackToId.get(itemId);
        if (wildcardIds != null && wildcardIds.contains(tagId)) {
            return true;
        }

        int exactKey = itemId | ((effectiveMeta + 1) << 16);
        var exactIds = stackToId.get(exactKey);

        return exactIds != null && exactIds.contains(tagId);
    }
}
