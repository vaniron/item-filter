// TODO:
// - Add translation key instead of literals
// - Let server owners change people's blacklist
// - Let server owners change permission level required to use command
package com.blacklist;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ItemBlacklistMod implements ModInitializer {
    private static final Map<ServerPlayerEntity, Set<Item>> playerBlacklist = new HashMap<>();

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("itemblacklist")
                .then(literal("add")
                    .then(argument("item", ItemStackArgumentType.itemStack(registryAccess))
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                            Item item = ItemStackArgumentType.getItemStackArgument(context, "item").getItem();
                            playerBlacklist.computeIfAbsent(player, p -> new HashSet<>()).add(item);
                            player.sendMessage(Text.literal("Added " + item.getName().getString() + " to your blacklist."), false);
                            return 1;
                        })))
                .then(literal("remove")
                    .then(argument("item", ItemStackArgumentType.itemStack(registryAccess))
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                            Item item = ItemStackArgumentType.getItemStackArgument(context, "item").getItem();
                            if (playerBlacklist.containsKey(player) && playerBlacklist.get(player).remove(item)) {
                                player.sendMessage(Text.literal("Removed " + item.getName().getString() + " from your blacklist."), false);
                            } else {
                                player.sendMessage(Text.literal(item.getName().getString() + " was not in your blacklist."), false);
                            }
                            return 1;
                        })))
                .then(literal("list")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                        Set<Item> blacklist = playerBlacklist.getOrDefault(player, Set.of());
                        if (blacklist.isEmpty()) {
                            player.sendMessage(Text.literal("Your blacklist is empty."), false);
                        } else {
                            player.sendMessage(Text.literal("Blacklisted items:"), false);
                            for (Item item : blacklist) {
                                player.sendMessage(Text.literal("- " + item.getName().getString()), false);
                            }
                        }
                        return 1;
                    })));
        });
    }

    public static Set<Item> getPlayerBlacklist(ServerPlayerEntity player) {
        return playerBlacklist.getOrDefault(player, Set.of());
    }
}
