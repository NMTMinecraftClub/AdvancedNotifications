package com.m0pt0pmatt.advancednotifications;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import com.bukkittoolkit.formatting.FormattedMessage;

public enum Strings implements FormattedMessage {

	ACCOUNTS("accounts"),
	PLAYERNAME("playerName"),
	REALNAME("realName"),
	EMAILADDRESS("emailAddress"),
	MESSAGES("messages"),
	BLOCKEDPLAYERS("blockedPlayers"),
	VALIDATED("validated"),
	ACTIVATIONCODE("activationCode"),
	SENDER("sender"),
	RECEIVER("receiver"),
	STATUS("status"),
	CONTENT("content"),
	TIMESTAMP("timestamp"),
	
	NOTREGISTERED("You have not registered yourself on this server!"),
	OTHERNOTREGISTERED("That player is not registered on the server!"),
	WHYREGISTER("Registering allows you to get email notifications and send/receive messages"),
	REGISTERFORCOMMANDS("You cannot use commands until you register"),
	REGISTERINTERACT("You cannot build until you register"),
	REGISTERENTITY("You cannot interact with entities until you register"),
	HOWTOREGISTER("Register by typing the command: /register [Your Name] [emailAddress]"), 
	
	REGISTER("register"),
	VALIDATE("validate"),
	INBOX("inbox"),
	SENDMSG("sendmsg"),
	BLOCK("block"),
	UNBLOCK("unblock"),
	INVALIDEMAIL("Error: Invalid email address"), 
	REGISTERED("You are now registered, but you still need to validate your email address"), 
	PLAYERBLOCKED("Player blocked"),
	PLAYERUNBLOCKED("Player unblocked"), 
	NOTVALIDATED("You have not validated your email account!"),
	HOWTOVALIDATE("You should have been sent an email with a code. Validate your account by running /validate [code]"),
	CORRECTCODE("Your email is now Validated"),
	INCORRECTCODE("Incorrect code. You are still not validated"),
	UPDATEDINFORMATION("Your information has been updated"),
	CHANGEDEMAIL("Your email has changed. You need to re-validate"),
	ALREADYVALIDATED("You have already validated your account"),
	LISTINGBLOCKEDPLAYERS("You have blocked communications with:"),
	MESSAGESENT("Message sent");
	
	private final String string;
	
	private static final Map<String, Strings> strings = new HashMap<String, Strings>();
	
	static {
        for(Strings s : EnumSet.allOf(Strings.class)){
        	strings.put(s.string, s);
        }  	
	}
	
	private Strings(String string){
		this.string = string;
	}
	
	@Override
	public String toString(){
		return string;
	}
	
	public Strings getEnum(String string){
		return strings.get(string);
	}

	public String getMessage() {
		return string;
	}
}
