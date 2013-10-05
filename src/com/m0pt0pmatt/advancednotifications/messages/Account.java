package com.m0pt0pmatt.advancednotifications.messages;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemorySection;

import com.m0pt0pmatt.advancednotifications.Strings;

/**
 * An account holds a player's messages, mail info, and other info.
 * @author Matthew
 *
 */
public class Account{

	private String playerName;
	private String realName;
	private String emailAddress;
	private List<Message> messages;
	private Set<String> blockedPlayers;
	private boolean validated;
	private String activationCode;
	
	public Account(String playerName, String realName, String emailAddress){
		this.playerName = playerName;
		this.realName = realName;
		this.emailAddress = emailAddress;
		
		messages = new ArrayList<Message>();
		blockedPlayers = new HashSet<String>();
		validated = false;
		activationCode = generateActivationCode();
	}
	
	private Account(String playerName, String realName, String emailAddress, List<Message> messages, Set<String> blockedPlayers, boolean validated, String activationCode){
		this.playerName = playerName;
		this.realName = realName;
		this.emailAddress = emailAddress;
		this.messages = messages;
		this.blockedPlayers = blockedPlayers;
		this.validated = validated;
		this.activationCode = activationCode;
	}
	
	public boolean isValidated() {
		return validated;
	}

	public void setValidated(boolean validated) {
		this.validated = validated;
	}

	public String getPlayerName() {
		return playerName;
	}

	public String getRealName() {
		return realName;
	}
	
	public void setRealName(String realName){
		this.realName = realName;
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

	public Set<String> getBlockedPlayers() {
		return blockedPlayers;
	}
	
	public void blockPlayer(String playerName){
		blockedPlayers.add(playerName);
	}
	
	public void unblockPlayer(String playerName){
		blockedPlayers.remove(playerName);
	}
	
	public static String generateActivationCode(){
		SecureRandom random = new SecureRandom();
		return new BigInteger(30, random).toString(10);
	}
	
	public static void serializeAccount(ConfigurationSection accountsSection, Account account){
		
		ConfigurationSection accountSection = accountsSection.createSection(account.getPlayerName());
		
		accountSection.set(Strings.PLAYERNAME.toString(), account.getPlayerName());
		accountSection.set(Strings.REALNAME.toString(), account.getRealName());
		accountSection.set(Strings.EMAILADDRESS.toString(), account.getEmailAddress());
		accountSection.set(Strings.BLOCKEDPLAYERS.toString(), account.getBlockedPlayers().toArray());
		accountSection.set(Strings.VALIDATED.toString(), account.isValidated());
		accountSection.set(Strings.ACTIVATIONCODE.toString(), account.getActivationCode());
		
		ConfigurationSection messagesSection = accountSection.createSection(Strings.MESSAGES.toString());
		int i = 0;
		for (Message message: account.getMessages()){
			Message.serializeMessage(messagesSection, message, String.valueOf(i));
			i++;
		}
		
	}
	
	public String getActivationCode() {
		return activationCode;
	}

	@SuppressWarnings("unchecked")
	public static Account unserializeAccount(ConfigurationSection accountSection){
		String playerName = "";
		String realName = "";
		String emailAddress = "";
		ArrayList<Message> messages = new ArrayList<Message>();
		Set<String> blockedPlayers = new HashSet<String>();
		boolean validated = false;
		String activationCode = "none";
		
		playerName = accountSection.getName();
		
		if (accountSection.contains(Strings.REALNAME.toString())){
			realName = (String) accountSection.get(Strings.REALNAME.toString());
		}
		
		if (accountSection.contains(Strings.EMAILADDRESS.toString())){
			emailAddress = (String) accountSection.get(Strings.EMAILADDRESS.toString());
		}
		
		if (accountSection.contains(Strings.MESSAGES.toString())){
			MemorySection messagesSection = (MemorySection) accountSection.get(Strings.MESSAGES.toString());
			for (String location: messagesSection.getKeys(false)){
				Message message = Message.unserializeMessage(messagesSection.getConfigurationSection(location));
				messages.add(message);
			}
		}
		
		if (accountSection.contains(Strings.BLOCKEDPLAYERS.toString())){
			blockedPlayers.addAll((List<String>) accountSection.getList(Strings.BLOCKEDPLAYERS.toString()));
		}
		
		if (accountSection.contains(Strings.VALIDATED.toString())){
			validated = (Boolean) accountSection.get(Strings.VALIDATED.toString());
		}
		
		if (accountSection.contains(Strings.ACTIVATIONCODE.toString())){
			activationCode = (String) accountSection.get(Strings.ACTIVATIONCODE.toString());
		}
		
		return new Account(playerName, realName, emailAddress, messages, blockedPlayers, validated, activationCode);
	}

	public void setActivationCode(String activationCode) {
		this.activationCode = activationCode;
	}
}
