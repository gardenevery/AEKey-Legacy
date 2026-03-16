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

package aekeylegacy.api.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

import net.minecraft.nbt.NBTTagCompound;

import aekeylegacy.api.stacks.AEKey;
import aekeylegacy.api.stacks.AEFluidKey;
import aekeylegacy.api.stacks.AEItemKey;

public class KeyStateView {
    public static final int COLUMNS = 9;

    private List<AEKey> cachedKeys = Collections.emptyList();
    private final KeyState.Listener keyStateListener = this::markDirty;
    private final KeyState keyState;

    private String searchString = "";
    private SortType sortType = SortType.NAME;
    private SortOrder sortOrder = SortOrder.ASCENDING;
    private KeyTypeFilter typeFilter = KeyTypeFilter.ALL;
    private CraftFilter craftFilter = CraftFilter.ALL;
    private boolean dirty = true;
    private int visibleRows;
    private int scrollOffset = 0;
    private boolean hasPower = true;

    public enum SortType {
        NAME, AMOUNT, MOD
    }

    public enum SortOrder {
        ASCENDING, DESCENDING
    }

    public enum KeyTypeFilter {
        ALL, ITEMS, FLUIDS, OTHER
    }

    public enum CraftFilter {
        ALL, CRAFTABLE, NO_CRAFTABLE
    }

    private enum SearchMode {
        DEFAULT, MOD_ID, REGISTRY_ID
    }

    private KeyStateView(KeyState keyState, int visibleRows) {
        this.keyState = keyState;
        this.visibleRows = visibleRows;
    }

    public static KeyStateView of(KeyState keyState, int visibleRows) {
        if (visibleRows <= 0) {
            throw new IllegalArgumentException("visibleRows must be > 0");
        }
        Objects.requireNonNull(keyState);

        KeyStateView view = new KeyStateView(keyState, visibleRows);
        view.keyState.addListener(view.keyStateListener);
        return view;
    }

    public void setVisibleRows(int visibleRows) {
        if (visibleRows <= 0) throw new IllegalArgumentException("visibleRows must be > 0");
        if (this.visibleRows != visibleRows) {
            this.visibleRows = visibleRows;
            markDirty();
        }
    }

    public int getVisibleRows() {
        return visibleRows;
    }

    public void scrollBy(int delta) {
        int max = getMaxScrollOffset();
        setScrollOffset(Math.max(0, Math.min(max, scrollOffset + delta)));
    }

    public void dispose() {
        keyState.removeListener(keyStateListener);
    }

    private void markDirty() {
        dirty = true;
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        var newSearch = searchString == null ? "" : searchString;
        if (!this.searchString.equals(newSearch)) {
            this.searchString = newSearch;
            markDirty();
        }
    }

