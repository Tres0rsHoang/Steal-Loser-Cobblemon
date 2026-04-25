package hoanquobao.steallosercobblemon;

import java.util.ArrayList;
import java.util.List;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

public class BattleEventHandler {

    public static void register() {

        CobblemonEvents.BATTLE_VICTORY.subscribe(event -> {
            PokemonBattle battle = event.getBattle();

            // Collect all actors
            List<BattleActor> actors = new ArrayList<>();
            for (BattleActor actor : battle.getActors()) actors.add(actor);

            // Only handle PVP battles (all actors are players)
            boolean isPvP = actors.stream().allMatch(a -> a instanceof PlayerBattleActor);
            if (!isPvP) return;

            // Determine winner and loser
            List<BattleActor> winners = new ArrayList<>();
            for (BattleActor w : event.getWinners()) winners.add(w);

            BattleActor winnerActor = winners.isEmpty() ? null : winners.get(0);
            BattleActor loserActor = actors.stream()
                    .filter(a -> !winners.contains(a))
                    .findFirst()
                    .orElse(null);

            if (!(winnerActor instanceof PlayerBattleActor) || !(loserActor instanceof PlayerBattleActor)) {
                return;
            }

            ServerPlayer winner = ((PlayerBattleActor) winnerActor).getEntity();
            ServerPlayer loser  = ((PlayerBattleActor) loserActor).getEntity();

            if (winner == null || loser == null) return;

            // Collect the Pokemon the loser used in this battle
            // Must use getOriginalPokemon() — this is the actual party Pokemon instance.
            // getEffectedPokemon() returns a battle copy whose UUID may differ from
            // the party entry, causing the UUID check in StealCommand to always fail.
            List<Pokemon> loserBattlePokemon = new ArrayList<>();
            for (BattlePokemon bp : loserActor.getPokemonList()) {
                Pokemon p = bp.getOriginalPokemon();
                if (p != null) loserBattlePokemon.add(p);
            }

            if (loserBattlePokemon.isEmpty()) return;

            // Save pending steal state
            PendingSteal pending = new PendingSteal(
                winner.getUUID(), loser.getUUID(), loserBattlePokemon
            );
            StealCommand.pendingMap.put(winner.getUUID(), pending);

            // Send clickable Pokemon selection menu to the winner
            MutableComponent msg = Component.literal(
                "§6[Steal] §eChoose 1 Pokemon from §c"
                + loser.getName().getString() + "§e's team:\n"
            );

            for (int i = 0; i < loserBattlePokemon.size(); i++) {
                Pokemon p = loserBattlePokemon.get(i);
                String name = p.getDisplayName(false).getString();
                int level = p.getLevel();
                String cmd = "/stealmon pick " + (i + 1);

                MutableComponent choice = Component.literal(
                    "  §a[" + (i + 1) + "] §f" + name + " §7Lv." + level + "\n"
                ).withStyle(style -> style
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("§eClick to steal §6" + name + " §7Lv." + level)
                    ))
                );
                msg.append(choice);
            }

            msg.append(Component.literal("§7(Or use §e/stealmon pick <number>§7)"));
            winner.sendSystemMessage(msg);

            // Notify the whole server: send to chat (overlay=false) AND action bar (overlay=true)
            // so the message is visible everywhere, similar to a player-join notification.
            var server = winner.getServer();
            if (server != null) {
                Component broadcastMsg = Component.literal(
                    "§6[Steal] §e" + winner.getName().getString()
                    + " §7defeated §c" + loser.getName().getString()
                    + " §7and may steal a Pokémon!"
                );
                // Chat
                server.getPlayerList().broadcastSystemMessage(broadcastMsg, false);
                // Action bar overlay for every online player
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    p.sendSystemMessage(broadcastMsg, true);
                }
            }
        });
    }
}


