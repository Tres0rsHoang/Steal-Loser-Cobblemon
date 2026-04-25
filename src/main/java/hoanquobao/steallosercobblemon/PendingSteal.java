package hoanquobao.steallosercobblemon;

import java.util.List;
import java.util.UUID;

import com.cobblemon.mod.common.pokemon.Pokemon;

public class PendingSteal {
    public final UUID winnerUUID;
    public final UUID loserUUID;
    public final List<Pokemon> battlePokemon; // Pokemon the loser used in battle

    public PendingSteal(UUID winnerUUID, UUID loserUUID, List<Pokemon> battlePokemon) {
        this.winnerUUID = winnerUUID;
        this.loserUUID = loserUUID;
        this.battlePokemon = battlePokemon;
    }
}
