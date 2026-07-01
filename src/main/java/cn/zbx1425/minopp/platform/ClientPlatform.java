package cn.zbx1425.minopp.platform;

import net.minecraft.client.KeyMapping;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import cn.zbx1425.minopp.fabric.MinoFabric;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

import java.util.function.Consumer;

public class ClientPlatform {

    public static void registerKeyBinding(RegistryObject<KeyMapping> keyMapping) {
        KeyBindingHelper.registerKeyBinding(keyMapping.get());
    }

    public static void registerNetworkReceiver(Identifier resourceLocation, Consumer<FriendlyByteBuf> consumer) {
        MinoFabric.PACKET_REGISTRY.registerNetworkReceiverS2C(resourceLocation, consumer);
    }

    public static void sendPacketToServer(Identifier id, FriendlyByteBuf packet) {
        MinoFabric.PACKET_REGISTRY.sendC2S(id, packet);
    }
}
