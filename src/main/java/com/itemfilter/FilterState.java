package com.itemfilter;

import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FilterState extends PersistentState {
    private final Map<String, Set<Item>> playerFilter = new HashMap<>();
    private final Map<String, Boolean> playerFilterStatus = new HashMap<>();
    private final Map<String, Boolean> playerFilterLocked = new HashMap<>();
    private final Map<String, Boolean> playerFilterHidden = new HashMap<>();

    public Set<Item> getFilter(String playerId) {
        return playerFilter.computeIfAbsent(playerId, k -> new HashSet<>());
    }

    public void setFilterStatus(String playerId, boolean isBlacklist) {
        playerFilterStatus.put(playerId, isBlacklist);
        markDirty();
    }

    public boolean getFilterStatus(String playerId) {
        return playerFilterStatus.getOrDefault(playerId, true);
    }

    public void setFilterLocked(String playerId, boolean isLocked) {
        playerFilterLocked.put(playerId, isLocked);
        markDirty();
    }

    public boolean getFilterLocked(String playerId) {
        return playerFilterLocked.getOrDefault(playerId, false);
    }

    public void setFilterHidden(String playerId, boolean isHidden) {
        playerFilterHidden.put(playerId, isHidden);
        markDirty();
    }

    public boolean getFilterHidden(String playerId) {
        return playerFilterHidden.getOrDefault(playerId, false);
    }

    public void addFilter(String playerId, Item item) {
        getFilter(playerId).add(item);
        markDirty();
    }

    public void removeFilter(String playerId, Item item) {
        Set<Item> filter = playerFilter.get(playerId);
        if (filter != null && filter.remove(item)) {
            markDirty();
        }
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound playersCompound = new NbtCompound();

        playerFilter.forEach((playerId, items) -> {
            NbtCompound itemsCompound = new NbtCompound();
            int index = 0;
            for (Item item : items) {
                Identifier itemId = Registries.ITEM.getId(item);
                itemsCompound.putString("item_" + index++, itemId.toString());
            }
            playersCompound.put(playerId, itemsCompound);
        });

        NbtCompound statusCompound = new NbtCompound();
        playerFilterStatus.forEach((playerId, isBlacklist) -> {
            statusCompound.putBoolean(playerId, isBlacklist);
        });

        NbtCompound lockedCompound = new NbtCompound();
        playerFilterLocked.forEach((playerId, isLocked) -> {
            lockedCompound.putBoolean(playerId, isLocked);
        });

        NbtCompound hiddenCompound = new NbtCompound();
        playerFilterHidden.forEach((playerId, isHidden) -> {
            hiddenCompound.putBoolean(playerId, isHidden);
        });

        nbt.put("filter", playersCompound);
        nbt.put("filterStatus", statusCompound);
        nbt.put("filterLocked", lockedCompound);
        nbt.put("filterHidden", hiddenCompound);
        return nbt;
    }

    public static FilterState fromNbt(NbtCompound nbt) {
        FilterState state = new FilterState();
        NbtCompound playersCompound = nbt.getCompound("filter");
        for (String playerId : playersCompound.getKeys()) {
            NbtCompound itemsCompound = playersCompound.getCompound(playerId);
            Set<Item> items = new HashSet<>();
            for (String key : itemsCompound.getKeys()) {
                Identifier itemId = new Identifier(itemsCompound.getString(key));
                items.add(Registries.ITEM.get(itemId));
            }
            state.playerFilter.put(playerId, items);
        }

        NbtCompound statusCompound = nbt.getCompound("filterStatus");
        for (String playerId : statusCompound.getKeys()) {
            state.playerFilterStatus.put(playerId, statusCompound.getBoolean(playerId));
        }

        NbtCompound lockedCompound = nbt.getCompound("filterLocked");
        for (String playerId : lockedCompound.getKeys()) {
            state.playerFilterLocked.put(playerId, lockedCompound.getBoolean(playerId));
        }

        NbtCompound hiddenCompound = nbt.getCompound("filterHidden");
        for (String playerId : hiddenCompound.getKeys()) {
            state.playerFilterHidden.put(playerId, hiddenCompound.getBoolean(playerId));
        }

        return state;
    }
}