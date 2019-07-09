package utils;

import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;

import org.junit.Test;

import com.qanairy.utils.BrowserUtils;

public class BrowserUtilsTests {

	@Test
	public void verifySanitizeUrlWithoutSubdomainOrWww() throws MalformedURLException{
		String url = "http://qanairy.com";
		String sanitized_url = BrowserUtils.sanitizeUrl(url);
		
		assertTrue("http://www.qanairy.com".equals(sanitized_url));
	}
	
	@Test
	public void verifySanitizeUrlWithoutSubdomainOrWwwWithParams() throws MalformedURLException{
		String url = "http://qanairy.com?value=test";
		String sanitized_url = BrowserUtils.sanitizeUrl(url);
		
		assertTrue("http://www.qanairy.com".equals(sanitized_url));
	}
	
	@Test
	public void verifySanitizeUrlWithoutSubdomain() throws MalformedURLException{
		String url = "http://www.qanairy.com";
		String sanitized_url = BrowserUtils.sanitizeUrl(url);
		
		assertTrue("http://www.qanairy.com".equals(sanitized_url));
	}
	
	@Test
	public void verifySanitizeUrlWithSubdomain(){
		String url = "http://test4.masschallenge.com";
		String sanitized_url = BrowserUtils.sanitizeUrl(url);
		
		assertTrue("http://test4.masschallenge.com".equals(sanitized_url));
	}
}
