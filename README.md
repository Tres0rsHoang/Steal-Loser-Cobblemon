# Steal Loser Cobblemon

A lightweight Fabric mod (Minecraft **1.21.1**) that integrates with **Cobblemon**.

After a **Cobblemon PvP battle** ends, the winner can choose **one of the loser’s battle-used Pokémon** to steal. The loser loses that Pokémon, and the winner receives it (sent to the winner’s PC if their party is full).

## Features

- Listens for Cobblemon battle results (PvP only)
- Shows the winner a clickable in-chat selection menu of the loser’s Pokémon used in the battle
- Adds the stolen Pokémon to the winner’s party, or to the winner’s PC if the party is full
- Removes the stolen Pokémon from the loser’s party

## Requirements

- Minecraft **1.21.1**
- Fabric Loader
- Fabric API
- Cobblemon (Fabric)

## Installation (Players / Server)

1. Install Fabric Loader for Minecraft 1.21.1.
2. Put these mods into your `mods` folder:
   - Cobblemon (Fabric)
   - Fabric API
   - This mod’s jar (built from this project, or from a release)
3. Launch the game / start the server.

## Usage

1. Start a Cobblemon PvP battle (Cobblemon provides the battle commands; e.g. `/pokebattle <player>`).
2. When the battle ends, the winner receives a message like:

   - `Choose 1 Pokemon from <loser>'s team:`
   - `[1] ...` `[2] ...` etc.
3. Pick a Pokémon by clicking a line in chat, or run:

   - `/stealmon pick <number>`

### Notes / Limitations

- This logic currently runs for **PvP battles only**.
- The loser is expected to remain online until the winner picks. (If the loser logs out before the pick, the steal may be cancelled depending on storage access.)

## Development

- Build jar:
  - Windows: `./gradlew.bat build`
  - macOS/Linux: `./gradlew build`
- Output jar: `build/libs/`

## License

See [LICENSE](LICENSE).
