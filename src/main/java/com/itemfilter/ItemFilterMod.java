package com.itemfilter;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
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
                            if (state.getFilterLocked(playerUuid)) {
                                player.sendMessage(Text.translatable("itemfilter.filterlocked"), false);
                                return 0;
                            }
                            String mode = state.getFilterStatus(playerUuid) ? Text.translatable("itemfilter.blacklist").getString() : Text.translatable("itemfilter.whitelist").getString();
                            Set<Item> filter = state.getFilter(playerUuid);
                            if (filter.contains(item)) {
                                player.sendMessage(Text.translatable("itemfilter.alreadycontains", mode, item.getName().getString()), false);
                                return 0;
                            }
                            state.addFilter(playerUuid, item);
                            player.sendMessage(Text.translatable("itemfilter.addeditem", item.getName().getString(), mode), false);
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
                            if (state.getFilterLocked(playerUuid)) {
                                player.sendMessage(Text.translatable("itemfilter.filterlocked"), false);
                                return 0;
                            }
                            String mode = state.getFilterStatus(playerUuid) ? Text.translatable("itemfilter.blacklist").getString() : Text.translatable("itemfilter.whitelist").getString();
                            Set<Item> filter = state.getFilter(playerUuid);
                            if (!filter.contains(item)) {
                                player.sendMessage(Text.translatable("itemfilter.doesntcontain", mode, item.getName().getString()), false);
                                return 0;
                            }
                            state.removeFilter(playerUuid, item);
                            player.sendMessage(Text.translatable("itemfilter.removeditem", item.getName().getString(), mode), false);
                            return 1;
                        })
                    )
                )
                .then(literal("list")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                        FilterState state = getFilterState(context.getSource().getServer());
                        String playerUuid = player.getUuidAsString();
                        String mode = state.getFilterStatus(playerUuid) ? Text.translatable("itemfilter.blacklist").getString() : Text.translatable("itemfilter.whitelist").getString();
                        if (state.getFilterHidden(playerUuid)) {
                            player.sendMessage(Text.translatable("itemfilter.filterhidden"), false);
                            return 0;
                        }
                        Set<Item> filter = state.getFilter(playerUuid);
                        if (filter.isEmpty()) {
                            player.sendMessage(Text.translatable("itemfilter.empty", mode), false);
                        } else {
                            player.sendMessage(Text.translatable("itemfilter.listitems", mode), false);
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
                            String newmode = Text.translatable("itemfilter.blacklist").getString();
                            if (state.getFilterLocked(playerUuid)) {
                                player.sendMessage(Text.translatable("itemfilter.filterlocked"), false);
                                return 0;
                            }

                            if (state.getFilterStatus(playerUuid)) {
                                player.sendMessage(Text.translatable("itemfilter.alreadyinmode", newmode), false);
                            } else {
                                state.setFilterStatus(playerUuid, true);
                                player.sendMessage(Text.translatable("itemfilter.switchedmode", newmode), false);
                            }

                            return 1;
                        })
                    )
                    .then(literal("whitelist")
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                            FilterState state = getFilterState(context.getSource().getServer());
                            String playerUuid = player.getUuidAsString();
                            String newmode = Text.translatable("itemfilter.whitelist").getString();
                            if (state.getFilterLocked(playerUuid)) {
                                player.sendMessage(Text.translatable("itemfilter.filterlocked"), false);
                                return 0;
                            }

                            if (!state.getFilterStatus(playerUuid)) {
                                player.sendMessage(Text.translatable("itemfilter.alreadyinmode", newmode), false);
                            } else {
                                state.setFilterStatus(playerUuid, false);
                                player.sendMessage(Text.translatable("itemfilter.switchedmode", newmode), false);
                            }

                            return 1;
                        })
                    )
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                        FilterState state = getFilterState(context.getSource().getServer());
                        String playerUuid = player.getUuidAsString();
                        String mode = state.getFilterStatus(playerUuid) ? Text.translatable("itemfilter.blacklist").getString() : Text.translatable("itemfilter.whitelist").getString();
                        if (state.getFilterHidden(playerUuid)) {
                            player.sendMessage(Text.translatable("itemfilter.filterhidden"), false);
                            return 0;
                        }

                        player.sendMessage(Text.translatable("itemfilter.filtermode", mode), false);

                        return 1;
                    })
                )
                .then(argument("player", EntityArgumentType.player())
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(literal("add")
                        .then(argument("item", ItemStackArgumentType.itemStack(registryAccess))
                            .executes(context -> {
                                ServerPlayerEntity executor = context.getSource().getPlayerOrThrow();
                                ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                Item item = ItemStackArgumentType.getItemStackArgument(context, "item").getItem();
                                String playerUuid = player.getUuidAsString();
                                FilterState state = getFilterState(context.getSource().getServer());
                                String mode = state.getFilterStatus(playerUuid) ? Text.translatable("itemfilter.blacklist").getString() : Text.translatable("itemfilter.whitelist").getString();
                                String playerName = player.getName().getString();
                                String itemName = item.getName().getString();
                                Set<Item> filter = state.getFilter(playerUuid);
                                if (filter.contains(item)) {
                                    player.sendMessage(Text.translatable("itemfilter.alreadycontainsalt", playerName, mode, itemName), false);
                                    return 0;
                                }
                                state.addFilter(playerUuid, item);
                                executor.sendMessage(Text.translatable("itemfilter.addeditemalt", itemName, playerName, mode), false);
                                return 1;
                            })
                        )
                    )
                    .then(literal("remove")
                        .then(argument("item", ItemStackArgumentType.itemStack(registryAccess))
                            .executes(context -> {
                                ServerPlayerEntity executor = context.getSource().getPlayerOrThrow();
                                ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                Item item = ItemStackArgumentType.getItemStackArgument(context, "item").getItem();
                                String playerUuid = player.getUuidAsString();
                                FilterState state = getFilterState(context.getSource().getServer());
                                String mode = state.getFilterStatus(playerUuid) ? Text.translatable("itemfilter.blacklist").getString() : Text.translatable("itemfilter.whitelist").getString();
                                String playerName = player.getName().getString();
                                String itemName = item.getName().getString();
                                Set<Item> filter = state.getFilter(playerUuid);
                                if (!filter.contains(item)) {
                                    player.sendMessage(Text.translatable("itemfilter.doesntcontainalt", playerName, mode, itemName), false);
                                    return 0;
                                }
                                state.removeFilter(playerUuid, item);
                                executor.sendMessage(Text.translatable("itemfilter.doesntcontainalt", itemName, playerName, mode), false);
                                return 1;
                            })
                        )
                    )
                    .then(literal("list")
                        .executes(context -> {
                            ServerPlayerEntity executor = context.getSource().getPlayerOrThrow();
                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                            FilterState state = getFilterState(context.getSource().getServer());
                            String playerUuid = player.getUuidAsString();
                            String mode = state.getFilterStatus(playerUuid) ? Text.translatable("itemfilter.blacklist").getString() : Text.translatable("itemfilter.whitelist").getString();
                            String playerName = player.getName().getString();
                            Set<Item> filter = state.getFilter(playerUuid);
                            if (filter.isEmpty()) {
                                executor.sendMessage(Text.translatable("itemfilter.emptyalt", playerName, mode), false);
                            } else {
                                executor.sendMessage(Text.translatable("itemfilter.listitemsalt", playerName, mode), false);
                                for (Item item : filter) {
                                    executor.sendMessage(Text.literal("- " + item.getName().getString()), false);
                                }
                            }
                            return 1;
                        })
                    )
                    .then(literal("mode")
                        .then(literal("blacklist")
                            .executes(context -> {
                                ServerPlayerEntity executor = context.getSource().getPlayerOrThrow();
                                ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                FilterState state = getFilterState(context.getSource().getServer());
                                String mode = Text.translatable("itemfilter.blacklist").getString();
                                String playerUuid = player.getUuidAsString();
                                String playerName = player.getName().getString();

                                if (state.getFilterStatus(playerUuid)) {
                                    executor.sendMessage(Text.translatable("itemfilter.alreadyinmodealt", playerName, mode), false);
                                } else {
                                    state.setFilterStatus(playerUuid, true);
                                    executor.sendMessage(Text.translatable("itemfilter.switchedmodealt", playerName, mode), false);
                                }

                                return 1;
                            })
                        )
                        .then(literal("whitelist")
                            .executes(context -> {
                                ServerPlayerEntity executor = context.getSource().getPlayerOrThrow();
                                ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                FilterState state = getFilterState(context.getSource().getServer());
                                String mode = Text.translatable("itemfilter.whitelist").getString();
                                String playerUuid = player.getUuidAsString();
                                String playerName = player.getName().getString();

                                if (!state.getFilterStatus(playerUuid)) {
                                    executor.sendMessage(Text.translatable("itemfilter.alreadyinmodealt", playerName, mode), false);
                                } else {
                                    state.setFilterStatus(playerUuid, false);
                                    executor.sendMessage(Text.translatable("itemfilter.switchedmodealt", playerName, mode), false);
                                }

                                return 1;
                            })
                        )
                        .executes(context -> {
                            ServerPlayerEntity executor = context.getSource().getPlayerOrThrow();
                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                            FilterState state = getFilterState(context.getSource().getServer());
                            String playerUuid = player.getUuidAsString();
                            String mode = state.getFilterStatus(playerUuid) ? Text.translatable("itemfilter.blacklist").getString() : Text.translatable("itemfilter.whitelist").getString();
                            String playerName = player.getName().getString();

                            executor.sendMessage(Text.translatable("itemfilter.filtermodealt", playerName, mode), false);

                            return 1;
                        })
                    )
                    .then(literal("lock")
                        .executes(context -> {
                            ServerPlayerEntity executor = context.getSource().getPlayerOrThrow();
                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                            FilterState state = getFilterState(context.getSource().getServer());
                            String playerUuid = player.getUuidAsString();
                            String mode = state.getFilterStatus(playerUuid) ? Text.translatable("itemfilter.blacklist").getString() : Text.translatable("itemfilter.whitelist").getString();
                            String playerName = player.getName().getString();

                            if (state.getFilterLocked(playerUuid)) {
                                executor.sendMessage(Text.translatable("itemfilter.alreadylocked", playerName, mode), false);
                            } else {
                                state.setFilterLocked(playerUuid, true);
                                executor.sendMessage(Text.translatable("itemfilter.nowlocked", playerName, mode), false);
                            }

                            return 1;
                        })
                    )
                    .then(literal("unlock")
                        .executes(context -> {
                            ServerPlayerEntity executor = context.getSource().getPlayerOrThrow();
                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                            FilterState state = getFilterState(context.getSource().getServer());
                            String playerUuid = player.getUuidAsString();
                            String mode = state.getFilterStatus(playerUuid) ? Text.translatable("itemfilter.blacklist").getString() : Text.translatable("itemfilter.whitelist").getString();
                            String playerName = player.getName().getString();

                            if (!state.getFilterLocked(playerUuid)) {
                                executor.sendMessage(Text.translatable("itemfilter.alreadyunlocked", playerName, mode), false);
                            } else {
                                state.setFilterLocked(playerUuid, false);
                                executor.sendMessage(Text.translatable("itemfilter.nowunlocked", playerName, mode), false);
                            }

                            return 1;
                        })
                    )
                    .then(literal("hide")
                        .executes(context -> {
                            ServerPlayerEntity executor = context.getSource().getPlayerOrThrow();
                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                            FilterState state = getFilterState(context.getSource().getServer());
                            String playerUuid = player.getUuidAsString();
                            String mode = state.getFilterStatus(playerUuid) ? Text.translatable("itemfilter.blacklist").getString() : Text.translatable("itemfilter.whitelist").getString();
                            String playerName = player.getName().getString();

                            if (state.getFilterHidden(playerUuid)) {
                                executor.sendMessage(Text.translatable("itemfilter.alreadyhidden", playerName, mode), false);
                            } else {
                                state.setFilterHidden(playerUuid, true);
                                executor.sendMessage(Text.translatable("itemfilter.nowhidden", playerName, mode), false);
                            }

                            return 1;
                        })
                    )
                    .then(literal("show")
                        .executes(context -> {
                            ServerPlayerEntity executor = context.getSource().getPlayerOrThrow();
                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                            FilterState state = getFilterState(context.getSource().getServer());
                            String playerUuid = player.getUuidAsString();
                            String mode = state.getFilterStatus(playerUuid) ? Text.translatable("itemfilter.blacklist").getString() : Text.translatable("itemfilter.whitelist").getString();
                            String playerName = player.getName().getString();

                            if (!state.getFilterHidden(playerUuid)) {
                                executor.sendMessage(Text.translatable("itemfilter.alreadyshown", playerName, mode), false);
                            } else {
                                state.setFilterHidden(playerUuid, false);
                                executor.sendMessage(Text.translatable("itemfilter.nowshown", playerName, mode), false);
                            }

                            return 1;
                        })
                    )
                )
                .then(literal("default")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(literal("locked")
                        .then(literal("false")
                            .executes(context -> {
                                ServerPlayerEntity executor = context.getSource().getPlayerOrThrow();
                                FilterState state = getFilterState(context.getSource().getServer());

                                if (!state.getFilterLockedDefault()) {
                                    executor.sendMessage(Text.translatable("itemfilter.alreadyunlockeddefault"), false);
                                } else {
                                    state.setFilterLockedDefault(false);
                                    executor.sendMessage(Text.translatable("itemfilter.unlockeddefault"), false);
                                }

                                return 1;
                            })
                        )
                        .then(literal("true")
                            .executes(context -> {
                                ServerPlayerEntity executor = context.getSource().getPlayerOrThrow();
                                FilterState state = getFilterState(context.getSource().getServer());

                                if (state.getFilterLockedDefault()) {
                                    executor.sendMessage(Text.translatable("itemfilter.alreadylockeddefault"), false);
                                } else {
                                    state.setFilterLockedDefault(true);
                                    executor.sendMessage(Text.translatable("itemfilter.lockeddefault"), false);
                                }

                                return 1;
                            })
                        )
                    )
                    .then(literal("hidden")
                        .then(literal("false")
                            .executes(context -> {
                                ServerPlayerEntity executor = context.getSource().getPlayerOrThrow();
                                FilterState state = getFilterState(context.getSource().getServer());

                                if (!state.getFilterHiddenDefault()) {
                                    executor.sendMessage(Text.translatable("itemfilter.alreadyshowndefault"), false);
                                } else {
                                    state.setFilterHiddenDefault(false);
                                    executor.sendMessage(Text.translatable("itemfilter.showndefault"), false);
                                }

                                return 1;
                            })
                        )
                        .then(literal("true")
                            .executes(context -> {
                                ServerPlayerEntity executor = context.getSource().getPlayerOrThrow();
                                FilterState state = getFilterState(context.getSource().getServer());

                                if (state.getFilterHiddenDefault()) {
                                    executor.sendMessage(Text.translatable("itemfilter.alreadyhiddendefault"), false);
                                } else {
                                    state.setFilterHiddenDefault(true);
                                    executor.sendMessage(Text.translatable("itemfilter.hiddendefault"), false);
                                }

                                return 1;
                            })
                        )
                    )
                )
            );
        });
    }

    public static Set<Item> getPlayerFilter(ServerPlayerEntity player) {
        FilterState state = getFilterState(player.getServer());
        return state.getFilter(player.getUuidAsString());
    }

    public static FilterState getFilterState(MinecraftServer server) {
        PersistentStateManager stateManager = server.getOverworld().getPersistentStateManager();
        return stateManager.getOrCreate(FilterState.TYPE, "item_filter_state");
    }
}