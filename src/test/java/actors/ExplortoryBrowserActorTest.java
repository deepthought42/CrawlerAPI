package actors;

import static org.junit.Assert.*;

import java.net.MalformedURLException;
import java.util.List;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.minion.actors.ExploratoryBrowserActor;
import com.minion.browsing.Browser;
import com.minion.browsing.BrowserConnectionFactory;
import com.qanairy.models.enums.BrowserEnvironment;

public class ExplortoryBrowserActorTest {

	//@Test
	public void verifyTransitionDetectionWorks() throws MalformedURLException{
		Browser browser = BrowserConnectionFactory.getConnection("firefox", BrowserEnvironment.TEST);
		browser.navigateTo("https://app.qanairy.com");
		WebElement email_elem = browser.getDriver().findElement(By.xpath("//input[contains(@name, 'email')]"));
		email_elem.sendKeys("bkindred@qanairy.com");
		
		WebElement password_elem = browser.getDriver().findElement(By.xpath("//input[contains(@name, 'password')]"));
		password_elem.sendKeys("password");
		
		WebElement submit_button = browser.getDriver().findElement(By.xpath("//button[contains(@type, 'submit')]"));
		submit_button.click();
		
		List<String> transition_keys = ExploratoryBrowserActor.getPageTransition(browser);
		System.err.println("transition keys :: " + transition_keys);
		browser.close();
		assertTrue(transition_keys.size() > 1);
	}
}
