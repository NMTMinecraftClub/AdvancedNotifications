package com.m0pt0pmatt.advancednotifications.mail;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import com.sun.mail.smtp.SMTPTransport;


/**
 * An EmailSender can send emails. It uses a Gmail account to send the emails
 * @author Matthew
 *
 */
public class EmailSender extends Thread{

	private LinkedList<Email> toSend;
	
	//The email and password the EmailSender uses to send emails
	private String emailAccount;
	private String password;
	
	/**
	 * Sets up the email and password by pulling them from a config file named "password.yml"
	 * "password.yml" is found in the data folder for the given Plugin
	 * @param plugin
	 */
	public EmailSender(Plugin plugin){
		
		toSend = new LinkedList<Email>();
		
		try {
			
			//Make sure data folder exists
			if (!plugin.getDataFolder().exists()){
				plugin.getDataFolder().mkdir();
			}
			
			//make sure file exists
			File file = new File(plugin.getDataFolder(), "password.yml");
			if (!file.exists()){
				file.createNewFile();
			}
			
			//load the configuration
			YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
			
			//get the email
			if (config.contains("email")){
				emailAccount = config.getString("email");
			} else {
				emailAccount = "null";
			}
			
			//get the password
			if (config.contains("password")){
				password = config.getString("password");
			} else {
				password = "null";
			}
		
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	@Override
	public void run(){
		while(true){
			try {
				Thread.sleep(1000 * 3);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (!toSend.isEmpty()){
				Email email = toSend.removeFirst();
				sendEmail(email, emailAccount, password);
			}
		}
	}
	
	public void sendEmail(Email email){
		toSend.addLast(email);
	}
	
	/**
	 * sends the email
	 */
	private void sendEmail(Email email, String emailAccount, String password){
		try {

			//Set system properties
			Properties props = System.getProperties();
	        props.put("mail.smtps.host","smtp.gmail.com");
	        props.put("mail.smtps.auth","true");
	        
	        //Create a session
	        Session session = Session.getInstance(props, null);
	        
	        //Create a message
	        Message msg = new MimeMessage(session);
	        
	        //Set sender
	        msg.setFrom(new InternetAddress(email.getSender()));
	        
	        //Set receiver
	        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email.getReceiver(), false));
	        
	        //Set subject
	        msg.setSubject(email.getSubject());
	        
	        //Set content
	        msg.setText(email.getContent());
	        
	        msg.setHeader("X-Mailer", "Tov Are's program");
	        
	        msg.setSentDate(new Date());
	        
	        //Send email
	        SMTPTransport t =
	            (SMTPTransport)session.getTransport("smtps");
	        t.connect("smtp.gmail.com", this.emailAccount, this.password);
	        t.sendMessage(msg, msg.getAllRecipients());
	        t.close();

		} catch (Exception e) {
		    e.printStackTrace();
		}
	}
	
}
