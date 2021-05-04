package services;

import com.looksee.services.EmailServiceImpl;

public class EmailServiceTest {
	
	public void testEmailSendsSuccessfully(){
		EmailServiceImpl email_service = new EmailServiceImpl();
		email_service.sendSimpleMessage("bkindred@qanairy.com", "Test", "Testing emails work and stuff");
	}
}
