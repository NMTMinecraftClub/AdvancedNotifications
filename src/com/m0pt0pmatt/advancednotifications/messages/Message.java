package com.m0pt0pmatt.advancednotifications.messages;

import java.util.UUID;

import org.bukkit.configuration.ConfigurationSection;

/**
 * A message represents a String of characters from one player to another
 * @author Matthew
 *
 */
public class Message{

	private UUID sender;
	private UUID receiver;
	private String content;
	private MessageStatus status;
	private long timestamp;
	
	public Message(UUID sender, UUID receiver, String content, MessageStatus status, long timestamp){
		this.sender = sender;
		this.receiver = receiver;
		this.content = content;
		this.status = status;
		this.timestamp = timestamp;
	}
	
	public Message(UUID sender, UUID receiver, String content, MessageStatus status){
		this.sender = sender;
		this.receiver = receiver;
		this.content = content;
		this.status = status;
		this.timestamp = System.currentTimeMillis();
	}

	public UUID getSender() {
		return sender;
	}

	public UUID getReceiver() {
		return receiver;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public MessageStatus getStatus() {
		return status;
	}

	public void setStatus(MessageStatus status) {
		this.status = status;
	}
	
	public static void serializeMessage(ConfigurationSection messagesSection, Message message, String location){
		
		ConfigurationSection messageSection = messagesSection.createSection(location);
		
		messageSection.set("sender", message.getSender().toString());
		messageSection.set("receiver", message.getReceiver().toString());
		messageSection.set("status", message.getStatus().toString());
		messageSection.set("content", message.getContent());
		messageSection.set("timestamp", message.getTimestamp());
	}
	
	public static Message unserializeMessage(ConfigurationSection messageSection){
		UUID sender = UUID.fromString((String) messageSection.get("sender"));
		UUID receiver = UUID.fromString((String) messageSection.get("receiver"));
		String status = (String) messageSection.get("status");
		String content = (String) messageSection.get("content");
		Long timestamp = messageSection.getLong("timestamp");
		return new Message(sender, receiver, content, MessageStatus.valueOf(status), timestamp);
	}
	
}
