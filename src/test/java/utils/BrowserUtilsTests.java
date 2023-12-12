package utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.junit.Before;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import com.looksee.models.enums.AuditCategory;
import com.looksee.services.BrowserService;
import com.looksee.utils.BrowserUtils;

public class BrowserUtilsTests {

	@Test
	public void verifySanitizeUrlWithoutSubdomainOrWww() throws MalformedURLException{
		String url = "http://qanairy.com";
		String sanitized_url = BrowserUtils.sanitizeUrl(url, false);
		
		assertTrue("http://qanairy.com".equals(sanitized_url));
	}
	
	@Test
	public void verifySanitizeUrlWithoutSubdomainOrWwwWithParams() throws MalformedURLException{
		String url = "http://qanairy.com?value=test";
		String sanitized_url = BrowserUtils.sanitizeUrl(url, false);
		
		assertTrue("http://qanairy.com".equals(sanitized_url));
	}
	
	@Test
	public void verifySanitizeUrlWithoutSubdomain() throws MalformedURLException{
		String url = "http://www.look-see.com";
		String sanitized_url = BrowserUtils.sanitizeUrl(url, false);
		
		assertTrue("http://look-see.com".equals(sanitized_url));
	}
	
	@Test
	public void verifySanitizeUrlWithSubdomain() throws MalformedURLException{
		String url = "http://test4.masschallenge.com";
		String sanitized_url = BrowserUtils.sanitizeUrl(url, false);
		
		assertTrue("http://test4.masschallenge.com".equals(sanitized_url));
	}
	
	@Test
	public void verifySanitizeUrlWithPath() throws MalformedURLException{
		String url = "http://zaelab.com/services";
		String sanitized_url = BrowserUtils.sanitizeUrl(url, false);

		assertTrue("http://zaelab.com/services".equals(sanitized_url));
	}
	
	@Test
	public void verifyPageUrlWithPath() throws MalformedURLException{
		String url = "http://zaelab.com/services";
		String sanitized_url = BrowserUtils.getPageUrl(new URL(url));

		assertTrue("zaelab.com/services".equals(sanitized_url));
		
		String url2 = "http://qa.turion.io/?#";
		String sanitized_url2 = BrowserUtils.getPageUrl(url2);

		assertTrue("qa.turion.io/?#".equals(sanitized_url2));
	}
	
	@Test
	public void httpStatusVerification() throws MalformedURLException {
		URL url = new URL("https://look-see.com/does-not-exist");
		int status = BrowserUtils.getHttpStatus(url);
		System.out.println("status :: "+status);
		assertTrue(status == 404);
		
		URL url2 = new URL("https://look-see.com");
		int status2 = BrowserUtils.getHttpStatus(url2);
		
		assertTrue(status2 == 200);
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
		
		URL valid_url4 = new URL("https://www.mirakl.com");
		boolean does_exist4 = BrowserUtils.doesUrlExist(valid_url4);
		
		assertTrue(does_exist4);
	}
	
	@Test
	public void verifyCanExtractFontFamiliesFromStylesheetCss() {
		String stylesheet = ".title { background-color: #111111; color:#ffffff} .header { font-family: 'sans-serif';} .paragraph { font-family: 'helvetica,open-sans-sans-serif' }";
		
		List<String> font_families = new ArrayList<>(BrowserUtils.extractFontFamiliesFromStylesheet(stylesheet));
		System.out.println("font_families    ::     "+font_families);
		Assert.assertTrue(font_families.size() == 2);
	}
	
	@Test
	public void testIsSecureCheck() throws IOException {
		URL url = new URL("http://www.look-see.com");

		boolean is_secure = BrowserUtils.checkIfSecure(url);
		assertTrue(is_secure);
		
		URL url_2 = new URL("https://app.look-see.com");

		boolean is_secure_2 = BrowserUtils.checkIfSecure(url_2);
		assertTrue(is_secure_2);
	}
	
