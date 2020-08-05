package utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Test;

import com.qanairy.utils.BrowserUtils;

public class BrowserUtilsTests {

	@Test
	public void verifySanitizeUrlWithoutSubdomainOrWww() throws MalformedURLException{
		String url = "http://qanairy.com";
		String sanitized_url = BrowserUtils.sanitizeUrl(url);
		
		assertTrue("http://qanairy.com".equals(sanitized_url));
	}
	
	@Test
	public void verifySanitizeUrlWithoutSubdomainOrWwwWithParams() throws MalformedURLException{
		String url = "http://qanairy.com?value=test";
		String sanitized_url = BrowserUtils.sanitizeUrl(url);
		
		assertTrue("http://qanairy.com".equals(sanitized_url));
	}
	
	@Test
	public void verifySanitizeUrlWithoutSubdomain() throws MalformedURLException{
		String url = "http://www.qanairy.com";
		String sanitized_url = BrowserUtils.sanitizeUrl(url);
		
		assertTrue("http://www.qanairy.com".equals(sanitized_url));
	}
	
	@Test
	public void verifySanitizeUrlWithSubdomain() throws MalformedURLException{
		String url = "http://test4.masschallenge.com";
		String sanitized_url = BrowserUtils.sanitizeUrl(url);
		
		assertTrue("http://test4.masschallenge.com".equals(sanitized_url));
	}
	
	@Test
	public void verifySanitizeUrlWithPath() throws MalformedURLException{
		String url = "http://zaelab.com/services";
		String sanitized_url = BrowserUtils.sanitizeUrl(url);
		
		assertTrue("http://zaelab.com/services".equals(sanitized_url));
	}
	
	@Test
	public void verifyLinkExtraction() {
		
	}
	
	@Test
	public void verifyUrlExists() throws IOException {
		URL valid_url = new URL("https://www.google.com");
		boolean does_exist = BrowserUtils.doesUrlExist(valid_url);
		assertTrue(does_exist);
	}
}
