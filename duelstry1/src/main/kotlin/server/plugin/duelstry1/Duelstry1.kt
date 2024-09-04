package server.plugin.command

import org.bukkit.*
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

class GameModeSelectorPlugin : JavaPlugin(), CommandExecutor, Listener {

    // HashMap to store player kills: player UUID -> kill count
    private val playerKills = HashMap<UUID, Int>()

    override fun onEnable() {
        // Register the commands
        getCommand("selectmode")?.setExecutor(this)
        getCommand("leaderboard")?.setExecutor(this)

        // Register the event listener
        server.pluginManager.registerEvents(this, this)
        logger.info("GameModeSelector enabled")
    }

    override fun onDisable() {
        logger.info("GameModeSelector disabled")
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("This command can only be used by players.")
            return true
        }

        val player = sender

        when (command.name.lowercase()) {
            "selectmode" -> {
                openModeSelectionMenu(player)
            }
            "leaderboard" -> {
                displayLeaderboard(player)
            }
        }
        return true
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val item = player.inventory.itemInMainHand

        if (item.type == Material.IRON_SWORD) {
            openModeSelectionMenu(player)
        }
    }

    private fun openModeSelectionMenu(player: Player) {
        val inventory = Bukkit.createInventory(null, 9, ChatColor.GREEN.toString() + "Select Your Mode")

        val modes = listOf("Nethpot", "CPvP", "UHC", "Warrior", "Sumo")
        val materials = listOf(
            Material.NETHERITE_SWORD,
            Material.DIAMOND_SWORD,
            Material.GOLDEN_APPLE,
            Material.SHIELD,
            Material.LEATHER_HELMET
        )

        for (i in modes.indices) {
            val modeItem = ItemStack(materials[i])
            val meta = modeItem.itemMeta
            meta?.setDisplayName(ChatColor.GOLD.toString() + modes[i])
            modeItem.itemMeta = meta
            inventory.setItem(i, modeItem)
        }

        player.openInventory(inventory)
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.view.title != ChatColor.GREEN.toString() + "Select Your Mode") return

        event.isCancelled = true

        val player = event.whoClicked as Player
        val clickedItem = event.currentItem ?: return

        if (clickedItem.itemMeta != null) {
            val mode = ChatColor.stripColor(clickedItem.itemMeta!!.displayName)
            player.sendMessage("${ChatColor.YELLOW}You have selected the mode: $mode")
            teleportPlayerToGameModeWorld(player, mode!!)
        }

        player.closeInventory()
    }

    private fun teleportPlayerToGameModeWorld(player: Player, mode: String) {
        val worldName = mode.lowercase()
        val world = Bukkit.getWorld(worldName) ?: Bukkit.createWorld(WorldCreator(worldName))
        val spawnLocation = world?.spawnLocation ?: return

        player.teleport(spawnLocation)
        player.sendMessage("${ChatColor.GREEN}You have been teleported to the $mode world!")

        // Prepare the player for the fight
        player.inventory.clear()
        givePlayerStartingItems(player, mode)
    }

    private fun givePlayerStartingItems(player: Player, mode: String) {
        when (mode.lowercase()) {
            "nethpot" -> {
                player.inventory.addItem(ItemStack(Material.NETHERITE_SWORD))
                player.inventory.addItem(ItemStack(Material.POTION, 16))
            }
            "cpvp" -> {
                player.inventory.addItem(ItemStack(Material.DIAMOND_SWORD))
                player.inventory.addItem(ItemStack(Material.GOLDEN_APPLE, 10))
            }
            "uhc" -> {
                player.inventory.addItem(ItemStack(Material.IRON_SWORD))
                player.inventory.addItem(ItemStack(Material.BOW))
                player.inventory.addItem(ItemStack(Material.ARROW, 64))
            }
            "warrior" -> {
                player.inventory.addItem(ItemStack(Material.STONE_SWORD))
                player.inventory.addItem(ItemStack(Material.SHIELD))
            }
            "sumo" -> {
                player.inventory.addItem(ItemStack(Material.LEATHER_HELMET))
            }
        }
    }

    // Track player kills
    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val killer = event.entity.killer ?: return
        val killerId = killer.uniqueId

        playerKills[killerId] = playerKills.getOrDefault(killerId, 0) + 1
    }

    // Display the leaderboard
    private fun displayLeaderboard(player: Player) {
        val sortedKills = playerKills.entries.sortedByDescending { it.value }.take(10)

        player.sendMessage("${ChatColor.GREEN}Top 10 Kill Leaders:")
        for ((index, entry) in sortedKills.withIndex()) {
            val playerName = Bukkit.getOfflinePlayer(entry.key).name
            player.sendMessage("${ChatColor.YELLOW}${index + 1}. $playerName - ${entry.value} kills")
        }
    }
}

