package com.m0pt0pmatt.advancednotifications;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.m0pt0pmatt.advancednotifications.mail.Email;
import com.m0pt0pmatt.advancednotifications.mail.EmailSender;
import com.m0pt0pmatt.advancednotifications.messages.Account;
import com.m0pt0pmatt.menuservice.api.MenuComponent;
import com.m0pt0pmatt.menuservice.api.MenuService;

/**
 * This plugin allows an admin to give notifications to users
 * There are many features planned for this plugin.
 * @author Matthew
 */
public class AdvancedNotifications extends JavaPlugin implements Listener{
	
	private YamlConfiguration config;
	private File configFile;
	private static final String configFileName = "config.yml";
	
	//private static MenuService menuService;
	
	private static EmailSender emailSender;
	
	private static Map<String, Account> accounts;
	
	/**
	 * Executed when the plugin is enabled.
	 * Sets up internal objects and loads settings
	 */
	public void onEnable(){
		
		accounts = new HashMap<String, Account>();
		
		//load config file
		try {
			if (!this.getDataFolder().exists()){
				this.getDataFolder().mkdir();
			}
			configFile = new File(getDataFolder(), configFileName);
			
			if (!configFile.exists()){
				configFile.createNewFile();
			}
			
			config = YamlConfiguration.loadConfiguration(configFile);
			
			loadAccounts();
			
			emailSender = new EmailSender(this);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//get MenuService
		//if (Bukkit.getServicesManager().isProvidedFor(MenuService.class)){
		//	menuService = Bukkit.getServicesManager().getRegistration(MenuService.class).getProvider();
		//} else{
		//	menuService = null;
		//}
		
		createMenus();
		
		//register as listener
		Bukkit.getPluginManager().registerEvents(this, this);
		
		
		//quick fix to keep "plugin.yml" in the jar
		YamlConfiguration pluginConfig = YamlConfiguration.loadConfiguration(new File("plugin.yml"));
		
	}
	
	private void createMenus() {
		//MenuComponent menu = new MenuComponent();
		//menu.setType("menu");
	}

	/**
	 * Executed when the plugin is being disabled
	 * Saves settings to file
	 */
	public void onDisable(){
		saveAccounts();
	}
	
	/**
	 * Executed when the plugin is being reloaded
	 * Saves and then loads settings
	 */
	public void onReload(){
		saveAccounts();
		loadAccounts();
	}
	
	/**
	 * Commands for this plugin are executed here
	 */
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		
		//Player wants to register with the server
		if (cmd.getName().equalsIgnoreCase(Strings.REGISTER.toString())){
			if (args.length < 2){
				return false;
			}
			
			registerPlayer(sender.getName(), args);
			return true;
		}
		
		//Player wants to validate with the server
		if (cmd.getName().equalsIgnoreCase(Strings.VALIDATE.toString())){
			if (args.length < 1){
				return false;
			}
			
			validatePlayer(sender.getName(), args[0]);
			return true;
		}
		
		//player wants to view the inbox
		else if (cmd.getName().equalsIgnoreCase(Strings.INBOX.toString())){
			if (args.length == 0){
				
			}
			
			return false;
		}
		
		//player wants to send a message
		else if (cmd.getName().equalsIgnoreCase(Strings.MSG.toString())){
			if (args.length < 2){
				return false;
			}
			
			return true;
		}
		
		//player wants to block another player
		else if (cmd.getName().equalsIgnoreCase(Strings.BLOCK.toString())){
			if (args.length == 1){
				blockPlayer(sender.getName(), args[0]);
			}
		}
		
		//player wnats to unblock another player
		else if (cmd.getName().equalsIgnoreCase(Strings.UNBLOCK.toString())){
			if (args.length == 1){
				unblockPlayer(sender.getName(), args[0]);
			}
		}
		
		return false;
	}
	
	/**
	 * Loads account information from file
	 */
	private void loadAccounts(){
		
		accounts.clear();
		
		if (!config.contains(Strings.ACCOUNTS.toString())){
			return;
		}
		
		MemorySection accountsSection = (MemorySection) config.get(Strings.ACCOUNTS.toString());
		for (String playerName: accountsSection.getKeys(false)){
			Account account = Account.unserializeAccount(accountsSection.getConfigurationSection(playerName));
			accounts.put(account.getPlayerName(), account);
		}
	}
	
