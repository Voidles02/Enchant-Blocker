package com.KDI.enchantmentblocker

import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class EnchantmentBlockerPlugin : JavaPlugin(), CommandExecutor {
    private lateinit var listener: EnchantmentBlockerListener

    override fun onEnable() {
        saveDefaultConfig()
        listener = EnchantmentBlockerListener(this)
        server.pluginManager.registerEvents(listener, this)
        checkNotNull(getCommand("enchantmentblocker")) {
            "Command 'enchantmentblocker' is missing from plugin.yml"
        }.setExecutor(this)
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage("§cYou don't have permission to use this command.")
            return true
        }

        when (args.firstOrNull()?.lowercase()) {
            "help" -> sendUsage(sender)

            "version" -> if (args.size == 1) {
                sender.sendMessage("$PREFIX Version ${description.version}.")
            } else sendUsage(sender)

            "enable" -> if (args.size == 1) {
                updateAllSettings(sender, true, "All blocking settings enabled.")
            } else sendUsage(sender)

            "disable" -> if (args.size == 1) {
                updateAllSettings(sender, false, "All blocking settings disabled.")
            } else sendUsage(sender)

            "reset" -> if (args.size == 1) {
                updateAllSettings(sender, true, "Default blocking settings restored.")
            } else sendUsage(sender)

            "reload" -> if (args.size == 1) {
                reloadConfig()
                listener.reloadSettings(this)
                sender.sendMessage("$PREFIX Configuration reloaded.")
            } else sendUsage(sender)

            "status" -> if (args.size == 1) {
                sender.sendMessage("$PREFIX weapons=${toggleState("block.weapons")}, armor=${toggleState("block.armor")}")
                sender.sendMessage("$PREFIX table=${toggleState("block.enchanting-table")}, anvil=${toggleState("block.anvil")}")
            } else sendUsage(sender)

            "weapons" -> handleToggle(sender, args, "block.weapons", "weapon")
            "armor" -> handleToggle(sender, args, "block.armor", "armor")
            "methods" -> handleMethodToggle(sender, args)
            else -> sendUsage(sender)
        }
        return true
    }

    private fun handleToggle(sender: CommandSender, args: Array<out String>, path: String, name: String) {
        if (args.size != 2) {
            sendUsage(sender)
            return
        }
        val enabled = parseToggle(args[1])
        if (enabled == null) {
            sender.sendMessage("$PREFIX Use on or off.")
            return
        }
        updateSetting(sender, path, enabled, "$name blocking")
    }

    private fun handleMethodToggle(sender: CommandSender, args: Array<out String>) {
        if (args.size != 3) {
            sendUsage(sender)
            return
        }
        val path = when (args[1].lowercase()) {
            "table" -> "block.enchanting-table"
            "anvil" -> "block.anvil"
            else -> null
        }
        val enabled = parseToggle(args[2])
        if (path == null || enabled == null) {
            sender.sendMessage("$PREFIX Use methods <table|anvil> <on|off>.")
            return
        }
        updateSetting(sender, path, enabled, "${args[1].lowercase()} blocking")
    }

    private fun updateSetting(sender: CommandSender, path: String, enabled: Boolean, name: String) {
        config.set(path, enabled)
        saveConfig()
        listener.reloadSettings(this)
        sender.sendMessage("$PREFIX $name ${if (enabled) "enabled" else "disabled"}.")
    }

    private fun updateAllSettings(sender: CommandSender, enabled: Boolean, message: String) {
        BLOCK_SETTING_PATHS.forEach { config.set(it, enabled) }
        saveConfig()
        listener.reloadSettings(this)
        sender.sendMessage("$PREFIX $message")
    }

    private fun toggleState(path: String): String = if (config.getBoolean(path, true)) "on" else "off"

    private fun parseToggle(value: String): Boolean? = when (value.lowercase()) {
        "on" -> true
        "off" -> false
        else -> null
    }

    private fun sendUsage(sender: CommandSender) {
        sender.sendMessage("$PREFIX Commands: /enchantmentblocker <help|version|enable|disable|reset>")
        sender.sendMessage("$PREFIX /enchantmentblocker <reload|status|weapons|armor|methods>")
        sender.sendMessage("$PREFIX methods <table|anvil> <on|off>; weapons/armor <on|off>")
    }

    private companion object {
        const val ADMIN_PERMISSION = "enchantmentblocker.admin"
        const val PREFIX = "§8[§bEnchantmentBlocker§8] §7"
        val BLOCK_SETTING_PATHS = listOf(
            "block.weapons",
            "block.armor",
            "block.enchanting-table",
            "block.anvil"
        )
    }
}