package cn.zbx1425.minopp.fabric;

import cn.zbx1425.minopp.Mino;
import cn.zbx1425.minopp.MinoClient;
import cn.zbx1425.minopp.gui.GameOverlayLayer;
import cn.zbx1425.minopp.render.BlockEntityMinoTableRenderer;
import cn.zbx1425.minopp.render.EntityAutoPlayerRenderer;
import cn.zbx1425.minopp.render.HandCardsSpecialRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.renderer.special.SpecialModelRenderers;

public final class MinoFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.
        MinoClient.init();
        MinoFabric.PACKET_REGISTRY.commitClient();

        HudElementRegistry.attachElementAfter(VanillaHudElements.SCOREBOARD, Mino.id("hand_card_overlay"), GameOverlayLayer.INSTANCE::render);

        BlockEntityRenderers.register(Mino.BLOCK_ENTITY_TYPE_MINO_TABLE.get(), BlockEntityMinoTableRenderer::new);
        EntityRendererRegistry.register(Mino.ENTITY_AUTO_PLAYER.get(), EntityAutoPlayerRenderer::new);

        ClientTickEvents.START_CLIENT_TICK.register(event -> MinoClient.tick());

        SpecialModelRenderers.ID_MAPPER.put(Mino.id("hand_cards_bewlr"), HandCardsSpecialRenderer.Unbaked.MAP_CODEC);
    }
}
