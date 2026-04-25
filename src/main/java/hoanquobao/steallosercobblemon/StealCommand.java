package hoanquobao.steallosercobblemon;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class StealCommand {

    // Pending steal state: winnerUUID -> PendingSteal
    public static final Map<UUID, PendingSteal> pendingMap = new HashMap<>();

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(Commands.literal("stealmon")
                .then(Commands.literal("pick")
                    .then(Commands.argument("choice", IntegerArgumentType.integer(1, 6))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            int choice = IntegerArgumentType.getInteger(ctx, "choice");
                            return handlePick(player, choice);
                        })
                    )
                )
            )
        );
    }

    private static int handlePick(ServerPlayer winner, int choice) {
        UUID winnerUUID = winner.getUUID();
        PendingSteal pending = pendingMap.get(winnerUUID);

        if (pending == null) {
            winner.sendSystemMessage(Component.literal("§cYou have no pending Pokemon steal!"));
            return 0;
        }

        if (choice < 1 || choice > pending.battlePokemon.size()) {
            winner.sendSystemMessage(Component.literal(
                "§cInvalid choice! Please choose between 1 and " + pending.battlePokemon.size()
            ));
            return 0;
        }

        Pokemon chosenPokemon = pending.battlePokemon.get(choice - 1);

        // Get winner's party via ServerPlayer
        PlayerPartyStore winnerParty = Cobblemon.INSTANCE.getStorage().getParty(winner);
        boolean partyFull = winnerParty.size() >= 6;

        // Check if the chosen Pokemon is still in the loser's party
        ServerPlayer loserOnline = winner.getServer().getPlayerList().getPlayer(pending.loserUUID);
        PlayerPartyStore loserParty;
        if (loserOnline != null) {
            loserParty = Cobblemon.INSTANCE.getStorage().getParty(loserOnline);
        } else {
            // Loser is offline - cannot access their party, cancel steal
            winner.sendSystemMessage(Component.literal(
                "§cThe losing player has gone offline. Steal cancelled!"
            ));
            pendingMap.remove(winnerUUID);
            return 0;
        }
        boolean stillInParty = false;
        for (Pokemon p : loserParty) {
            if (p != null && p.getUuid().equals(chosenPokemon.getUuid())) {
                stillInParty = true;
                break;
            }
        }

        if (!stillInParty) {
            winner.sendSystemMessage(Component.literal(
                "§cThat Pokemon is no longer in the loser's party!"
            ));
            pendingMap.remove(winnerUUID);
            return 0;
        }

        // Remove Pokemon from loser's party
        loserParty.remove(chosenPokemon);

        // Add Pokemon to winner's party or PC if party is full
        boolean sentToPC = false;
        if (partyFull) {
            PCStore winnerPC = Cobblemon.INSTANCE.getStorage().getPC(winner);
            winnerPC.add(chosenPokemon);
            sentToPC = true;
        } else {
            winnerParty.add(chosenPokemon);
        }

        // Clear pending steal state
        pendingMap.remove(winnerUUID);

        String pokemonName = chosenPokemon.getDisplayName(false).getString();

        // Notify winner
        if (sentToPC) {
            winner.sendSystemMessage(Component.literal(
                "§a[Steal] §eParty full! §6" + pokemonName + " §7Lv." + chosenPokemon.getLevel()
                + " §ehas been sent to your §bPC§e!"
            ));
        } else {
            winner.sendSystemMessage(Component.literal(
                "§a[Steal] §eYou obtained §6" + pokemonName + " §7Lv." + chosenPokemon.getLevel() + "§e!"
            ));
        }

        // Notify loser if online
        if (loserOnline != null) {
            loserOnline.sendSystemMessage(Component.literal(
                "§c[Steal] §e" + winner.getName().getString()
                + " §cstole §6" + pokemonName + " §cfrom your party!"
            ));
        }

        StealLoserCobblemon.LOGGER.info("[Steal] {} stole {} (Lv.{}) from {}",
            winner.getName().getString(),
            pokemonName,
            chosenPokemon.getLevel(),
            loserOnline != null ? loserOnline.getName().getString() : pending.loserUUID.toString()
        );

        return 1;
    }
}
