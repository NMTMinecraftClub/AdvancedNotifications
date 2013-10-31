package com.m0pt0pmatt.advancednotifications;

import com.m0pt0pmatt.advancednotifications.mail.Email;
import com.m0pt0pmatt.advancednotifications.messages.Account;
import com.m0pt0pmatt.advancednotifications.messages.Message;
import com.m0pt0pmatt.advancednotifications.messages.MessageStatus;

/**
 * The message sender allows other plugins to send messages back and forth.
 * It also allows other plugins to send emails
 * @author Matthew
 *
 */
public class MessageServer {

	public void sendMessage(Message message){
		
		//check that sender has an account
		if (!AdvancedNotifications.accounts.containsKey(message.getSender())){
			return;
		}
		
		//check that receiver has an account
		if (!AdvancedNotifications.accounts.containsKey(message.getReceiver())){
			return;
		}
		
		AdvancedNotifications.accounts.get(message.getReceiver()).addMessage(message);
		
		message.setStatus(MessageStatus.SENT);
	}
	
	public void sendEmail(Message message){
		//check that sender has an account
		if (!AdvancedNotifications.accounts.containsKey(message.getSender())){
			return;
		}
		
		//check that receiver has an account
		if (!AdvancedNotifications.accounts.containsKey(message.getReceiver())){
			return;
		}
		
		Account senderAccount = AdvancedNotifications.accounts.get(message.getSender());
		Account receiverAccount = AdvancedNotifications.accounts.get(message.getReceiver());
		
		
		//check that sender has a validated email
		if (!senderAccount.isValidated()){
			return;
		}
		
		//check that receiver has a validated email
		if (!receiverAccount.isValidated()){
			return;
		}
		
		Email email = new Email(senderAccount.getEmailAddress(), 
				receiverAccount.getEmailAddress(),
				"NMT Minecraft Server: Message from " + senderAccount.getPlayerName(),
				"Server delivers the following message:\n" + message.getContent());
		
		AdvancedNotifications.emailSender.sendEmail(email);

	}
	
	public void sendEmail(Email email){
		AdvancedNotifications.emailSender.sendEmail(email);
	}
	
}
