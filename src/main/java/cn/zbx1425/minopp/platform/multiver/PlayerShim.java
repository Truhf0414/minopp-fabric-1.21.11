package cn.zbx1425.minopp.platform.multiver;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class PlayerShim {

    public static void sendSystemMessage(Player player, Component message) {
        player.displayClientMessage(message, true);
    }

    public static UUID getGameProfileId(Player player) {
        return player.getGameProfile().id();
    }

    public static String getGameProfileName(Player player) {
        return player.getGameProfile().name();
    }

    public static boolean hasPermissions(Player player, int level) {
        return player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(level)));
    }

    public static ServerLevel serverLevel(ServerPlayer player) {
        return player.level();
    }
}
