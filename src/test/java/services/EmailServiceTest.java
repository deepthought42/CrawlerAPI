package services;

import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.qanairy.services.EmailServiceImpl;

public class EmailServiceTest {
	
	public void testEmailSendsSuccessfully(){
		EmailServiceImpl email_service = new EmailServiceImpl();
		email_service.sendSimpleMessage("bkindred@qanairy.com", "Test", "Testing emails work and stuff");
	}
}
