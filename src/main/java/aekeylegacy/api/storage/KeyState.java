package aekeylegacy.api.storage;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import aekeylegacy.AELog;
import aekeylegacy.api.stacks.AEKey;
import aekeylegacy.api.stacks.KeyCounter;

public class KeyState {
    public interface Listener {
        void onKeyStateChanged();
    }

    private final KeyCounter amounts = new KeyCounter();
    private final KeyCounter craftable = new KeyCounter();
    private final List<WeakReference<Listener>> listeners = new ArrayList<>();

    private KeyState() {
    }

    public static KeyState of() {
        return new KeyState();
    }

    public synchronized void addListener(Listener listener) {
        Objects.requireNonNull(listener);
        listeners.add(new WeakReference<>(listener));
    }

    public synchronized void removeListener(Listener listener) {
        Objects.requireNonNull(listener);
        var iter = listeners.iterator();
        while (iter.hasNext()) {
            var l = iter.next().get();
            if (l == null || l == listener) {
                iter.remove();
            }
        }
    }

    protected void notifyListeners() {
        List<Listener> activeListeners;
        synchronized (this) {
            activeListeners = new ArrayList<>(listeners.size());
            var iter = listeners.iterator();
            while (iter.hasNext()) {
                var ref = iter.next();
                var l = ref.get();
                if (l == null) {
                    iter.remove();
                } else {
                    activeListeners.add(l);
                }
            }
        }

        for (var l : activeListeners) {
            l.onKeyStateChanged();
        }
    }

    public synchronized int size() {
        return keySet().size();
    }

    public synchronized long getKeyAmount(@Nonnull AEKey key) {
        return amounts.get(key);
    }

    public synchronized boolean isCraft(@Nonnull AEKey key) {
        return craftable.get(key) > 0;
    }

    public synchronized boolean containsKey(@Nonnull AEKey key) {
        return amounts.get(key) > 0 || craftable.get(key) > 0;
    }

    public synchronized Set<AEKey> keySet() {
        Set<AEKey> keys = new HashSet<>();
        for (var entry : amounts) {
            keys.add(entry.getKey());
        }

        for (var entry : craftable) {
            keys.add(entry.getKey());
        }
        return keys;
    }

    public synchronized boolean isEmpty() {
        return amounts.isEmpty() && craftable.isEmpty();
    }

    public synchronized void addKey(@Nonnull AEKey key, long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative: " + amount);
        }

        if (!containsKey(key)) {
            if (updateKey(key, amount, false)) {
                notifyListeners();
            }
        }
    }

    public synchronized void addKey(@Nonnull AEKey key, boolean craftableFlag) {
        if (!containsKey(key)) {
            if (updateKey(key, 0L, craftableFlag)) {
                notifyListeners();
            }
        }
    }

    public synchronized void addKey(@Nonnull AEKey key, long amount, boolean craftableFlag) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative: " + amount);
        }

        if (!containsKey(key)) {
            if (updateKey(key, amount, craftableFlag)) {
                notifyListeners();
            }
        }
    }

    public synchronized void addKeyAmount(@Nonnull AEKey key, long delta) {
        if (delta == 0) {
            return;
        }

        long currentAmount = getKeyAmount(key);
        if (delta > 0 && Long.MAX_VALUE - currentAmount < delta) {
            throw new ArithmeticException("Amount overflow: " + currentAmount + " + " + delta);
        }

        long newAmount = currentAmount + delta;
        if (newAmount < 0) {
            throw new IllegalArgumentException("Resulting amount cannot be negative: " + newAmount);
        }

        boolean currentCraft = isCraft(key);
        if (updateKey(key, newAmount, currentCraft)) {
            notifyListeners();
        }
    }

    public synchronized void setKeyCraft(@Nonnull AEKey key, boolean craft) {
        long currentAmount = getKeyAmount(key);
        if (craft != isCraft(key)) {
            if (updateKey(key, currentAmount, craft)) {
                notifyListeners();
            }
        }
    }

    public synchronized void removeKey(@Nonnull AEKey key) {
        if (updateKey(key, 0L, false)) {
            notifyListeners();
        }
    }

    public synchronized void clear() {
        boolean hadData = !amounts.isEmpty() || !craftable.isEmpty();
        amounts.clear();
        craftable.clear();
        if (hadData) {
            notifyListeners();
        }
    }

    public synchronized void clearAmounts() {
        if (!amounts.isEmpty()) {
            amounts.clear();
            notifyListeners();
        }
    }

    public synchronized void clearCraftFlags() {
        if (!craftable.isEmpty()) {
            craftable.clear();
            notifyListeners();
        }
    }

    public synchronized NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();

        var amountList = new NBTTagList();
        for (var entry : amounts) {
            var entryTag = new NBTTagCompound();
            entryTag.setTag("key", entry.getKey().toTagGeneric());
            entryTag.setLong("amount", entry.getLongValue());
            amountList.appendTag(entryTag);
        }
        tag.setTag("amounts", amountList);

        var craftList = new NBTTagList();
        for (var entry : craftable) {
            var entryTag = new NBTTagCompound();
            entryTag.setTag("key", entry.getKey().toTagGeneric());
            craftList.appendTag(entryTag);
        }
        tag.setTag("craftable", craftList);
        return tag;
    }

    public synchronized void readFromNBT(NBTTagCompound tag) {
        amounts.clear();
        craftable.clear();

        var amountList = tag.getTagList("amounts", 10);
        for (int i = 0; i < amountList.tagCount(); i++) {
            var entryTag = amountList.getCompoundTagAt(i);
            var key = AEKey.fromTagGeneric(entryTag.getCompoundTag("key"));
            if (key == null) {
                AELog.warn("Skipping invalid key entry in amounts at index {}", i);
                continue;
            }

            long amount = entryTag.getLong("amount");
            if (amount > 0) {
                amounts.set(key, amount);
            } else {
                AELog.warn("Amount is zero for key {} in NBT, skipping", key);
            }
        }

        var craftList = tag.getTagList("craftable", 10);
        for (int i = 0; i < craftList.tagCount(); i++) {
            var entryTag = craftList.getCompoundTagAt(i);
            var key = AEKey.fromTagGeneric(entryTag.getCompoundTag("key"));
            if (key == null) {
                AELog.warn("Skipping invalid key entry in craftable at index {}", i);
                continue;
            }
            craftable.set(key, 1);
        }
        notifyListeners();
    }

    private boolean updateKey(@Nonnull AEKey key, long newAmount, boolean newCraft) {
        if (newAmount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative: " + newAmount);
        }

        long oldAmount = amounts.get(key);
        boolean oldCraft = craftable.get(key) > 0;

        if (newAmount == 0 && !newCraft) {
            if (oldAmount > 0 || oldCraft) {
                amounts.remove(key);
                craftable.remove(key);
                return true;
            }
            return false;
        }

        boolean changed = false;
        if (newAmount > 0) {
            if (oldAmount != newAmount) {
                amounts.set(key, newAmount);
                changed = true;
            }
        } else {
            if (oldAmount > 0) {
                amounts.remove(key);
                changed = true;
            }
        }

        if (newCraft) {
            if (!oldCraft) {
                craftable.set(key, 1);
                changed = true;
            }
        } else {
            if (oldCraft) {
                craftable.remove(key);
                changed = true;
            }
        }
        return changed;
    }
}
