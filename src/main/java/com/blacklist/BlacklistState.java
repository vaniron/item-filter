package com.blacklist;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BlacklistState extends PersistentState {
    private final Map<String, Set<Item>> playerBlacklist = new HashMap<>();

    public Set<Item> getBlacklist(String playerId) {
        return playerBlacklist.computeIfAbsent(playerId, k -> new HashSet<>());
    }

    public void addBlacklist(String playerId, Item item) {
        getBlacklist(playerId).add(item);
        markDirty();
    }

    public void removeBlacklist(String playerId, Item item) {
        Set<Item> blacklist = playerBlacklist.get(playerId);
        if (blacklist != null && blacklist.remove(item)) {
            markDirty();
        }
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound playersCompound = new NbtCompound();
        playerBlacklist.forEach((playerId, items) -> {
            NbtCompound itemsCompound = new NbtCompound();
            int index = 0;
            for (Item item : items) {
                Identifier itemId = Registries.ITEM.getId(item);
                itemsCompound.putString("item_" + index++, itemId.toString());
            }
            playersCompound.put(playerId, itemsCompound);
        });
        nbt.put("blacklist", playersCompound);
        return nbt;
    }

    public static BlacklistState fromNbt(NbtCompound nbt) {
        BlacklistState state = new BlacklistState();
        NbtCompound playersCompound = nbt.getCompound("blacklist");
        for (String playerId : playersCompound.getKeys()) {
            NbtCompound itemsCompound = playersCompound.getCompound(playerId);
            Set<Item> items = new HashSet<>();
            for (String key : itemsCompound.getKeys()) {
                Identifier itemId = new Identifier(itemsCompound.getString(key));
                items.add(Registries.ITEM.get(itemId));
            }
            state.playerBlacklist.put(playerId, items);
        }
        return state;
    }
}
