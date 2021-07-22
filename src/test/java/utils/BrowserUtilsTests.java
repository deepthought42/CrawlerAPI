package utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.looksee.utils.BrowserUtils;

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
		String url = "http://www.look-see.com";
		String sanitized_url = BrowserUtils.sanitizeUrl(url);
		
		assertTrue("http://look-see.com".equals(sanitized_url));
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
	public void verifyUrlExists() throws Exception {
		URL valid_url = new URL("https://www.google.com");
		boolean does_exist = BrowserUtils.doesUrlExist(valid_url);
		
		assertTrue(does_exist);

		URL valid_url2 = new URL("https://app.look-see.com");
		boolean does_exist2 = BrowserUtils.doesUrlExist(valid_url2);
		
		assertTrue(does_exist2);
		
		URL valid_url3 = new URL("https://app-nonexisting.look-see.com");
		boolean does_exist3 = BrowserUtils.doesUrlExist(valid_url3);
		
		assertFalse(does_exist3);
	}
	
	@Test
	public void verifyCanExtractFontFamiliesFromStylesheetCss() {
		String stylesheet = ".title { background-color: #111111; color:#ffffff} .header { font-family: 'sans-serif';} .paragraph { font-family: 'helvetica,open-sans-sans-serif' }";
		
		List<String> font_families = new ArrayList<>(BrowserUtils.extractFontFamiliesFromStylesheet(stylesheet));
		System.out.println("font_families    ::     "+font_families);
		Assert.assertTrue(font_families.size() == 2);
	}
	
	public void testIsSecureCheck() throws IOException {
		URL url = new URL("http://www.look-see.com");

		boolean is_secure = BrowserUtils.checkIfSecure(url);
		
		assertTrue(is_secure);
		
		URL url_2 = new URL("http://app.look-see.com");

		boolean is_secure_2 = BrowserUtils.checkIfSecure(url_2);
		
		assertTrue(is_secure_2);
	}
}
