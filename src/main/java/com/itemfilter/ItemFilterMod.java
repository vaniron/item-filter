// TODO:
// - Add translation key instead of literals
// - Let server owners change people's filter
// - Let server owners change permission level required to use command
package com.itemfilter;
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

public class ItemFilterMod implements ModInitializer {
    private static final String STATE_NAME = "item_filter";

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("itemfilter")
                .then(literal("add")
                    .then(argument("item", ItemStackArgumentType.itemStack(registryAccess))
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                            Item item = ItemStackArgumentType.getItemStackArgument(context, "item").getItem();
                            String playerUuid = player.getUuidAsString();
                            FilterState state = getFilterState(context.getSource().getServer());
                            String mode = state.getFilterStatus(playerUuid) ? "blacklist" : "whitelist";
                            state.addFilter(playerUuid, item);
                            player.sendMessage(Text.literal("Added " + item + " to your " + mode + "."), false);
                            return 1;
                        })
                    )
                )
                .then(literal("remove")
                    .then(argument("item", ItemStackArgumentType.itemStack(registryAccess))
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                            Item item = ItemStackArgumentType.getItemStackArgument(context, "item").getItem();
                            String playerUuid = player.getUuidAsString();
                            FilterState state = getFilterState(context.getSource().getServer());
                            String mode = state.getFilterStatus(playerUuid) ? "blacklist" : "whitelist";
                            state.removeFilter(playerUuid, item);
                            player.sendMessage(Text.literal("Removed " + item + " from your " + mode + "."), false);
                            return 1;
                        })
                    )
                )
                .then(literal("list")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                        FilterState state = getFilterState(context.getSource().getServer());
                        String playerUuid = player.getUuidAsString();
                        String mode = state.getFilterStatus(playerUuid) ? "blacklist" : "whitelist";
                        Set<Item> filter = state.getFilter(playerUuid);
                        if (filter.isEmpty()) {
                            player.sendMessage(Text.literal("Your " + mode + " is empty."), false);
                        } else {
                            player.sendMessage(Text.literal("Blacklisted items:"), false);
                            for (Item item : filter) {
                                player.sendMessage(Text.literal("- " + item.getName().getString()), false);
                            }
                        }
                        return 1;
                    })
                )
                .then(literal("mode")
                    .then(literal("blacklist")
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                            FilterState state = getFilterState(context.getSource().getServer());
                            String playerUuid = player.getUuidAsString();

                            if (state.getFilterStatus(playerUuid)) {
                                player.sendMessage(Text.literal("You are already in blacklist mode; nothing changed."), false);
                            } else {
                                state.setFilterStatus(playerUuid, true);
                                player.sendMessage(Text.literal("Switched to blacklist mode."), false);
                            }

                            return 1;
                        })
                    )
                    .then(literal("whitelist")
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                            FilterState state = getFilterState(context.getSource().getServer());
                            String playerUuid = player.getUuidAsString();

                            if (!state.getFilterStatus(playerUuid)) {
                                player.sendMessage(Text.literal("You are already in whitelist mode; nothing changed."), false);
                            } else {
                                state.setFilterStatus(playerUuid, false);
                                player.sendMessage(Text.literal("Switched to whitelist mode."), false);
                            }

                            return 1;
                        })
                    )
                )
            );
        });
    }

    public static Set<Item> getPlayerFilter(ServerPlayerEntity player) {
        FilterState state = getFilterState(player.getServer());
        return state.getFilter(player.getUuidAsString());
    }

    private static FilterState getFilterState(MinecraftServer server) {
        PersistentStateManager stateManager = server.getOverworld().getPersistentStateManager();
        return stateManager.getOrCreate(FilterState::fromNbt, FilterState::new, STATE_NAME);
    }
}
