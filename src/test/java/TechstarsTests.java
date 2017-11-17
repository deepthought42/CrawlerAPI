import java.net.MalformedURLException;
import java.net.URL;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.server.FirefoxDriverProvider;
import org.testng.annotations.Test;

public class TechstarsTests {

		@Test
		public void testHttpsSettingsInPhantomjs(){
			DesiredCapabilities cap = DesiredCapabilities.phantomjs();
			cap.setJavascriptEnabled(true);
			cap.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, new String[] {"--ssl-protocol=tlsv1"});

			/*
			DesiredCapabilities cap = DesiredCapabilities.phantomjs();
			cap.setJavascriptEnabled(true);
			cap.setCapability("web-security","true");
			cap.setCapability("ssl-protocol","any");
			cap.setCapability("ignore-ssl-errors","true");//,"--webdriver-loglevel=NONE"});
			*/
			//WebDriver driver = new PhantomJSDriver(cap);
			// optional video recording
			/*String record_video = "True";
			// video record
			if (record_video.equalsIgnoreCase("True")) {
				cap.setCapability("video", "True"); // NOTE: "True" is a case sensitive string, not boolean.
			} else {
				cap.setCapability("video", "False"); // NOTE: "False" is a case sensitive string, not boolean.
			}*/
	        try {
	        	//WebDriver driver = new FirefoxDriver(cap);
				RemoteWebDriver driver = new RemoteWebDriver(new URL("http://104.131.30.168:4444/wd/hub"), cap);
				driver.get("https://www.techstars.com");
				//driver.navigate().to("https://www.techstars.com");
				String src = driver.getPageSource();
				String domain = driver.getCurrentUrl();
				
				System.err.println("SOURCE :: "+src);
				System.err.println("Domain :: " + domain);
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	 
	        
		}
}
