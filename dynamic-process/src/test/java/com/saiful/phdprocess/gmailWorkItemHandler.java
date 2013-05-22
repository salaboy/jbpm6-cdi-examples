package com.saiful.phdprocess;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
 
public class gmailWorkItemHandler  {
	public String userName;
	public String password;
	
	public gmailWorkItemHandler() {
	}

	public gmailWorkItemHandler(String userName, String password) {
		this.userName = userName;
		this.password = password;
	}
	
	public void sendGmail(String to1, String to2, String to3, String subject, String body) {
		
		Properties props = new Properties();
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.socketFactory.port", "465");
		props.put("mail.smtp.socketFactory.class",
				"javax.net.ssl.SSLSocketFactory");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.port", "465");
 
		Session session = Session.getDefaultInstance(props,
			new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(userName,password);
				}
			});
 
		try {
			int count = 1;
			
			if(!to2.isEmpty() && !to2.trim().isEmpty()){
				count++;
				if(!to3.isEmpty() && !to3.trim().isEmpty()){
					count++;
				}
			}
			InternetAddress[] addressTo = new InternetAddress[count];
			addressTo[0] = new InternetAddress(to1);
			if(!to2.isEmpty() && !to2.trim().isEmpty()){
				addressTo[1] = new InternetAddress(to2);
				if(!to3.isEmpty() && !to3.trim().isEmpty()){
					addressTo[2] = new InternetAddress(to3);
				}
			}
			
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(userName));
			message.setRecipients(Message.RecipientType.TO,addressTo);
			message.setSubject(subject);
			message.setText(body);
 
			Transport.send(message);
  
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
	}
	
}