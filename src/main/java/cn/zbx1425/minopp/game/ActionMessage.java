package cn.zbx1425.minopp.game;

import cn.zbx1425.minopp.platform.DummyLookupProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;


public record ActionMessage(Type type, Component message) {

    public ActionMessage(ValueInput input) {
        this(
            input.getString("type").map(Type::valueOf).orElse(Type.STATE),
            input.read("message", ComponentSerialization.CODEC).orElse(Component.empty())
        );
    }

    public void nbtWriteTo(ValueOutput output) {
        output.putString("type", type.name());
        output.store("message", ComponentSerialization.CODEC, message);
    }

    public enum Type {
        STATE,
        FAIL,
        MESSAGE_ALL;

        public boolean isEphemeral() {
            return this == FAIL || this == MESSAGE_ALL;
        }
    }

    public static final ActionMessage NO_GAME = new ActionMessage(Type.STATE, Component.translatable("game.minopp.play.no_game"));
}