	/**
	 * Saves account information to file
	 */
	public void saveAccounts(){
		ConfigurationSection accountsSection = null;
		if (config.contains(Strings.ACCOUNTS.toString())) {
			accountsSection = config.getConfigurationSection(Strings.ACCOUNTS.toString());
		}
		else{
			accountsSection = config.createSection(Strings.ACCOUNTS.toString());
		}
		
		for (Account account: accounts.values()){
			Account.serializeAccount(accountsSection, account);
		}
		
		try {
			config.save(configFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Registers a player
	 * @param playerName
	 * @param args
	 */
	public void registerPlayer(String playerName, String[] args){
		
		Player player = Bukkit.getPlayer(playerName);
		
		//Get the player's email address
		String emailAddress = args[args.length - 1];
		if (!emailAddress.toUpperCase().matches("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}")){
			player.sendMessage(Strings.INVALIDEMAIL.toString());
			return;
		}
		
		//Get the player's real name
		String realName = args[0];
		for (int i = 1; i < args.length - 1; i++){
			realName = realName + " " + args[i];
		}
		
		if (accounts.containsKey(playerName)){
			Account account = accounts.get(playerName);
			account.setRealName(realName);
			player.sendMessage(Strings.UPDATEDINFORMATION.toString());
			if (!account.getEmailAddress().equalsIgnoreCase(emailAddress)){
				account.setEmailAddress(emailAddress);
				account.setValidated(false);
				player.sendMessage(Strings.CHANGEDEMAIL.toString());
				account.setActivationCode(Account.generateActivationCode());
			}
			
			this.emailActivationCode(playerName);
			
			return;
		}
		
		//create a new account
		accounts.put(playerName, new Account(playerName, realName, emailAddress));
		
		//email the activation code
		this.emailActivationCode(playerName);
		
		//notify the player
		player.sendMessage(Strings.REGISTERED.toString());
		player.sendMessage("Full Name: " + realName);
		player.sendMessage("Email Address: " + emailAddress);
	}
	
	public void validatePlayer(String playerName, String code){
		Player player = Bukkit.getPlayer(playerName);
		Account account = accounts.get(playerName);
		if (account == null){
			player.sendMessage(Strings.NOTREGISTERED.toString());
			return;
		}
		
		if (account.isValidated()){
			player.sendMessage(Strings.ALREADYVALIDATED.toString());
			return;
		}
		
		if (code.equalsIgnoreCase(account.getActivationCode())){
			player.sendMessage(Strings.CORRECTCODE.toString());
			account.setValidated(true);
		}
		else{
			player.sendMessage(Strings.INCORRECTCODE.toString());
			account.setValidated(false);
		}
	}
	
	/**
	 * Block a player
	 * @param playerName
	 * @param toBeBlocked
	 */
	public void blockPlayer(String playerName, String toBeBlocked){
		Player player = Bukkit.getPlayer(playerName);
		Account account = accounts.get(playerName);
		if (account == null){
			player.sendMessage(Strings.NOTREGISTERED.toString());
			return;
		}
		else if (!account.isValidated()){
			player.sendMessage(Strings.NOTVALIDATED.toString());
			player.sendMessage(Strings.HOWTOVALIDATE.toString());
			return;
		}
		
		accounts.get(playerName).blockPlayer(toBeBlocked);
		player.sendMessage(Strings.PLAYERBLOCKED.toString());
	}
	
	/**
	 * Unblock a player
	 * @param playerName
	 * @param toBeUnblocked
	 */
	public void unblockPlayer(String playerName, String toBeUnblocked){
		Player player = Bukkit.getPlayer(playerName);
		Account account = accounts.get(playerName);
		if (account == null){
			player.sendMessage(Strings.NOTREGISTERED.toString());
			return;
		}
		else if (!account.isValidated()){
			player.sendMessage(Strings.NOTVALIDATED.toString());
			player.sendMessage(Strings.HOWTOVALIDATE.toString());
			return;
		}
		
		accounts.get(playerName).unblockPlayer(toBeUnblocked);
		player.sendMessage(Strings.PLAYERUNBLOCKED.toString());
	}
	
	public void emailActivationCode(String playerName){
		Player player = Bukkit.getPlayer(playerName);
		Account account = accounts.get(playerName);
		if (account == null){
			player.sendMessage(Strings.NOTREGISTERED.toString());
			return;
		}
		
		//email the code
		String message = "Your activation code is: " + account.getActivationCode() + "\nActivate your account by running the command /validate [code]";
		Email email = new Email("activate@nmtminecraftclub.com", account.getEmailAddress(), "Activate your account!", message);
		emailSender.sendEmail(email);
	}
	
	/**
	 * Check that player is registered when joing the server
	 * @param event
	 */
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event){
		
		//check if player is registered
		Player player = event.getPlayer();
		Account account = accounts.get(player.getName());
		
		if (account == null){
			player.sendMessage(Strings.NOTREGISTERED.toString());
			player.sendMessage(Strings.WHYREGISTER.toString());
			player.sendMessage(Strings.HOWTOREGISTER.toString());
		}
		else if (!account.isValidated()){
			player.sendMessage(Strings.NOTVALIDATED.toString());
			player.sendMessage(Strings.HOWTOVALIDATE.toString());
		}
	}
	
	@EventHandler
	public void onInteract(PlayerInteractEvent event){
		Player player = event.getPlayer();
		Account account = accounts.get(player.getName());
		
		if (account == null){
			player.sendMessage(Strings.NOTREGISTERED.toString());
			player.sendMessage(Strings.REGISTERINTERACT.toString());
			player.sendMessage(Strings.HOWTOREGISTER.toString());
			event.setCancelled(true);
			return;
		}
		else if (!account.isValidated()){
			player.sendMessage(Strings.NOTVALIDATED.toString());
			player.sendMessage(Strings.HOWTOVALIDATE.toString());
			event.setCancelled(true);
			return;
		}
	}
	
	@EventHandler
	public void onEntityInteract(PlayerInteractEntityEvent event){
		Player player = event.getPlayer();
		Account account = accounts.get(player.getName());
		if (account == null){
			player.sendMessage(Strings.NOTREGISTERED.toString());
			player.sendMessage(Strings.REGISTERENTITY.toString());
			player.sendMessage(Strings.HOWTOREGISTER.toString());
			event.setCancelled(true);
			return;
		}
		else if (!account.isValidated()){
			player.sendMessage(Strings.NOTVALIDATED.toString());
			player.sendMessage(Strings.HOWTOVALIDATE.toString());
			event.setCancelled(true);
			return;
		}
	}
	
	@EventHandler
	public void onEntityDamage(EntityDamageByEntityEvent event){	
		Entity entity = event.getDamager();
		if (!(entity instanceof Player)){
			return;
		}
		Player player = (Player) entity;
		Account account = accounts.get(player.getName());
		if (account == null){
			player.sendMessage(Strings.NOTREGISTERED.toString());
			player.sendMessage(Strings.REGISTERENTITY.toString());
			player.sendMessage(Strings.HOWTOREGISTER.toString());
			event.setCancelled(true);
			return;
		}
		else if (!account.isValidated()){
			player.sendMessage(Strings.NOTVALIDATED.toString());
			player.sendMessage(Strings.HOWTOVALIDATE.toString());
			event.setCancelled(true);
			return;
		}
	}
	
	/**
	 * Make sure a player is registered before the send a command
	 * @param event
	 */
	@EventHandler
	public void onCommand(PlayerCommandPreprocessEvent event){
		if (event.getMessage().startsWith("/register ") || event.getMessage().startsWith("/validate ")){
			return;
		}
		
		Player player = event.getPlayer();
		Account account = accounts.get(player.getName());
		if (account == null){
			player.sendMessage(Strings.NOTREGISTERED.toString());
			player.sendMessage(Strings.REGISTERFORCOMMANDS.toString());
			player.sendMessage(Strings.HOWTOREGISTER.toString());
			event.setCancelled(true);
			return;
		} 
		else if (!account.isValidated()){
			player.sendMessage(Strings.NOTVALIDATED.toString());
			player.sendMessage(Strings.HOWTOVALIDATE.toString());
			event.setCancelled(true);
			return;
		}
	}
	
	
	/**
	 * Filter blocked players
	 * @param event
	 */
	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent event){
		
		for (Account account: accounts.values()){
			if (account.getBlockedPlayers().contains(event.getPlayer().getName())){
				event.getRecipients().remove(account.getPlayerName());
			}
		}
		
	}
	
	public static void main(String[] args){
	}
	
}
