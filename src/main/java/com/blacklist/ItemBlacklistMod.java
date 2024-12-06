// TODO:
// - Add translation key instead of literals
// - Let server owners change people's blacklist
// - Let server owners change permission level required to use command
// - Allow users to switch to whitelist
package com.blacklist;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import java.util.Set;
import net.minecraft.world.PersistentStateManager;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ItemBlacklistMod implements ModInitializer {
    private static final String STATE_NAME = "item_blacklist";

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("itemblacklist")
                .then(literal("add")
                    .then(argument("item", ItemStackArgumentType.itemStack(registryAccess))
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                            Item item = ItemStackArgumentType.getItemStackArgument(context, "item").getItem();
                            BlacklistState state = getBlacklistState(context.getSource().getServer());
                            state.addBlacklist(player.getUuidAsString(), item);
                            player.sendMessage(Text.literal("Added " + item + " to your blacklist."), false);
                            return 1;
                        })))
                .then(literal("remove")
                    .then(argument("item", ItemStackArgumentType.itemStack(registryAccess))
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                            Item item = ItemStackArgumentType.getItemStackArgument(context, "item").getItem();
                            BlacklistState state = getBlacklistState(context.getSource().getServer());
                            state.removeBlacklist(player.getUuidAsString(), item);
                            player.sendMessage(Text.literal("Removed " + item + " from your blacklist."), false);
                            return 1;
                        })))
                .then(literal("list")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                        BlacklistState state = getBlacklistState(context.getSource().getServer());
                        Set<Item> blacklist = state.getBlacklist(player.getUuidAsString());
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
        BlacklistState state = getBlacklistState(player.getServer());
        return state.getBlacklist(player.getUuidAsString());
    }

    private static BlacklistState getBlacklistState(MinecraftServer server) {
        PersistentStateManager stateManager = server.getOverworld().getPersistentStateManager();
        return stateManager.getOrCreate(BlacklistState::fromNbt, BlacklistState::new, STATE_NAME);
    }
}
