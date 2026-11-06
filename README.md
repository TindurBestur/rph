# ResourcePackHub

Standalone Paper plugin project for Paper 1.21.11 / Java 21.

## Build

1. Install JDK 21.
2. Install Gradle 8.10+ (or generate a Gradle wrapper locally with `gradle wrapper`).
3. Run `gradle clean test build`.
4. Find the plugin at `build/libs/ResourcePackHub-1.0.0.jar`.

GitHub Actions builds and tests the project on every push / pull request. Download the JAR from the workflow run's **Artifacts** section.

## Install

1. Stop your Paper server.
2. Put the JAR in `plugins/`.
3. Start the server once to generate `plugins/ResourcePackHub/`.
4. Edit `config.yml`.
5. Restart the server.

## Main command

`/resourcepackhub` shows the plugin version and help. `/rp` manages configured resource packs.

## Resource packs

Configure packs in `config.yml` under `settings.resource-packs.packs`. Each pack needs:
- `url`: publicly reachable direct HTTPS ZIP URL
- `sha1`: SHA-1 hash of the ZIP (40 hexadecimal characters)
- `required`: whether clients must accept it

Enable packs with `/rp enable <name>` and disable with `/rp disable <name>`. `/rp enable all` applies all configured packs to online players. Minecraft clients can only have one server pack active at a time through the normal single-pack API; pack stacking is client/version dependent. This initial implementation sends the selected pack to players, it does not merge ZIP files.

For `/rp giveitem <pack> <model> <material>`, configure `settings.item-models.<model>` as a namespaced key, for example `weapons:dragon_sword`. The item model key must exist in the pack's assets.

## Rank permissions

The rank manager stores rank definitions and player assignments in `ranks.yml`. It supports rank creation, deletion, assignment, prefix changes and permission nodes. Bukkit permissions are registered dynamically for online players when ranks are applied. Rank inheritance and temporary expiry are not part of this initial codebase.

## Command block whitelist

`/commandwhitelist add <command>`, `remove`, `list`, and `reload` manage the allowed command list. In whitelist mode, command-block and command-block-minecart commands not on the list are cancelled. This does not restrict player commands or console commands.

## Data

Homes, warps, moderation records and settings are stored in YAML files in the plugin data folder. Back up this folder before upgrades.

## Notes

This project provides a functional foundation for the listed core systems, not a claim that every feature from the original wishlist (economy, Vault economy provider, kits, GUI editor, PlaceholderAPI expansion, Discord integration, backup scheduler, anti-bot system, etc.) is implemented. Those integrations require additional dedicated implementations and tests.
