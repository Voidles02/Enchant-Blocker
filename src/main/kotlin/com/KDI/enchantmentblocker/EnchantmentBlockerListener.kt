package com.KDI.enchantmentblocker

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.AnvilInventory
import org.bukkit.inventory.ItemStack
import java.util.EnumMap

class EnchantmentBlockerListener(plugin: EnchantmentBlockerPlugin) : Listener {
    private var blockWeapons = true
    private var blockArmor = true
    private var blockEnchantingTable = true
    private var blockAnvil = true
    private var materialRules: Map<Material, EnchantmentRule> = emptyMap()

    init {
        reloadSettings(plugin)
    }

    fun reloadSettings(plugin: EnchantmentBlockerPlugin) {
        blockWeapons = plugin.config.getBoolean("block.weapons", true)
        blockArmor = plugin.config.getBoolean("block.armor", true)
        blockEnchantingTable = plugin.config.getBoolean("block.enchanting-table", true)
        blockAnvil = plugin.config.getBoolean("block.anvil", true)

        val rulesByType = HashMap<String, EnchantmentRule>()
        val updatedRules = EnumMap<Material, EnchantmentRule>(Material::class.java)
        for ((material, type) in MATERIAL_TYPES) {
            val rule = rulesByType.getOrPut(type) {
                val path = "block.types.$type"
                val enchantmentPath = "$path.enchantments"
                val enchantments = if (plugin.config.contains(enchantmentPath)) {
                    plugin.config.getStringList(enchantmentPath)
                } else {
                    listOf("*")
                }
                EnchantmentRule(
                    enabled = plugin.config.getBoolean("$path.enabled", true),
                    armor = type.startsWith("armor."),
                    enchantments = enchantments.asSequence()
                        .map(::normalizeEnchantment)
                        .filter(String::isNotEmpty)
                        .toSet()
                )
            }
            updatedRules[material] = rule
        }
        materialRules = updatedRules
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onEnchantItem(event: EnchantItemEvent) {
        val rule = activeRule(event.item) ?: return
        if (blockEnchantingTable && event.enchantsToAdd.keys.any(rule::blocks)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPrepareAnvil(event: PrepareAnvilEvent) {
        if (!blockAnvil) return

        val result = event.result ?: return
        if (addsBlockedEnchantment(result, event.inventory)) {
            event.result = null
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onAnvilResultClick(event: InventoryClickEvent) {
        if (!blockAnvil || event.view.type != InventoryType.ANVIL || event.rawSlot != 2) return

        val result = event.currentItem ?: return
        val inventory = event.view.topInventory as? AnvilInventory ?: return
        if (addsBlockedEnchantment(result, inventory)) {
            event.isCancelled = true
        }
    }

    private fun addsBlockedEnchantment(result: ItemStack, inventory: AnvilInventory): Boolean {
        val rule = activeRule(result) ?: return false
        val firstInput = inventory.getItem(0)
        return result.enchantments.any { (enchantment, level) ->
            rule.blocks(enchantment) && level > (firstInput?.getEnchantmentLevel(enchantment) ?: 0)
        }
    }

    private fun activeRule(item: ItemStack): EnchantmentRule? {
        if (item.type.isAir) return null
        val rule = materialRules[item.type] ?: return null
        if (!rule.enabled || (rule.armor && !blockArmor) || (!rule.armor && !blockWeapons)) return null
        return rule
    }

    private data class EnchantmentRule(
        val enabled: Boolean,
        val armor: Boolean,
        val enchantments: Set<String>
    ) {
        fun blocks(enchantment: Enchantment): Boolean {
            val key = normalizeEnchantment(enchantment.key.toString())
            return "*" in enchantments || key in enchantments
        }
    }

    private companion object {
        val MATERIAL_TYPES: Map<Material, String> = EnumMap<Material, String>(Material::class.java).apply {
            Material.values().forEach { material ->
                materialType(material)?.let { put(material, it) }
            }
        }

        fun normalizeEnchantment(value: String): String = value.trim().lowercase().removePrefix("minecraft:")

        fun materialType(material: Material): String? {
            val name = material.name
            return when {
                name.endsWith("_SWORD") -> "weapons.swords"
                name.endsWith("_AXE") -> "weapons.axes"
                name == "BOW" -> "weapons.bows"
                name == "CROSSBOW" -> "weapons.crossbows"
                name == "TRIDENT" -> "weapons.tridents"
                name == "MACE" -> "weapons.maces"
                name.endsWith("_HELMET") -> "armor.helmets"
                name.endsWith("_CHESTPLATE") -> "armor.chestplates"
                name.endsWith("_LEGGINGS") -> "armor.leggings"
                name.endsWith("_BOOTS") -> "armor.boots"
                name == "SHIELD" -> "armor.shields"
                name == "ELYTRA" -> "armor.elytra"
                name == "WOLF_ARMOR" -> "armor.wolf-armor"
                name.endsWith("_HORSE_ARMOR") -> "armor.horse-armor"
                else -> null
            }
        }
    }
}