package com.m0pt0pmatt.advancednotifications;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

import com.bukkittoolkit.formatting.Formatting;
import com.bukkittoolkit.formatting.MessageFormat;
import com.m0pt0pmatt.advancednotifications.mail.Email;
import com.m0pt0pmatt.advancednotifications.mail.EmailSender;
import com.m0pt0pmatt.advancednotifications.messages.Account;
import com.m0pt0pmatt.advancednotifications.messages.Message;
import com.m0pt0pmatt.advancednotifications.messages.MessageStatus;

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
	protected static Map<UUID, Account> accounts;
	
	//The thread which saves accounts to file every couple of minutes
	private Thread savingThread;
		
	/**
	 * Executed when the plugin is first loaded
	 * Creates internal objects and threads
	 */
	@Override
	public void onLoad(){
		accounts = new HashMap<UUID, Account>();
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
		Formatting.msgSender.addLabel(this.getName(), ChatColor.LIGHT_PURPLE);
		
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
			if (args.length < 1){
				return false;
			}
			
			registerPlayer(((Player)sender).getUniqueId(), args);
			return true;
		}
		
		//Player wants to validate with the server
		if (cmd.getName().equalsIgnoreCase(Strings.VALIDATE.toString())){
			if (args.length < 1){
				return false;
			}
			
			validatePlayer(((Player)sender).getUniqueId(), args[0]);
			return true;
		}
		
		//player wants to view the inbox
		else if (cmd.getName().equalsIgnoreCase(Strings.INBOX.toString())){
			if (args.length == 0){
				openInbox(((Player)sender).getUniqueId());
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
			
			UUID receiver = null;
			for (Player p: Bukkit.getServer().getOnlinePlayers()){
				if (p.getName().equals(args[0])){
					receiver = p.getUniqueId();
				}
			}
			
			sendMessage(((Player)sender).getUniqueId(), receiver, message);
			return true;
		}
		
		//player wants to block another player
		else if (cmd.getName().equalsIgnoreCase(Strings.BLOCK.toString())){
			if (args.length == 1){
				
				UUID receiver = null;
				for (Player p: Bukkit.getServer().getOnlinePlayers()){
					if (p.getName().equals(args[0])){
						receiver = p.getUniqueId();
					}
				}
				
				blockPlayer(((Player)sender).getUniqueId(), receiver);
			}
		}
		
		//player wnats to unblock another player
		else if (cmd.getName().equalsIgnoreCase(Strings.UNBLOCK.toString())){
			if (args.length == 1){
				
				UUID receiver = null;
				for (Player p: Bukkit.getServer().getOnlinePlayers()){
					if (p.getName().equals(args[0])){
						receiver = p.getUniqueId();
					}
				}
				
				unblockPlayer(((Player)sender).getUniqueId(), receiver);
			}
		}
		
		//Player wants to view his/her blocked players
		else if (cmd.getName().equalsIgnoreCase(Strings.BLOCKEDPLAYERS.toString())){
			if (args.length == 0){
				listBlockedPlayers(((Player)sender).getUniqueId());
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
	public void registerPlayer(UUID playerName, String[] args){
		
		Player player = Bukkit.getPlayer(playerName);
		
		//Get the player's email address
		String emailAddress = args[0];
		if (!emailAddress.toUpperCase().matches("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}")){
			Formatting.msgSender.sendMessage(player, MessageFormat.BADARGUMENTS, this, Strings.INVALIDEMAIL);
			return;
		}
		
		if (accounts.containsKey(playerName)){
			Account account = accounts.get(playerName);
			Formatting.msgSender.sendMessage(player, MessageFormat.NOTIFICATION, this, Strings.UPDATEDINFORMATION);
			if (!account.getEmailAddress().equalsIgnoreCase(emailAddress)){
				account.setEmailAddress(emailAddress);
				account.setValidated(false);
				Formatting.msgSender.sendMessage(player, MessageFormat.NOTIFICATION, this, Strings.CHANGEDEMAIL);
				account.setActivationCode(Account.generateActivationCode());
			}
			
			this.emailActivationCode(playerName);
			
			return;
		}
		
		//create a new account
		accounts.put(playerName, new Account(playerName, emailAddress));
		
		//email the activation code
		this.emailActivationCode(playerName);
		
		//notify the player
		Formatting.msgSender.sendMessage(player, MessageFormat.NOTIFICATION, this, Strings.REGISTERED);
		Formatting.msgSender.sendMessage(player, MessageFormat.OUTPUT, this, "Email Address: " + emailAddress);
	}
	
	public void validatePlayer(UUID playerName, String code){
		Player player = Bukkit.getPlayer(playerName);
		Account account = accounts.get(playerName);
		if (account == null){
			Formatting.msgSender.sendMessage(player, MessageFormat.ERROR, this, Strings.NOTREGISTERED);
			return;
		}
		
		if (account.isValidated()){
			Formatting.msgSender.sendMessage(player, MessageFormat.ERROR, this, Strings.ALREADYVALIDATED);
			return;
		}
		
		if (code.equalsIgnoreCase(account.getActivationCode())){
			Formatting.msgSender.sendMessage(player, MessageFormat.DEFAULT, this, Strings.CORRECTCODE);
			account.setValidated(true);
		}
		else{
			Formatting.msgSender.sendMessage(player, MessageFormat.ERROR, this, Strings.INCORRECTCODE);
			account.setValidated(false);
		}
	}
	
	private void sendMessage(UUID senderName, UUID receiverName, String message) {
		Player sender = Bukkit.getPlayer(senderName);
		Account senderAccount = accounts.get(senderName);
		if (senderAccount == null){
			Formatting.msgSender.sendMessage(sender, MessageFormat.ERROR, this, Strings.NOTREGISTERED);
			return;
		}
		else if (!senderAccount.isValidated()){
			Formatting.msgSender.sendMessage(sender, MessageFormat.ERROR, this, Strings.NOTVALIDATED);
			Formatting.msgSender.sendMessage(sender, MessageFormat.DEFAULT, this, Strings.HOWTOVALIDATE);
			return;
		}
		
		Account receiverAccount = accounts.get(receiverName);
		if (receiverAccount == null){
			Formatting.msgSender.sendMessage(sender, MessageFormat.ERROR, this, Strings.OTHERNOTREGISTERED);
			return;
		}
		
		Message m = new Message(senderName, receiverName, message, MessageStatus.SENT);
		receiverAccount.addMessage(m);
		
		Formatting.msgSender.sendMessage(sender, MessageFormat.NOTIFICATION, this, Strings.MESSAGESENT);
		
	}
	
	private void openInbox(UUID playerName) {
		Player player = Bukkit.getPlayer(playerName);
		Account account = accounts.get(playerName);
		if (account == null){
			Formatting.msgSender.sendMessage(player, MessageFormat.ERROR, this, Strings.NOTREGISTERED);
			return;
		}
		else if (!account.isValidated()){
			Formatting.msgSender.sendMessage(player, MessageFormat.ERROR, this, Strings.NOTVALIDATED);
			Formatting.msgSender.sendMessage(player, MessageFormat.DEFAULT, this, Strings.HOWTOVALIDATE);
			return;
		}
		
	}
	
	/**
	 * Block a player
	 * @param playerName
	 * @param toBeBlocked
	 */
	public void blockPlayer(UUID playerName, UUID toBeBlocked){
		Player player = Bukkit.getPlayer(playerName);
		Account account = accounts.get(playerName);
		if (account == null){
			Formatting.msgSender.sendMessage(player, MessageFormat.ERROR, this, Strings.NOTREGISTERED);
			return;
		}
		else if (!account.isValidated()){
			Formatting.msgSender.sendMessage(player, MessageFormat.ERROR, this, Strings.NOTVALIDATED);
			Formatting.msgSender.sendMessage(player, MessageFormat.DEFAULT, this, Strings.HOWTOVALIDATE);
			return;
		}
		
		accounts.get(playerName).blockPlayer(toBeBlocked);
		Formatting.msgSender.sendMessage(player, MessageFormat.DEFAULT, this, Strings.PLAYERBLOCKED);
	}
	
	/**
	 * Unblock a player
	 * @param playerName
	 * @param toBeUnblocked
	 */
	public void unblockPlayer(UUID playerName, UUID toBeUnblocked){
		Player player = Bukkit.getPlayer(playerName);
		Account account = accounts.get(playerName);
		if (account == null){
			Formatting.msgSender.sendMessage(player, MessageFormat.ERROR, this, Strings.NOTREGISTERED);
			return;
		}
		else if (!account.isValidated()){
			Formatting.msgSender.sendMessage(player, MessageFormat.ERROR, this, Strings.NOTVALIDATED);
			Formatting.msgSender.sendMessage(player, MessageFormat.DEFAULT, this, Strings.HOWTOVALIDATE);
			return;
		}
		
		accounts.get(playerName).unblockPlayer(toBeUnblocked);
		
		Formatting.msgSender.sendMessage(player, MessageFormat.DEFAULT, this, Strings.PLAYERUNBLOCKED);
	}
	
	
	private void listBlockedPlayers(UUID playerName) {
		Player player = Bukkit.getPlayer(playerName);
		Account account = accounts.get(playerName);
		if (account == null){
			Formatting.msgSender.sendMessage(player, MessageFormat.ERROR, this, Strings.NOTREGISTERED);
			return;
		}
		else if (!account.isValidated()){
			Formatting.msgSender.sendMessage(player, MessageFormat.ERROR, this, Strings.NOTVALIDATED);
			Formatting.msgSender.sendMessage(player, MessageFormat.DEFAULT, this, Strings.HOWTOVALIDATE);
			return;
		}

		Formatting.msgSender.sendMessage(player, MessageFormat.OUTPUT, this, Strings.LISTINGBLOCKEDPLAYERS);
		for (UUID blockedPlayer: account.getBlockedPlayers()){
			Formatting.msgSender.sendMessage(player, MessageFormat.DEFAULT, "", ChatColor.RED + Bukkit.getPlayer(blockedPlayer).getName());
		}
		
	}
	
	public void emailActivationCode(UUID playerName){
		Player player = Bukkit.getPlayer(playerName);
		Account account = accounts.get(playerName);
		if (account == null){
			Formatting.msgSender.sendMessage(player, MessageFormat.ERROR, this, Strings.NOTREGISTERED);
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
			Formatting.msgSender.sendMessage(player, MessageFormat.ERROR, this, Strings.NOTREGISTERED);
			Formatting.msgSender.sendMessage(player, MessageFormat.DEFAULT, this, Strings.WHYREGISTER);
			Formatting.msgSender.sendMessage(player, MessageFormat.DEFAULT, this, Strings.HOWTOREGISTER);
		}
		else if (!account.isValidated()){
			Formatting.msgSender.sendMessage(player, MessageFormat.ERROR, this, Strings.NOTVALIDATED);
			Formatting.msgSender.sendMessage(player, MessageFormat.ERROR, this, Strings.HOWTOVALIDATE);
		}
	}
	
	@EventHandler
	public void onInteract(PlayerInteractEvent event){
		Player player = event.getPlayer();
		Account account = accounts.get(player.getName());
		
		if (account == null){
			Formatting.msgSender.sendMessage(player, MessageFormat.ERROR, this, Strings.NOTREGISTERED);
			Formatting.msgSender.sendMessage(player, MessageFormat.DEFAULT, this, Strings.REGISTERINTERACT);
			Formatting.msgSender.sendMessage(player, MessageFormat.DEFAULT, this, Strings.HOWTOREGISTER);
			event.setCancelled(true);
			return;
		}
		else if (!account.isValidated()){
			Formatting.msgSender.sendMessage(player, MessageFormat.ERROR, this, Strings.NOTVALIDATED);
			Formatting.msgSender.sendMessage(player, MessageFormat.DEFAULT, this, Strings.HOWTOVALIDATE);
			event.setCancelled(true);
			return;
		}
	}
	
	@EventHandler
	public void onEntityInteract(PlayerInteractEntityEvent event){
		Player player = event.getPlayer();
		Account account = accounts.get(player.getName());
		if (account == null){
			Formatting.msgSender.sendMessage(player, MessageFormat.ERROR, this, Strings.NOTREGISTERED);
			Formatting.msgSender.sendMessage(player, MessageFormat.DEFAULT, this, Strings.REGISTERENTITY);
			Formatting.msgSender.sendMessage(player, MessageFormat.DEFAULT, this, Strings.HOWTOREGISTER);
			event.setCancelled(true);
			return;
		}
		else if (!account.isValidated()){
			Formatting.msgSender.sendMessage(player, MessageFormat.ERROR, this, Strings.NOTVALIDATED);
			Formatting.msgSender.sendMessage(player, MessageFormat.DEFAULT, this, Strings.HOWTOVALIDATE);
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
			Formatting.msgSender.sendMessage(player, MessageFormat.ERROR, this, Strings.NOTREGISTERED);
			Formatting.msgSender.sendMessage(player, MessageFormat.DEFAULT, this, Strings.REGISTERENTITY);
			Formatting.msgSender.sendMessage(player, MessageFormat.DEFAULT, this, Strings.HOWTOREGISTER);
			event.setCancelled(true);
			return;
		}
		else if (!account.isValidated()){
			Formatting.msgSender.sendMessage(player, MessageFormat.ERROR, this, Strings.NOTVALIDATED);
			Formatting.msgSender.sendMessage(player, MessageFormat.DEFAULT, this, Strings.HOWTOVALIDATE);
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
			Formatting.msgSender.sendMessage(player, MessageFormat.ERROR, this, Strings.NOTREGISTERED);
			Formatting.msgSender.sendMessage(player, MessageFormat.DEFAULT, this, Strings.REGISTERFORCOMMANDS);
			Formatting.msgSender.sendMessage(player, MessageFormat.DEFAULT, this, Strings.HOWTOREGISTER);
			event.setCancelled(true);
			return;
		}
		else if (!account.isValidated()){
			Formatting.msgSender.sendMessage(player, MessageFormat.ERROR, this, Strings.NOTVALIDATED);
			Formatting.msgSender.sendMessage(player, MessageFormat.DEFAULT, this, Strings.HOWTOVALIDATE);
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
