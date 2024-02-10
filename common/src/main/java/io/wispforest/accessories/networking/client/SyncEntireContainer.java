package io.wispforest.accessories.networking.client;

import io.wispforest.accessories.AccessoriesAccess;
import io.wispforest.accessories.networking.AccessoriesPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class SyncEntireContainer extends AccessoriesPacket {

    public CompoundTag containerTag;
    public int entityId;

    public SyncEntireContainer(){}

    public SyncEntireContainer(CompoundTag containerTag, int entityId){
        super(false);

        this.containerTag = containerTag;
        this.entityId = entityId;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeNbt(this.containerTag);
        buf.writeVarInt(this.entityId);
    }

    @Override
    protected void read(FriendlyByteBuf buf) {
        this.containerTag = buf.readNbt();
        this.entityId = buf.readVarInt();
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void handle(Player player) {
        super.handle(player);

        var entity = player.level().getEntity(entityId);

        if(!(entity instanceof LivingEntity livingEntity)) return;

        AccessoriesAccess.getHolder(livingEntity).read(containerTag);
    }
}