package cn.zbx1425.minopp.platform;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import cn.zbx1425.minopp.fabric.MinoFabric;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

public class ServerPlatform {

    public static <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(ServerPlatform.BlockEntitySupplier<T> supplier, Block block) {
        return FabricBlockEntityTypeBuilder.create(supplier::supplier, block).build();
    }

    public static void registerPacket(Identifier resourceLocation) {
        MinoFabric.PACKET_REGISTRY.registerPacket(resourceLocation);
    }

    public static void registerNetworkReceiver(Identifier resourceLocation, ServerPlatform.C2SPacketHandler packetCallback) {
        MinoFabric.PACKET_REGISTRY.registerNetworkReceiverC2S(resourceLocation, packetCallback);
    }

    public static void sendPacketToPlayer(ServerPlayer player, Identifier id, FriendlyByteBuf packet) {
        MinoFabric.PACKET_REGISTRY.sendS2C(player, id, packet);
    }

    @SuppressWarnings("unchecked")
    public static <T> DataComponentType<T> createDataComponentType(Codec<T> codec, StreamCodec<ByteBuf, T> streamCodec) {
        return (DataComponentType<T>) DataComponentType.builder().persistent((Codec<Object>)codec)
            .networkSynchronized((StreamCodec<? super RegistryFriendlyByteBuf, Object>)streamCodec).build();
    }

    @FunctionalInterface
    public interface C2SPacketHandler {

        void handlePacket(MinecraftServer server, ServerPlayer player, FriendlyByteBuf packet);
    }

    @FunctionalInterface
    public interface BlockEntitySupplier<T extends BlockEntity> {
        T supplier(BlockPos pos, BlockState state);
    }

}
