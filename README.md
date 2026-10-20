# EnchantmentBlocker

EnchantmentBlocker controls which enchantments can be added to weapon and armor item types through vanilla enchanting tables and anvils. Each supported equipment type has an independent enable switch and enchantment allowlist. The plugin is lightweight, has no required plugin dependencies, and applies the same rules in every dimension.

| Property | Value |
| --- | --- |
| Version | `12A-enchant.v11` |
| Author | `voidles02` |
| Server API | Paper 1.21+ (built against Paper 1.21.4) |
| Java runtime | Java 25 |
| Required plugins | None |

## Installation and configuration

1. Put the plugin JAR in the server's `plugins` directory.
2. Start or restart the server. The plugin creates `plugins/EnchantmentBlocker/config.yml` on first startup.
3. Edit the configuration, then apply changes with `/enchantmentblocker reload`.

The generated configuration includes separate rules for swords, axes, bows, crossbows, tridents, spears, maces, helmets, chestplates, leggings, boots, shields, elytra, horse armor, and wolf armor. Each equipment type has:

- `enabled`: whether that type is protected.
- `enchantments`: enchantment keys mapped to the first level that is blocked for that type.

Example:

```yaml
block:
  weapons: true
  armor: true
  enchanting-table: true
  anvil: true
  types:
    weapons:
      swords:
        enabled: true
        enchantments:
          sharpness: 1
          smite: 1
          mending: 2
      bows:
        enabled: true
        enchantments:
          power: 1
          flame: 1
      spears:
        enabled: true
        enchantments:
          lunge: 1
          sharpness: 1
    armor:
      boots:
        enabled: true
        enchantments:
          protection: 1
          feather_falling: 1
          mending: 2
```

The example is abbreviated; keep or add the other type sections from the generated configuration as needed.

### Configuration reference

| Setting | Default | Effect |
| --- | --- | --- |
| `block.weapons` | `true` | Master switch for every weapon type. |
| `block.armor` | `true` | Master switch for every armor type. |
| `block.enchanting-table` | `true` | Enforce configured rules for table enchantments. |
| `block.anvil` | `true` | Enforce configured rules for enchantments added or upgraded in an anvil. |
| `block.types.<group>.<type>.enabled` | `true` | Enable or disable a single equipment type. |
| `block.types.<group>.<type>.enchantments` | See generated config | Enchantment keys mapped to their first blocked level for that type. |

Recognized type paths are:

| Item type | Configuration path suffix |
| --- | --- |
| Every sword material | `weapons.swords` |
| Every axe material | `weapons.axes` |
| Bow | `weapons.bows` |
| Crossbow | `weapons.crossbows` |
| Trident | `weapons.tridents` |
| Spears (all material variants recognized by the server API) | `weapons.spears` |
| Mace | `weapons.maces` |
| Every helmet, including turtle helmets | `armor.helmets` |
| Every chestplate | `armor.chestplates` |
| Every pair of leggings | `armor.leggings` |
| Every pair of boots | `armor.boots` |
| Shield | `armor.shields` |
| Elytra | `armor.elytra` |
| Horse armor | `armor.horse-armor` |
| Wolf armor | `armor.wolf-armor` |

For example, `block.types.weapons.swords.enchantments` affects wooden through netherite swords. Item material variants are detected by their Bukkit material type; one rule is shared by all variants of that equipment type.

Enchantment names are lowercase Minecraft keys without the `minecraft:` prefix, such as `sharpness`, `swift_sneak`, and `vanishing_curse`. Each value is the minimum level to block: `wind_burst: 2` blocks levels 2 and above while allowing level 1. Set a value to `1` to block every normal level of that enchantment. Namespaced keys can also be used for registered custom enchantments, for example `"myplugin:poison": 2`. The special key `"*": 1` blocks every enchantment reported by Bukkit for that item. An empty mapping blocks none; enchantments omitted from a type's mapping are allowed. Existing configurations using a list of enchantment names remain supported, and each listed name is treated as blocked from level 1.

The top-level weapon/armor and method switches must also be on for a type rule to apply. A type can be individually disabled without changing other types. A missing type enchantment list in an older configuration defaults to `"*"` to preserve the plugin's former behavior; add the desired list under `block.types` to customize it.

## Enforcement behavior

