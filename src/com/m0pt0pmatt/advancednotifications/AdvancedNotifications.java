package com.m0pt0pmatt.advancednotifications;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import com.m0pt0pmatt.advancednotifications.messages.Message;
import com.m0pt0pmatt.advancednotifications.messages.MessageStatus;
import com.m0pt0pmatt.menuservice.api.AbstractComponent;
import com.m0pt0pmatt.menuservice.api.Component;
import com.m0pt0pmatt.menuservice.api.ComponentType;
import com.m0pt0pmatt.menuservice.api.Menu;
import com.m0pt0pmatt.menuservice.api.MenuPart;
import com.m0pt0pmatt.menuservice.api.MenuService;
import com.m0pt0pmatt.menuservice.api.rendering.Renderer;

/**
 * This plugin allows an admin to give notifications to users
 * There are many features planned for this plugin.
 * @author Matthew
 */
public class AdvancedNotifications extends JavaPlugin implements Listener{
	
	//Configuration File filename
	private static final String configFileName = "config.yml";
	
	//Configuration File
	private File configFile;
	
	//Yaml configuration file
	private YamlConfiguration config;
	
	//Sends emails	
	protected static EmailSender emailSender;
	
	//Map of usernames to accounts
	protected static Map<String, Account> accounts;
	
	//The thread which saves accounts to file every couple of minutes
	private Thread savingThread;
	
	public static MenuService menuService;
	
	/**
	 * Executed when the plugin is first loaded
	 * Creates internal objects and threads
	 */
	@Override
	public void onLoad(){
		accounts = new HashMap<String, Account>();
		savingThread = new Thread(){
			public void run(){
				try {
					//wait 15 minutes
					Thread.sleep(1000 * 60 * 15);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				//save accounts if enabled
				if (isEnabled()){
					saveAccounts();
				}
			}
		};
		emailSender = new EmailSender(this);
	}
	
	/**
	 * Executed when the plugin is enabled.
	 * Sets up internal objects, loads settings, and starts threads.
	 * Also does:
	 * 1) Adds a BukkitUtil label for the plugin
	 * 2) Loads configuration files
	 * 3) Create and Register Menus
	 * 4) Register Listeners to Bukkit
	 * 5) Start threads
	 */
	@Override
	public void onEnable(){	
		
		//1) Adds a BukkitUtil label for the plugin
		//BukkitUtil.msgSender.addLabel(this.getName(), ChatColor.LIGHT_PURPLE);
		
		//2) Loads configuration files
		try {
			
			//make sure data folder exists
			if (!this.getDataFolder().exists()){
				this.getDataFolder().mkdir();
			}
			
			//make sure config file exists
			configFile = new File(getDataFolder(), configFileName);
			if (!configFile.exists()){
				configFile.createNewFile();
			}
			config = YamlConfiguration.loadConfiguration(configFile);
			
			//load account from configuration file
			loadAccounts();
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//3) Create and Register Menus
		//get MenuService
		if (Bukkit.getServicesManager().isProvidedFor(MenuService.class)){
			menuService = Bukkit.getServicesManager().getRegistration(MenuService.class).getProvider();
		} else{
			menuService = null;
		}
		
		//4) Register Listeners to Bukkit
		Bukkit.getPluginManager().registerEvents(this, this);
		
		//5) Start threads
		savingThread.start();
		emailSender.start();
	}

	/**
	 * Executed when the plugin is being disabled
	 * Saves settings to file
	 */
	public void onDisable(){
		saveAccounts();
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
				openInbox(sender.getName());
			}
			
			return false;
		}
		
		//player wants to send a message
		else if (cmd.getName().equalsIgnoreCase(Strings.SENDMSG.toString())){
			if (args.length < 2){
				return false;
			}
			
			String message = "";
			for (int i = 1; i < args.length; i++) message = message.concat(args[i]);
			sendMessage(sender.getName(), args[0], message);
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
		
		//Player wants to view his/her blocked players
		else if (cmd.getName().equalsIgnoreCase(Strings.BLOCKEDPLAYERS.toString())){
			if (args.length == 0){
				listBlockedPlayers(sender.getName());
			}
		}
		
		return false;
	}

	/**
	 * Loads account information from file
	 */
	private void loadAccounts(){
		
		//remove current accounts
		accounts.clear();
		
		//check if the config file has an accounts section
		if (!config.contains(Strings.ACCOUNTS.toString())){
			return;
		}
		
		//for each account
		MemorySection accountsSection = (MemorySection) config.get(Strings.ACCOUNTS.toString());
		for (String playerName: accountsSection.getKeys(false)){
			
			//create an account from the data in the config file
			Account account = Account.unserializeAccount(accountsSection.getConfigurationSection(playerName));
			
			//add the account
			accounts.put(account.getPlayerName(), account);
		}
	}
	
	/**
	 * Saves account information to file
	 */
	public void saveAccounts(){
		
		ConfigurationSection accountsSection = null;
		
		//get the section of the config which holds the account, if it exists
		if (config.contains(Strings.ACCOUNTS.toString())) {
			accountsSection = config.getConfigurationSection(Strings.ACCOUNTS.toString());
		}
		
		//otherwise create a new section
		else{
			accountsSection = config.createSection(Strings.ACCOUNTS.toString());
		}
		
		//add each account to the accountsSection
		for (Account account: accounts.values()){
			Account.serializeAccount(accountsSection, account);
		}
		
		//save the config to file
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
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.BADARGUMENTS, this, Strings.INVALIDEMAIL);
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
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.NOTIFICATION, this, Strings.UPDATEDINFORMATION);
			if (!account.getEmailAddress().equalsIgnoreCase(emailAddress)){
				account.setEmailAddress(emailAddress);
				account.setValidated(false);
				//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.NOTIFICATION, this, Strings.CHANGEDEMAIL);
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
		//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.NOTIFICATION, this, Strings.REGISTERED);
		//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.OUTPUT, this, "Full Name: " + realName);
		//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.OUTPUT, this, "Email Address: " + emailAddress);
	}
	
