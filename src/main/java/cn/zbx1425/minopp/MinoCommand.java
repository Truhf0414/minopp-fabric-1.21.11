package cn.zbx1425.minopp;

import cn.zbx1425.minopp.block.BlockEntityMinoTable;
import cn.zbx1425.minopp.block.BlockMinoTable;
import cn.zbx1425.minopp.platform.multiver.PlayerShim;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public class MinoCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("minopp")
                .then(Commands.literal("shout")
                        .executes(context -> {
                            boolean success = Mino.onServerChatMessage("mino", context.getSource().getPlayerOrException());
                            if (!success) throw new SimpleCommandExceptionType(Component.translatable("game.minopp.play.no_game")).create();
                            return 1;
                        }))
                .then(Commands.literal("set_table_award")
                    .requires((commandSourceStack) -> commandSourceStack.isPlayer()
                        && PlayerShim.hasPermissions(Objects.requireNonNull(commandSourceStack.getPlayer()), 2))
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        ItemStack holdingItem = player.getMainHandItem();
                        if (holdingItem.isEmpty()) {
                            context.getSource().sendFailure(Component.literal("Requirement: Hold an item"));
                            return 0;
                        }
                        if (!player.getBlockStateOn().is(Mino.BLOCK_MINO_TABLE.get())) {
                            context.getSource().sendFailure(Component.literal("Requirement: Stand on a table"));
                            return 0;
                        }
                        BlockPos corePos = BlockMinoTable.getCore(player.getBlockStateOn(), player.getOnPos());
                        if (PlayerShim.serverLevel(player).getBlockEntity(corePos) instanceof BlockEntityMinoTable tableEntity) {
                            tableEntity.award = holdingItem.copy();
                            tableEntity.sync();
                            context.getSource().sendSuccess(() -> Component.literal("Table award set"), true);
                            return 1;
                        } else {
                            context.getSource().sendFailure(Component.literal("Requirement: Stand on a table"));
                            return 0;
                        }
                    }))
                .then(Commands.literal("set_table_demo")
                        .requires((commandSourceStack) -> commandSourceStack.isPlayer()
                            && PlayerShim.hasPermissions(Objects.requireNonNull(commandSourceStack.getPlayer()), 2))
                        .then(Commands.argument("demo", BoolArgumentType.bool())
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            if (!player.getBlockStateOn().is(Mino.BLOCK_MINO_TABLE.get())) {
                                context.getSource().sendFailure(Component.literal("Requirement: Stand on a table"));
                                return 0;
                            }
                            BlockPos corePos = BlockMinoTable.getCore(player.getBlockStateOn(), player.getOnPos());
                            if (PlayerShim.serverLevel(player).getBlockEntity(corePos) instanceof BlockEntityMinoTable tableEntity) {
                                tableEntity.demo = BoolArgumentType.getBool(context, "demo");
                                tableEntity.sync();
                                context.getSource().sendSuccess(() -> Component.literal("Table demo set"), true);
                                return 1;
                            } else {
                                context.getSource().sendFailure(Component.literal("Requirement: Stand on a table"));
                                return 0;
                            }
                        })))
        );
    }
}