- **Enchanting table:** Cancels an attempt only when a proposed enchantment reaches its configured blocked level for that item type.
- **Book-to-book anvils:** The plugin preserves the server's vanilla book-merging result, including merging matching levels (for example, Protection II + Protection II can become Protection III) and combining different enchantments on one book.
- **Enchanted books on gear:** Each stored enchantment is accepted only when `Enchantment.canEnchantItem` allows it on the target and it does not conflict with an enchantment already on the target or another accepted book enchantment. Incompatible enchantments are removed from the prepared result; compatible enchantments remain. If none can be applied, the output is empty.
- These vanilla compatibility checks always apply to enchanted books, even when the configurable `block.anvil` rule switch is off; that switch controls only the plugin's configured enchantment-blocking rules.
- **Examples:** Breach IV on a netherite sword produces no result, while Breach IV on a mace is allowed. A book containing both Sharpness and Smite applies only the enchantment that does not conflict with the target and other accepted book enchantments.
- **Anvil rules:** After compatibility filtering, the plugin removes the result if it adds or increases an enchantment to a configured blocked level compared with the first input. This includes enchantments transferred from the second input.
- **Anvil output:** Rechecks the server-side result and current inputs when a player collects the output, including clicks already cancelled by another listener. It does not trust a client-supplied item stack; latency alone cannot bypass the check.
- **XP cost and other anvil behavior:** The plugin does not modify the vanilla anvil cost or alter repair/rename behavior when a compatible result is allowed.
- **Repairs and renames:** Remain available when they do not add or increase a blocked enchantment.
- **Dimensions:** No world or dimension filter is used. The same rules apply anywhere the relevant vanilla inventory events occur.

The checks only process relevant inventory events and use cached material-to-rule mappings. Configuration is parsed when the plugin starts or reloads, not during each event. The anvil filter avoids an unnecessary target metadata lookup and skips sorting when a book has only one stored enchantment. These are small targeted reductions in event-time work, not a benchmarked 50× improvement. No per-player tracker/cache is maintained, avoiding extra memory growth.

## Commands

Every command requires `enchantmentblocker.admin`.

| Command | Description |
| --- | --- |
| `/enchantmentblocker help` | Display command usage. |
| `/enchantmentblocker version` | Display the installed plugin version. |
| `/enchantmentblocker enable` | Turn on all master blocking switches. |
| `/enchantmentblocker disable` | Turn off all master blocking switches. |
| `/enchantmentblocker reset` | Restore the four master switches to their default enabled state. Type-specific rules are retained. |
| `/enchantmentblocker reload` | Reload `config.yml` and apply rules immediately. |
| `/enchantmentblocker status` | Show the weapon, armor, table, and anvil master switches. |
| `/enchantmentblocker weapons <on\|off>` | Toggle the weapon master switch. |
| `/enchantmentblocker armor <on\|off>` | Toggle the armor master switch. |
| `/enchantmentblocker methods <table\|anvil> <on\|off>` | Toggle enforcement for one method. |

Admin command changes are saved to `config.yml` and applied immediately. Configure individual equipment types and enchantment lists in the file, then run `reload`.

## Permissions and LuckPerms

The single admin permission is `enchantmentblocker.admin`; it defaults to server operators. LuckPerms is optional and can manage it with:

```text
/lp group admin permission set enchantmentblocker.admin true
/lp user <player> permission set enchantmentblocker.admin true
```

Set the permission to `false` to revoke it. The plugin uses Bukkit's standard permission checks, so LuckPerms can grant or deny access without being a hard dependency.

## Reliability and limitations

The plugin creates no repeating tasks, background threads, database connections, player caches, or tracking subsystem. Bukkit unregisters its event listener when the plugin is disabled; on restart, the configuration and cached rules are rebuilt during enable. There are no dimension-specific resources or shutdown jobs to leave behind.

Enforcement covers vanilla enchanting-table and anvil events. Other plugins or commands that directly edit item data, custom enchantment systems that bypass Bukkit's enchantment events, or another plugin that deliberately changes inventory output after this plugin checks it can operate outside these checks. As with any Bukkit plugin, conflicts from plugins that rewrite the same event cannot be guaranteed against universally.

## Versioning

Current version: `12A-enchant.v11`. For each future release change, increment the suffix (`12A-enchant.v12`, `12A-enchant.v13`, and so on) and keep the version in `build.gradle.kts`, `plugin.yml`, and this README identical.