	public void validatePlayer(String playerName, String code){
		Player player = Bukkit.getPlayer(playerName);
		Account account = accounts.get(playerName);
		if (account == null){
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.ERROR, this, Strings.NOTREGISTERED);
			return;
		}
		
		if (account.isValidated()){
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.ERROR, this, Strings.ALREADYVALIDATED);
			return;
		}
		
		if (code.equalsIgnoreCase(account.getActivationCode())){
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.DEFAULT, this, Strings.CORRECTCODE);
			account.setValidated(true);
		}
		else{
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.ERROR, this, Strings.INCORRECTCODE);
			account.setValidated(false);
		}
	}
	
	private void sendMessage(String senderName, String receiverName, String message) {
		Player sender = Bukkit.getPlayer(senderName);
		Account senderAccount = accounts.get(senderName);
		if (senderAccount == null){
			//BukkitUtil.msgSender.sendMessage(sender, UtilMessageFormat.ERROR, this, Strings.NOTREGISTERED);
			return;
		}
		else if (!senderAccount.isValidated()){
			//BukkitUtil.msgSender.sendMessage(sender, UtilMessageFormat.ERROR, this, Strings.NOTVALIDATED);
			//BukkitUtil.msgSender.sendMessage(sender, UtilMessageFormat.DEFAULT, this, Strings.HOWTOVALIDATE);
			return;
		}
		
		Player receiver = Bukkit.getPlayer(receiverName);
		Account receiverAccount = accounts.get(receiverName);
		if (receiverAccount == null){
			//BukkitUtil.msgSender.sendMessage(sender, UtilMessageFormat.ERROR, this, Strings.OTHERNOTREGISTERED);
			return;
		}
		
		Message m = new Message(senderName, receiverName, message, MessageStatus.SENT);
		receiverAccount.addMessage(m);
		
		//BukkitUtil.msgSender.sendMessage(sender, UtilMessageFormat.NOTIFICATION, this, Strings.MESSAGESENT);
		
	}
	
	private void openInbox(String playerName) {
		Player player = Bukkit.getPlayer(playerName);
		Account account = accounts.get(playerName);
		if (account == null){
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.ERROR, this, Strings.NOTREGISTERED);
			return;
		}
		else if (!account.isValidated()){
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.ERROR, this, Strings.NOTVALIDATED);
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.DEFAULT, this, Strings.HOWTOVALIDATE);
			return;
		}
		
		Menu menu = new Menu();
		menu.setSize(6);
		List<Component> components = new LinkedList<Component>();
		for (Message m: account.getMessages()){
			components.add(m.toComponent());
		}
		menu.addPart(new MenuPart("messages", components));
		
		Renderer r = menuService.getRenderer("inventory");
		r.openMenu(menu, playerName);
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
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.ERROR, this, Strings.NOTREGISTERED);
			return;
		}
		else if (!account.isValidated()){
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.ERROR, this, Strings.NOTVALIDATED);
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.DEFAULT, this, Strings.HOWTOVALIDATE);
			return;
		}
		
		accounts.get(playerName).blockPlayer(toBeBlocked);
		//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.DEFAULT, this, Strings.PLAYERBLOCKED);
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
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.ERROR, this, Strings.NOTREGISTERED);
			return;
		}
		else if (!account.isValidated()){
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.ERROR, this, Strings.NOTVALIDATED);
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.DEFAULT, this, Strings.HOWTOVALIDATE);
			return;
		}
		
		accounts.get(playerName).unblockPlayer(toBeUnblocked);
		//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.DEFAULT, this, Strings.PLAYERUNBLOCKED);
	}
	
	
	private void listBlockedPlayers(String playerName) {
		Player player = Bukkit.getPlayer(playerName);
		Account account = accounts.get(playerName);
		if (account == null){
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.ERROR, this, Strings.NOTREGISTERED);
			return;
		}
		else if (!account.isValidated()){
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.ERROR, this, Strings.NOTVALIDATED);
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.DEFAULT, this, Strings.HOWTOVALIDATE);
			return;
		}

		//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.OUTPUT, this, Strings.LISTINGBLOCKEDPLAYERS);
		for (String blockedPlayer: account.getBlockedPlayers()){
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.DEFAULT, "", ChatColor.RED + blockedPlayer);
		}
		
	}
	
	public void emailActivationCode(String playerName){
		Player player = Bukkit.getPlayer(playerName);
		Account account = accounts.get(playerName);
		if (account == null){
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.ERROR, this, Strings.NOTREGISTERED);
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
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.ERROR, this, Strings.NOTREGISTERED);
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.DEFAULT, this, Strings.WHYREGISTER);
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.DEFAULT, this, Strings.HOWTOREGISTER);
		}
		else if (!account.isValidated()){
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.ERROR, this, Strings.NOTVALIDATED);
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.ERROR, this, Strings.HOWTOVALIDATE);
		}
	}
	
	@EventHandler
	public void onInteract(PlayerInteractEvent event){
		Player player = event.getPlayer();
		Account account = accounts.get(player.getName());
		
		if (account == null){
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.ERROR, this, Strings.NOTREGISTERED);
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.DEFAULT, this, Strings.REGISTERINTERACT);
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.DEFAULT, this, Strings.HOWTOREGISTER);
			event.setCancelled(true);
			return;
		}
		else if (!account.isValidated()){
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.ERROR, this, Strings.NOTVALIDATED);
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.DEFAULT, this, Strings.HOWTOVALIDATE);
			event.setCancelled(true);
			return;
		}
	}
	
	@EventHandler
	public void onEntityInteract(PlayerInteractEntityEvent event){
		Player player = event.getPlayer();
		Account account = accounts.get(player.getName());
		if (account == null){
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.ERROR, this, Strings.NOTREGISTERED);
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.DEFAULT, this, Strings.REGISTERENTITY);
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.DEFAULT, this, Strings.HOWTOREGISTER);
			event.setCancelled(true);
			return;
		}
		else if (!account.isValidated()){
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.ERROR, this, Strings.NOTVALIDATED);
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.DEFAULT, this, Strings.HOWTOVALIDATE);
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
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.ERROR, this, Strings.NOTREGISTERED);
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.DEFAULT, this, Strings.REGISTERENTITY);
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.DEFAULT, this, Strings.HOWTOREGISTER);
			event.setCancelled(true);
			return;
		}
		else if (!account.isValidated()){
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.ERROR, this, Strings.NOTVALIDATED);
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.DEFAULT, this, Strings.HOWTOVALIDATE);
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
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.ERROR, this, Strings.NOTREGISTERED);
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.DEFAULT, this, Strings.REGISTERFORCOMMANDS);
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.DEFAULT, this, Strings.HOWTOREGISTER);
			event.setCancelled(true);
			return;
		}
		else if (!account.isValidated()){
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.ERROR, this, Strings.NOTVALIDATED);
			//BukkitUtil.msgSender.sendMessage(player, UtilMessageFormat.DEFAULT, this, Strings.HOWTOVALIDATE);
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
