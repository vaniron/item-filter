package com.itemfilter.mixin;
import com.itemfilter.ItemFilterMod;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import java.util.Set;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Inject(method = "onPlayerCollision(Lnet/minecraft/entity/player/PlayerEntity;)V", at = @At("HEAD"), cancellable = true)
    private void onPlayerCollision(PlayerEntity player, CallbackInfo ci) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            World world = player.getWorld();
            ServerWorld serverWorld = (ServerWorld) world;
            MinecraftServer server = serverWorld.getServer();
            Set<Item> filter = ItemFilterMod.getPlayerFilter(serverPlayer);
            String playerUuid = serverPlayer.getUuidAsString();
            Boolean isBlacklist = ItemFilterMod.getFilterState(server).getFilterStatus(playerUuid);
            Boolean inList = filter.contains(((ItemEntity) (Object) this).getStack().getItem());
            if (isBlacklist) {
                if (inList) {
                    ci.cancel();
                }
            } else {
                if (!inList) {
                    ci.cancel();
                }
            }
        }
    }
}