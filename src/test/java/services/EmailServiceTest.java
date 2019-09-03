package services;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;

import com.qanairy.services.EmailServiceImpl;

@SpringBootTest
public class EmailServiceTest {
	
	@Autowired 
	JavaMailSender email_sender;
	
	public void testEmailSendsSuccessfully(){
		EmailServiceImpl email_service = new EmailServiceImpl(email_sender);
		email_service.sendHtmlMessage("bkindred@qanairy.com", "Test", "Testing emails work and stuff");
	}
	
	public void test48HourEmailSendsSuccessfully(){
		EmailServiceImpl email_service = new EmailServiceImpl(email_sender);
		email_service.sendHtmlMessage("bkindred@qanairy.com", "Test", "Testing emails work and stuff");
	}
}