	@Test
	public void isRelativeLinkTest() throws URISyntaxException {
		assertFalse( BrowserUtils.isRelativeLink("look-see.com", "look-see.com"));
		assertFalse( BrowserUtils.isRelativeLink("look-see.com", "https://look-see.com"));
		assertFalse( BrowserUtils.isRelativeLink("look-see.com", "look-see.com/product"));
		assertFalse( BrowserUtils.isRelativeLink("app.look-see.com", "app.look-see.com"));
		assertFalse( BrowserUtils.isRelativeLink("look-see.com", "//look-see.com.com/images/example.png"));
		
		assertTrue( BrowserUtils.isRelativeLink("look-see.com", "?hsLang=en"));
		assertTrue( BrowserUtils.isRelativeLink("apple.com", "/105/media/us/mac/2019/36178e80-30fd-441c-9a5b-349c6365bb36/ar/mac-pro/case-on.usdz"));
		assertTrue( BrowserUtils.isRelativeLink("look-see.com", "/products"));
		assertTrue( BrowserUtils.isRelativeLink("look-see.com", "#products"));
		assertTrue( BrowserUtils.isRelativeLink("look-see.com", "signup.html"));

	}
	
	@Test
	public void isExternalLinkTest() throws URISyntaxException, MalformedURLException {
		assertFalse( BrowserUtils.isExternalLink("look-see.com", "look-see.com"));
		assertFalse( BrowserUtils.isExternalLink("look-see.com", "/products"));
		assertFalse( BrowserUtils.isExternalLink("look-see.com", "look-see.com/products"));
		assertFalse( BrowserUtils.isExternalLink("app.look-see.com", "app.look-see.com"));
		assertFalse( BrowserUtils.isExternalLink("look-see.com", "http://look-see.com"));

		assertTrue( BrowserUtils.isExternalLink("app.look-see.com", "look-see.com"));
		assertTrue( BrowserUtils.isExternalLink("look-see.com", "app.look-see.com"));
		assertTrue( BrowserUtils.isExternalLink("look-see.com", "wikipedia.com"));
		assertTrue( BrowserUtils.isExternalLink("shootproof.com", "shootproof.community"));
		assertTrue( BrowserUtils.isExternalLink("shootproof.com", "foreground.co/about"));

	}
	
	@Test
	public void containsHost() {
		assertFalse(BrowserUtils.containsHost("index.html"));
		assertTrue(BrowserUtils.containsHost("look-see.com"));
	}
	
	@Test
	public void isSubdomain() throws URISyntaxException, MalformedURLException {
		assertFalse( BrowserUtils.isSubdomain("look-see.com", "look-see.com"));
		assertFalse( BrowserUtils.isSubdomain("look-see.com", "look-see.com/products"));
		assertFalse( BrowserUtils.isSubdomain("app.look-see.com", "app.look-see.com"));
		assertFalse(  BrowserUtils.isSubdomain("app.look-see.com", "apple.com"));
		
		assertTrue( BrowserUtils.isSubdomain("app.look-see.com", "look-see.com"));
		assertTrue( BrowserUtils.isSubdomain("look-see.com", "app.look-see.com"));
	}
	
	@Test
	public void isFile() throws URISyntaxException, MalformedURLException {
		assertFalse( BrowserUtils.isFile("look-see.com"));
		assertFalse( BrowserUtils.isFile("look-see.com/products"));
		assertFalse( BrowserUtils.isFile("app.look-see.com"));

		assertTrue( BrowserUtils.isFile("look-see.com/thisisafile.zip") );
		assertTrue( BrowserUtils.isFile("look-see.com/thisisafile.usdt") );
		assertTrue( BrowserUtils.isFile("look-see.com/thisisafile.rss") );
		assertTrue( BrowserUtils.isFile("look-see.com/thisisafile.svg") );
		assertTrue( BrowserUtils.isFile("look-see.com/thisisafile.pdf") );
		assertTrue( BrowserUtils.isFile("look-see.com/thisisafile.m3u8") );
		assertTrue( BrowserUtils.isFile("look-see.com/thisisafile.usdz") );
		assertTrue( BrowserUtils.isFile("look-see.com/thisisafile.doc") );
		assertTrue( BrowserUtils.isFile("look-see.com/thisisafile.docx") );
		assertTrue( BrowserUtils.isFile("look-see.com/thisisafile.jpg") );
	}
	
	@Test
	public void formatUrlTest() throws MalformedURLException {
		String url = "https://www.zaelab.com/blogs/using-a-continuous-delivery-model-to-innovate-faster/";
		String formatted_url = BrowserUtils.formatUrl(null, "zaelab.com", url, false);
		assertTrue(formatted_url.contentEquals("https://www.zaelab.com/blogs/using-a-continuous-delivery-model-to-innovate-faster/"));
		
		String url2 = "/products";
		String formatted_url2 = BrowserUtils.formatUrl("https", "look-see.com", url2, true);
		assertTrue(formatted_url2.contentEquals("https://look-see.com/products"));
		
		String url3 = "?lang=en";
		String formatted_url3 = BrowserUtils.formatUrl("https", "look-see.com", url3, true);
		assertTrue(formatted_url3.contentEquals("https://look-see.com?lang=en"));
		
		String url4 = "#products";
		String formatted_url4 = BrowserUtils.formatUrl("https", "look-see.com", url4, true);
		assertTrue(formatted_url4.contentEquals("https://look-see.com#products"));
	}

