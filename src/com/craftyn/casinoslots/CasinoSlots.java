package com.craftyn.casinoslots;

import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.craftyn.casinoslots.command.AnCommandExecutor;
import com.craftyn.casinoslots.listeners.AnBlockListener;
import com.craftyn.casinoslots.listeners.AnChunkListener;
import com.craftyn.casinoslots.listeners.AnPlayerListener;
import com.craftyn.casinoslots.listeners.EntityListener;
import com.craftyn.casinoslots.slot.RewardData;
import com.craftyn.casinoslots.slot.SlotData;
import com.craftyn.casinoslots.slot.TypeData;
import com.craftyn.casinoslots.util.ConfigData;
import com.craftyn.casinoslots.util.Permissions;
import com.craftyn.casinoslots.util.StatData;

public class CasinoSlots extends JavaPlugin {
	
	protected CasinoSlots plugin;
	public Economy economy = null;
	public Server server;
	private final Logger logger = Logger.getLogger("Minecraft");
	private PluginManager pm = null;
	
	public String consolePrefix = "[CasinoSlots] ";
	public String pluginVer;
	
	private AnPlayerListener playerListener = new AnPlayerListener(this);
	private AnBlockListener blockListener = new AnBlockListener(this);
	private AnChunkListener chunkListener = new AnChunkListener(this);
	private EntityListener entity = new EntityListener(this);
	private AnCommandExecutor commandExecutor = new AnCommandExecutor(this);
	
	public ConfigData configData = new ConfigData(this);
	public SlotData slotData = new SlotData(this);
	public TypeData typeData = new TypeData(this);
	public StatData statsData = new StatData(this);
	public RewardData rewardData = new RewardData(this);
	public Permissions permission = new Permissions(this);

	public void onDisable() {
		if (economy != null) {
			//configData.save();
			configData.saveSlots();
			configData.saveStats();
			
			this.configData = null;
			this.slotData = null;
			this.typeData = null;
			this.statsData = null;
			this.rewardData = null;
			this.permission = null;
		}
	}

	public void onEnable() {
		server = this.getServer();
		pm = this.getServer().getPluginManager();
		if(!pm.isPluginEnabled("Vault")) {
			error("Vault is required in order to use this plugin.");
			error("dev.bukkit.org/server-mods/vault/");
			pm.disablePlugin(this);
			return;
		} else {
			if(!setupEconomy()) {
				error("An economy plugin is required in order to use this plugin.");
				pm.disablePlugin(this);
				return;
			}
		}
		configData.load();
		saveConfig();
		if(configData.inDebug()) debug("Debugging enabled.");
		
		pm.registerEvents(playerListener, this);
		pm.registerEvents(blockListener, this);
		pm.registerEvents(chunkListener, this);
		pm.registerEvents(entity, this);
		
		
		getCommand("casino").setExecutor(commandExecutor);
		pluginVer = getDescription().getVersion();
	}
	
	// Provides a way to shutdown the server from some other class
	public void disablePlugin() {
		if (pm == null) {
			log("Sorry couldn't disable the plugin for some odd reason. :(");
		}else {
			pm.disablePlugin(this);
		}
	}
	
	/**
	 * Sends a properly formatted message to the player.
	 *
	 * @param player The player to send the message to
	 * @param message The message to send to the player
	 */
	public void sendMessage(Player player, String message) {		
		message = configData.prefixColor + configData.prefix + configData.chatColor + " " + message;
		message = message.replaceAll("(?i)&([0-9abcdefklmnor])", "\u00A7$1");
		player.sendMessage(message);
	}
	
	public void debug(String message) {
		logger.info("-debug- " + consolePrefix + message);
	}
	
	/**
	 * Logs a properly formatted message to the console with a info prefix.
	 *
	 * @param message The info message to log.
	 */
	public void log(String message) {
		logger.info(consolePrefix + message);
	}
	
	/**
	 * Logs a properly formatted message to the console with a error prefix.
	 *
	 * @param message The warning message to log.
	 */
	public void error(String message) {
		logger.warning(consolePrefix + message);
	}
	
	/**
	 * Logs a properly formatted message to the console with the severe prefix.
	 * 
	 * @param message The warning message to log.
	 */
	public void severe(String message) {
		logger.severe(consolePrefix + message);
	}
	
	//saves the files
	public void saveFiles() {
		saveConfig();
		configData.saveSlots();
		configData.saveStats();
	}
	
	// Registers economy with Vault
	private Boolean setupEconomy() {		
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }
        return (economy != null); 
    }
}