    public SortType getSortType() {
        return sortType;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public KeyTypeFilter getKeyTypeFilter() {
        return typeFilter;
    }

    public CraftFilter getCraftFilter() {
        return craftFilter;
    }

    public void cycleSortType() {
        sortType = switch (sortType) {
            case NAME -> SortType.AMOUNT;
            case AMOUNT -> SortType.MOD;
            case MOD -> SortType.NAME;
        };
        markDirty();
    }

    public void cycleSortTypeReverse() {
        sortType = switch (sortType) {
            case NAME -> SortType.MOD;
            case AMOUNT -> SortType.NAME;
            case MOD -> SortType.AMOUNT;
        };
        markDirty();
    }

    public void cycleSortOrder() {
        sortOrder = (sortOrder == SortOrder.ASCENDING) ? SortOrder.DESCENDING : SortOrder.ASCENDING;
        markDirty();
    }

    public void cycleKeyTypeFilter() {
        typeFilter = switch (typeFilter) {
            case ALL -> KeyTypeFilter.ITEMS;
            case ITEMS -> KeyTypeFilter.FLUIDS;
            case FLUIDS -> KeyTypeFilter.OTHER;
            case OTHER -> KeyTypeFilter.ALL;
        };
        markDirty();
    }

    public void cycleKeyTypeFilterReverse() {
        typeFilter = switch (typeFilter) {
            case ALL -> KeyTypeFilter.OTHER;
            case ITEMS -> KeyTypeFilter.ALL;
            case FLUIDS -> KeyTypeFilter.ITEMS;
            case OTHER -> KeyTypeFilter.FLUIDS;
        };
        markDirty();
    }

    public void cycleCraftFilter() {
        craftFilter = switch (craftFilter) {
            case ALL -> CraftFilter.CRAFTABLE;
            case CRAFTABLE -> CraftFilter.NO_CRAFTABLE;
            case NO_CRAFTABLE -> CraftFilter.ALL;
        };
        markDirty();
    }

    public void cycleCraftFilterReverse() {
        craftFilter = switch (craftFilter) {
            case ALL -> CraftFilter.NO_CRAFTABLE;
            case CRAFTABLE -> CraftFilter.ALL;
            case NO_CRAFTABLE -> CraftFilter.CRAFTABLE;
        };
        markDirty();
    }

    public void setSortType(SortType sortType) {
        if (this.sortType != sortType) {
            this.sortType = sortType;
            markDirty();
        }
    }

    public void setSortOrder(SortOrder sortOrder) {
        if (this.sortOrder != sortOrder) {
            this.sortOrder = sortOrder;
            markDirty();
        }
    }

    public void setKeyTypeFilter(KeyTypeFilter typeFilter) {
        if (this.typeFilter != typeFilter) {
            this.typeFilter = typeFilter;
            markDirty();
        }
    }

    public void setCraftFilter(CraftFilter craftFilter) {
        if (this.craftFilter != craftFilter) {
            this.craftFilter = craftFilter;
            markDirty();
        }
    }

    public List<AEKey> getKeys() {
        if (dirty) {
            rebuildCache();
        }
        return cachedKeys;
    }

    public List<AEKey> getVisibleKeys() {
        if (!hasPower) {
            return Collections.emptyList();
        }

        var all = getKeys();
        if (all.isEmpty()) {
            return Collections.emptyList();
        }

        int visibleSlots = COLUMNS * visibleRows;
        int maxStartRow = Math.max(0, (all.size() - visibleSlots + COLUMNS - 1) / COLUMNS);
        int startRow = Math.min(scrollOffset, maxStartRow);
        int startIndex = startRow * COLUMNS;
        int endIndex = Math.min(startIndex + visibleSlots, all.size());
        return all.subList(startIndex, endIndex);
    }

    public void setScrollOffset(int scrollOffset) {
        if (scrollOffset < 0) {
            throw new IllegalArgumentException("scrollOffset must be >= 0");
        }

        int max = getMaxScrollOffset();
        this.scrollOffset = Math.min(scrollOffset, max);
    }

    public int getScrollOffset() {
        return scrollOffset;
    }

    public int getMaxScrollOffset() {
        int total = getKeys().size();
        int visibleSlots = COLUMNS * visibleRows;

        if (total <= visibleSlots) {
            return 0;
        }
        return (total - visibleSlots + COLUMNS - 1) / COLUMNS;
    }

    public KeyState getKeyState() {
        return keyState;
    }

    public void setPower(boolean hasPower) {
        if (this.hasPower != hasPower) {
            this.hasPower = hasPower;
        }
    }

    public boolean hasPower() {
        return hasPower;
    }

    public void writeToNBT(NBTTagCompound tag) {
        tag.setString("sortType", sortType.name());
        tag.setString("sortOrder", sortOrder.name());
        tag.setString("typeFilter", typeFilter.name());
        tag.setString("craftFilter", craftFilter.name());
    }

    public void readFromNBT(NBTTagCompound tag) {
        if (tag.hasKey("sortType")) {
            try {
                sortType = SortType.valueOf(tag.getString("sortType"));
            } catch (IllegalArgumentException e) {
                sortType = SortType.NAME;
            }
        }

        if (tag.hasKey("sortOrder")) {
            try {
                sortOrder = SortOrder.valueOf(tag.getString("sortOrder"));
            } catch (IllegalArgumentException e) {
                sortOrder = SortOrder.ASCENDING;
            }
        }

        if (tag.hasKey("typeFilter")) {
            try {
                typeFilter = KeyTypeFilter.valueOf(tag.getString("typeFilter"));
            } catch (IllegalArgumentException e) {
                typeFilter = KeyTypeFilter.ALL;
            }
        }

        if (tag.hasKey("craftFilter")) {
            try {
                craftFilter = CraftFilter.valueOf(tag.getString("craftFilter"));
            } catch (IllegalArgumentException e) {
                craftFilter = CraftFilter.ALL;
            }
        }
        markDirty();
    }

    private boolean passesTypeFilter(AEKey key) {
        return switch (typeFilter) {
            case ALL -> true;
            case ITEMS -> AEItemKey.is(key);
            case FLUIDS -> AEFluidKey.is(key);
            case OTHER -> !AEItemKey.is(key) && !AEFluidKey.is(key);
        };
    }

    private boolean passesCraftFilter(AEKey key) {
        if (craftFilter == CraftFilter.ALL) {
            return true;
        }

        boolean craftable = keyState.isCraft(key);
        if (craftFilter == CraftFilter.CRAFTABLE) {
            return craftable;
        } else {
            return !craftable;
        }
    }

    private boolean matchesSearch(AEKey key) {
        if (searchString.isEmpty()) {
            return true;
        }

        var trimmed = searchString.trim();
        var mode = SearchMode.DEFAULT;
        String inner = trimmed;

        if (trimmed.startsWith("*")) {
            mode = SearchMode.REGISTRY_ID;
            inner = trimmed.substring(1).trim();
        } else if (trimmed.startsWith("@")) {
            mode = SearchMode.MOD_ID;
            inner = trimmed.substring(1).trim();
        }

        if (inner.isEmpty()) {
            return true;
        }

        var lowerInner = inner.toLowerCase();
        var terms = lowerInner.split("\\s+");
        List<String> include = new ArrayList<>();
        List<String> exclude = new ArrayList<>();

        for (var term : terms) {
            if (term.isEmpty()) {
                continue;
            }

            if (term.startsWith("-") || term.startsWith("!")) {
                if (term.length() > 1) {
                    exclude.add(term.substring(1));
                }
            } else {
                include.add(term);
            }
        }

        if (include.isEmpty()) {
            include.add("");
        }

        String target = switch (mode) {
            case REGISTRY_ID -> key.getId().toString().toLowerCase();
            case MOD_ID -> key.getModId().toLowerCase();
            default -> key.getDisplayName().toLowerCase() + " " + key.getModId().toLowerCase();
        };

        for (var inc : include) {
            if (!target.contains(inc)) {
                return false;
            }
        }

        for (var exc : exclude) {
            if (target.contains(exc)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchNameOrMod(AEKey key, String inner, boolean searchMod) {
        var lowerInner = inner.toLowerCase();
        var terms = lowerInner.split("\\s+");
        List<String> include = new ArrayList<>();
        List<String> exclude = new ArrayList<>();

        for (var term : terms) {
            if (term.isEmpty()) {
                continue;
            }

            if (term.startsWith("-") || term.startsWith("!")) {
                if (term.length() > 1) {
                    exclude.add(term.substring(1));
                }
            } else {
                include.add(term);
            }
        }

        if (include.isEmpty()) {
            include.add("");
        }

        var modId = key.getModId().toLowerCase();
        var displayName = key.getDisplayName().toLowerCase();

        for (var inc : include) {
            boolean incMatch;

            if (searchMod) {
                incMatch = modId.contains(inc);
            } else {
                incMatch = displayName.contains(inc) || modId.contains(inc);
            }

            if (!incMatch) {
                return false;
            }
        }

        for (var exc : exclude) {
            if (searchMod) {
                if (modId.contains(exc)) {
                    return false;
                }
            } else {
                if (displayName.contains(exc) || modId.contains(exc)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void rebuildCache() {
        var allKeys = new ArrayList<>(keyState.keySet());

        var filtered = allKeys.stream()
                .filter(this::passesTypeFilter)
                .filter(this::passesCraftFilter)
                .filter(this::matchesSearch)
                .collect(Collectors.toList());

        var comparator = createComparator();
        filtered.sort(comparator);
        cachedKeys = Collections.unmodifiableList(filtered);
        dirty = false;
    }

    private Comparator<AEKey> createComparator() {
        var comparator = switch (sortType) {
            case NAME -> Comparator.comparing(AEKey::getDisplayName, String.CASE_INSENSITIVE_ORDER);
            case AMOUNT -> getAeKeyComparator();
            case MOD -> Comparator.comparing(AEKey::getModId, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(AEKey::getDisplayName, String.CASE_INSENSITIVE_ORDER);
        };
        return sortOrder == SortOrder.DESCENDING ? comparator.reversed() : comparator;
    }

    @Nonnull
    private Comparator<AEKey> getAeKeyComparator() {
        var comp = Comparator.comparingDouble((AEKey key) -> {
            long amount = keyState.getKeyAmount(key);
            boolean craft = keyState.isCraft(key);
            if (amount == 0 && craft) {
                return -1.0;
            }
            if (AEFluidKey.is(key)) {
                return amount / 1000.0;
            } else {
                return amount;
            }
        });
        comp = comp.thenComparing(AEKey::getDisplayName, String.CASE_INSENSITIVE_ORDER);
        return comp;
    }
}