	@Test
	public void doesUrlExistTest() throws Exception {
		String url1 = "https://www.look-see.com";
		
		assertTrue(BrowserUtils.doesUrlExist(url1));
		
		//String url2 = "https://www.businesswirefasfasfasfew.com/";
		//assertFalse(BrowserUtils.doesUrlExist(url2));
		
		String url3 = "https://www.look-see.com/product";
		
		assertTrue(BrowserUtils.doesUrlExist(url3));
	}

	@InjectMocks
	private BrowserService browser_service;

	@Before
	public void start(){
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void isValidUrlTest() throws MalformedURLException, URISyntaxException {
		String host = "look-see.com";
		String sanitized_url0 = "abcde://look-see.com";

		String sanitized_url1 = "https://look-see.com";
		String sanitized_url2 = "itms-apps://look-see.com";
		String sanitized_url3 = "snap://look-see.com";
		String sanitized_url4 = "tel://look-see.com";
		String sanitized_url5 = "mailto://look-see.com";
		String sanitized_url6 = "applenews://look-see.com";
		String sanitized_url7 = "applenewss://look-see.com";
		String sanitized_url8 = "mailto://look-see.com";
		
		assertFalse(BrowserUtils.isValidUrl(sanitized_url0, host));
		assertTrue(BrowserUtils.isValidUrl(sanitized_url1, host));
		assertFalse(BrowserUtils.isValidUrl(sanitized_url2, host));
		assertFalse(BrowserUtils.isValidUrl(sanitized_url3, host));
		assertFalse(BrowserUtils.isValidUrl(sanitized_url4, host));
		assertFalse(BrowserUtils.isValidUrl(sanitized_url5, host));
		assertFalse(BrowserUtils.isValidUrl(sanitized_url6, host));
		assertFalse(BrowserUtils.isValidUrl(sanitized_url7, host));
		assertFalse(BrowserUtils.isValidUrl(sanitized_url8, host));
	}

	/*
	@Test
	public void extractPageSrcTest() throws MalformedURLException {
		URL sanitized_url1 = new URL("https://www.wikipedia.org/");
		URL sanitized_url2 = new URL("https://www.look-see.com/");

		String page_src1 = BrowserUtils.extractPageSrc(sanitized_url1, browser_service);
		String page_src2 = BrowserUtils.extractPageSrc(sanitized_url2, browser_service);

		assertTrue(!page_src1.isEmpty());
		assertTrue(page_src1 != page_src2);
	}
	*/
	
	@Test
	public void isValidLinkTest(){
		//The first link should be correct, all else should be malformed for testing
		
		//Set a mock page
		String page_src = "<a href=\"https://www.wikipedia.org\">Wikipedia</a><a href=\"itms-apps://www.apple.com\">Apple</a><a href=\"tel://www.google.com\">Google</a><a href=\"mailto://www.snapchat.com\">Snapchat</a>";
		//Get the page source
		Document doc = Jsoup.parse(page_src);
		Elements links = doc.select("a");
		
		//Get the first link
		Element link = links.get(0);
		String href_str = link.attr("href");
		href_str = href_str.replaceAll(";", "").trim();

		assertTrue(BrowserUtils.isValidLink(href_str));

		//check every link to make sure that they are fail the assertion
		for(int i = 1; i < links.size(); ++i){
			href_str = links.get(i).attr("href");
			href_str = href_str.replaceAll(";", "").trim();
			
			assertFalse(BrowserUtils.isValidLink(href_str));
		}
		
		assertFalse(BrowserUtils.isValidLink(null));
		assertFalse(BrowserUtils.isValidLink(""));
	}

	@Test
	public void hasValidHttpStatusTest() throws MalformedURLException {
		URL wikipedia_link = new URL("https://www.wikipedia.org/");
		URL wikipedia_bad_link = new URL("https://www.wikipadai.org/");

		assertTrue(BrowserUtils.hasValidHttpStatus(wikipedia_link));
		assertFalse(BrowserUtils.hasValidHttpStatus(wikipedia_bad_link));

	}
}
