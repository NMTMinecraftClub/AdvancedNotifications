package com.m0pt0pmatt.advancednotifications.messages;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemorySection;

import com.m0pt0pmatt.advancednotifications.Strings;

/**
 * An account holds a player's messages, mail info, and other info.
 * @author Matthew
 *
 */
public class Account{

	//The username of the player
	private UUID playerName;
	
	//the email address of the player
	private String emailAddress;
	
	//the list of messages in this players inbox
	private List<Message> messages;
	
	//the other players this player has blocked
	private Set<UUID> blockedPlayers;
	
	//a flag which determines is this account has been validated via email
	private boolean validated;
	
	//the activation code for this account.
	private String activationCode;
	
	/**
	 * Creates a new account for a player.
	 * @param playerName
	 * @param realName
	 * @param emailAddress
	 */
	public Account(UUID playerName, String emailAddress){
		this.playerName = playerName;
		this.emailAddress = emailAddress;
		
		messages = new ArrayList<Message>();
		blockedPlayers = new HashSet<UUID>();
		validated = false;
		
		//create a random activation string
		activationCode = generateActivationCode();
	}
	
	/**
	 * Creates a player account, given all details of the account.
	 * This constructor is used to create an account from file.
	 * @param playerName
	 * @param realName
	 * @param emailAddress
	 * @param messages
	 * @param blockedPlayers
	 * @param validated
	 * @param activationCode
	 */
	private Account(UUID playerName, String emailAddress, List<Message> messages, Set<UUID> blockedPlayers, boolean validated, String activationCode){
		this.playerName = playerName;
		this.emailAddress = emailAddress;
		this.messages = messages;
		this.blockedPlayers = blockedPlayers;
		this.validated = validated;
		this.activationCode = activationCode;
	}
	
	//-----------------getters and setters-----------------//
	
	public boolean isValidated() {
		return validated;
	}

	public void setValidated(boolean validated) {
		this.validated = validated;
	}

	public UUID getPlayerName() {
		return playerName;
	}

	public String getEmailAddress() {
		return emailAddress;
	}
	
	public void setEmailAddress(String emailAddress){
		this.emailAddress = emailAddress;
	}

	public List<Message> getMessages() {
		return messages;
	}
	
	public void addMessage(Message message){
		messages.add(message);
	}
	
	public boolean hasMessage(Message message){
		return messages.contains(message);
	}
	
	public void removeMessage(Message message){
		messages.remove(message);
	}

	public Set<UUID> getBlockedPlayers() {
		return blockedPlayers;
	}
	
	public void blockPlayer(UUID playerName){
		blockedPlayers.add(playerName);
	}
	
	public void unblockPlayer(UUID playerName){
		blockedPlayers.remove(playerName);
	}
	
	public String getActivationCode() {
		return activationCode;
	}
	
	public void setActivationCode(String activationCode) {
		this.activationCode = activationCode;
	}
	
	//-----------------end of getters and setters-----------------//
	
	
	/**
	 * Generates a random activation string
	 * @return
	 */
	public static String generateActivationCode(){
		SecureRandom random = new SecureRandom();
		return new BigInteger(30, random).toString(10);
	}
	
	/**
	 * Stores an account in a configuration section.
	 * creates a new ConfigurationSection from the section given.
	 * @param accountsSection
	 * @param account
	 */
	public static void serializeAccount(ConfigurationSection accountsSection, Account account){
		
		//create a new section for the account
		ConfigurationSection accountSection = accountsSection.createSection(account.getPlayerName().toString());
		
		//add simple data to the configuration section
		accountSection.set(Strings.PLAYERNAME.toString(), account.getPlayerName().toString());
		accountSection.set(Strings.EMAILADDRESS.toString(), account.getEmailAddress());
		accountSection.set(Strings.BLOCKEDPLAYERS.toString(), account.getBlockedPlayers().toArray());
		accountSection.set(Strings.VALIDATED.toString(), account.isValidated());
		accountSection.set(Strings.ACTIVATIONCODE.toString(), account.getActivationCode());
		
		//add the messages to the configuration section
		ConfigurationSection messagesSection = accountSection.createSection(Strings.MESSAGES.toString());
		int i = 0;
		for (Message message: account.getMessages()){
			Message.serializeMessage(messagesSection, message, String.valueOf(i));
			i++;
		}
		
	}
	
	/**
	 * Creates an account from a configuration section
	 * @param accountSection
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Account unserializeAccount(ConfigurationSection accountSection){
		
		//create empty attributes
		UUID playerName = null;
		String emailAddress = "";
		ArrayList<Message> messages = new ArrayList<Message>();
		Set<UUID> blockedPlayers = new HashSet<UUID>();
		boolean validated = false;
		String activationCode = "none";
		
		//grab the playerName
		playerName = UUID.fromString(accountSection.getName());
		
		//grab the email address
		if (accountSection.contains(Strings.EMAILADDRESS.toString())){
			emailAddress = (String) accountSection.get(Strings.EMAILADDRESS.toString());
		}
		
		//grab the messages
		if (accountSection.contains(Strings.MESSAGES.toString())){
			MemorySection messagesSection = (MemorySection) accountSection.get(Strings.MESSAGES.toString());
			for (String location: messagesSection.getKeys(false)){
				Message message = Message.unserializeMessage(messagesSection.getConfigurationSection(location));
				messages.add(message);
			}
		}
		
		//grab the blocked players
		if (accountSection.contains(Strings.BLOCKEDPLAYERS.toString())){
			List<String> uids = (List<String>) accountSection.getList(Strings.BLOCKEDPLAYERS.toString());
			
			for (String id: uids){
				blockedPlayers.add(UUID.fromString(id));
			}
			
		}
		
		//grab the validated flag
		if (accountSection.contains(Strings.VALIDATED.toString())){
			validated = (Boolean) accountSection.get(Strings.VALIDATED.toString());
		}
		
		//grab the activation code
		if (accountSection.contains(Strings.ACTIVATIONCODE.toString())){
			activationCode = (String) accountSection.get(Strings.ACTIVATIONCODE.toString());
		}
		
		//create and return the new account
		return new Account(playerName, emailAddress, messages, blockedPlayers, validated, activationCode);
	}

}
