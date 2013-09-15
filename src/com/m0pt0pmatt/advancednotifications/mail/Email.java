package com.m0pt0pmatt.advancednotifications.mail;

/**
 * Represents an email.
 * This class will be used to send emails to the members of the server
 * @author Matthew
 *
 */
public class Email {

	private String sender, receiver, subject, content;
	
	public Email(){
		this.sender = "";
		this.receiver = "";
		this.subject = "";
		this.content = "";
	}
	
	public Email(String sender, String receiver, String subject, String content){
		this.sender = sender;
		this.receiver = receiver;
		this.subject = subject;
		this.content = content;
	}
	
	public void setSender(String sender){
		this.sender = sender;
	}
	
	public void setReceiver(String receiver){
		this.receiver = receiver;
	}
	
	public void setTitle(String subject){
		this.subject = subject;
	}
	
	public void setContent(String content){
		this.content = content;
	}
	
	public String getSender(){
		return sender;
	}
	
	public String getReceiver(){
		return receiver;
	}
	
	public String getTitle(){
		return subject;
	}
	
	public String getContent(){
		return content;
	}
	
	public String getSubject(){
		return subject;
	}

}
