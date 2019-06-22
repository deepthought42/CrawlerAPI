package com.minion.browsing;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;
import com.qanairy.models.enums.BrowserEnvironment;

public class BrowserConnectionFactory {

	//GOOGLE CLOUD CLUSTER
	//private static final String[] CHROME_DISCOVERY_HUB_IP_ADDRESS = {"35.239.77.58:4444", "23.251.149.198:4444"};
	//private static final String[] FIREFOX_DISCOVERY_HUB_IP_ADDRESS = {"35.239.245.6:4444", "173.255.118.118:4444"};

    private static final String[] DISCOVERY_HUB_IP_ADDRESS = {"35.239.77.58:4444", "35.239.245.6:4444", "173.255.118.118:4444"};
	private static final String TEST_HUB_IP_ADDRESS = "34.73.96.186:4444";

	// PRODUCTION HUB ADDRESS
	//private static final String HUB_IP_ADDRESS= "142.93.192.184:4444";

	//STAGING HUB ADDRESS
	//private static final String HUB_IP_ADDRESS="159.89.226.116:4444";


	public static Browser getConnection(String browser, BrowserEnvironment environment) throws MalformedURLException{
		URL hub_url = null;
		if(environment.equals(BrowserEnvironment.TEST)){
			hub_url = new URL( "http://"+TEST_HUB_IP_ADDRESS+"/wd/hub" );
		}
		else if(environment.equals(BrowserEnvironment.DISCOVERY) && "chrome".equalsIgnoreCase(browser)){
			Random randomGenerator = new Random();
			int randomInt = randomGenerator.nextInt(DISCOVERY_HUB_IP_ADDRESS.length);
			hub_url = new URL( "http://"+DISCOVERY_HUB_IP_ADDRESS[randomInt]+"/wd/hub");
		}
		else if(environment.equals(BrowserEnvironment.DISCOVERY) && "firefox".equalsIgnoreCase(browser)){
			Random randomGenerator = new Random();
			int randomInt = randomGenerator.nextInt(DISCOVERY_HUB_IP_ADDRESS.length);
			hub_url = new URL( "http://"+DISCOVERY_HUB_IP_ADDRESS[randomInt]+"/wd/hub");
		}

		return new Browser(browser, hub_url);
	}

}
