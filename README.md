# EnchantmentBlocker

EnchantmentBlocker controls which enchantments can be added to weapon and armor item types through vanilla enchanting tables and anvils. Each supported equipment type has an independent enable switch and enchantment allowlist. The plugin is lightweight, has no required plugin dependencies, and applies the same rules in every dimension.

| Property | Value |
| --- | --- |
| Version | `12A-enchant.v7` |
| Author | `voidles02` |
| Server API | Paper 1.21+ (built against Paper 1.21.4) |
| Java runtime | Java 25 |
| Required plugins | None |

## Installation and configuration

1. Put the plugin JAR in the server's `plugins` directory.
2. Start or restart the server. The plugin creates `plugins/EnchantmentBlocker/config.yml` on first startup.
3. Edit the configuration, then apply changes with `/enchantmentblocker reload`.

The generated configuration includes separate rules for swords, axes, bows, crossbows, tridents, maces, helmets, chestplates, leggings, boots, shields, elytra, horse armor, and wolf armor. Each equipment type has:

- `enabled`: whether that type is protected.
- `enchantments`: enchantment keys to block for that type.

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
          - sharpness
          - smite
          - mending
      bows:
        enabled: true
        enchantments:
          - power
          - flame
    armor:
      boots:
        enabled: true
        enchantments:
          - protection
          - feather_falling
          - mending
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
| `block.types.<group>.<type>.enchantments` | See generated config | Enchantment keys blocked on that type. |

Recognized type paths are:

| Item type | Configuration path suffix |
| --- | --- |
| Every sword material | `weapons.swords` |
| Every axe material | `weapons.axes` |
| Bow | `weapons.bows` |
| Crossbow | `weapons.crossbows` |
| Trident | `weapons.tridents` |
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

Enchantment names are lowercase Minecraft keys without the `minecraft:` prefix, such as `sharpness`, `swift_sneak`, and `vanishing_curse`. Namespaced keys can also be used for registered custom enchantments, for example `myplugin:poison`. The special value `"*"` blocks every enchantment reported by Bukkit for that item. An empty list blocks none. To block a specific enchantment, remove it from the corresponding list; enchantments omitted from the list are allowed.

The top-level weapon/armor and method switches must also be on for a type rule to apply. A type can be individually disabled without changing other types. A missing type enchantment list in an older configuration defaults to `"*"` to preserve the plugin's former behavior; add the desired list under `block.types` to customize it.

## Enforcement behavior

- **Enchanting table:** Cancels an enchantment attempt only when at least one proposed enchantment is in the matching type's blocked list.
- **Anvil:** Removes a prepared result if it adds a blocked enchantment or increases its level compared with the item in the first input slot. This includes enchantments transferred from the second input.
- **Anvil output:** Checks attempts to collect the result as an additional guard, so a modified output cannot be taken when it adds a configured blocked enchantment.
- **Repairs and renames:** Remain available when they do not add or increase a blocked enchantment.
- **Dimensions:** No world or dimension filter is used. The same rules apply anywhere the relevant vanilla inventory events occur.

The checks only process relevant inventory events and use cached material-to-rule mappings and cached enchantment sets. Configuration is parsed when the plugin starts or reloads, not during each event.

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

The plugin creates no repeating tasks, background threads, database connections, or player caches. Bukkit unregisters its event listener when the plugin is disabled; on restart, the configuration and cached rules are rebuilt during enable. There are no dimension-specific resources or shutdown jobs to leave behind.

Enforcement covers vanilla enchanting-table and anvil events. Other plugins or commands that directly edit item data, custom enchantment systems that bypass Bukkit's enchantment events, or another plugin that deliberately changes inventory output after this plugin checks it can operate outside these checks. As with any Bukkit plugin, conflicts from plugins that rewrite the same event cannot be guaranteed against universally.

## Versioning

Current version: `12A-enchant.v6`. For each future release change, increment the suffix (`12A-enchant.v7`, `12A-enchant.v8`, and so on) and keep the version in `build.gradle.kts`, `plugin.yml`, and this README identical.