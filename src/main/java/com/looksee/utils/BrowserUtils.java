package com.looksee.utils;

import java.awt.Color;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.looksee.browsing.Browser;
import com.looksee.models.ElementState;
import com.looksee.models.LookseeObject;
import com.looksee.models.PageState;
import com.looksee.models.audit.ColorData;

import io.github.resilience4j.retry.annotation.Retry;

@Retry(name = "default")
public class BrowserUtils {
	private static Logger log = LoggerFactory.getLogger(BrowserUtils.class);
	
	public static String sanitizeUrl(String url) {
		assert url != null;
		assert !url.isEmpty();
		
		if(!url.contains("://")) {
			url = "http://"+url;
		}
		
		url = url.replace("www.", "");
		String domain = url;
		int param_index = domain.indexOf("?");
		if(param_index >= 0){
			domain = domain.substring(0, param_index);
		}
		
		domain = domain.replace("index.html", "");
		domain = domain.replace("index.htm", "");

		if(!domain.isEmpty() && domain.charAt(domain.length()-1) == '/'){
			domain = domain.substring(0, domain.length()-1);
		}
		
		//remove any anchor link references
		int hash_index = domain.indexOf("#");
		if(hash_index > 0) {
			domain = domain.substring(0, hash_index);
		}
		return domain;
	}
	
	/**
	 * Reformats url so that it matches the Look-see requirements
	 * 
	 * @param url 
	 * 
	 * @return sanitized url string
	 * 
	 * @throws MalformedURLException
	 * 
	 * @pre url != null
	 * @pre !url.isEmpty()
	 */
	public static String sanitizeUserUrl(String url) throws MalformedURLException  {
		assert url != null;
		assert !url.isEmpty();
		
		if(!url.contains("://")) {
			url = "http://"+url;
		}
		URL new_url = new URL(url);
		//check if host is subdomain
		String new_host = new_url.getHost();
		new_host.replace("www.", "");
		/*
		if(!new_host.startsWith("www.")){
			new_host = "www."+new_host;
		}
		*/
		String new_key = new_host+new_url.getPath();
		if(new_key.endsWith("/")){
			new_key = new_key.substring(0, new_key.length()-1);
		}
		
		new_key = new_key.replace("index.html", "");
		new_key = new_key.replace("index.htm", "");
		
		if(new_key.endsWith("/")){
			new_key = new_key.substring(0, new_key.length()-1);
		}
				
		return "http://"+new_key;
	}

	public static ElementState updateElementLocations(Browser browser, ElementState element) {
		WebElement web_elem = browser.findWebElementByXpath("");//element.getXpath());
		Point location = web_elem.getLocation();
		if(location.getX() != element.getXLocation() || location.getY() != element.getYLocation()){
			log.warn("updating element state from ::  ( " + element.getXLocation() + " , "+element.getYLocation()+" ) " + " to    ::  ( " + location.getX() + " , "+location.getY()+")");
			element.setXLocation(location.getX());
			element.setYLocation(location.getY());
		}
		
		return element;
	}

	public static boolean doesHostChange(List<String> urls) throws MalformedURLException {
		for(String url : urls){
			String last_host_and_path = "";
			URL url_obj = new URL(url);
			String host_and_path = url_obj.getHost()+url_obj.getPath();
			if(!last_host_and_path.equals(host_and_path)){
				return true;
			}
		}
		return false;
	}
	
	public static boolean doesSpanMutlipleDomains(String start_url, String end_url, List<LookseeObject> path_objects) throws MalformedURLException {
		return !(start_url.trim().contains(new URL(end_url).getHost()) || end_url.contains((new URL(PathUtils.getLastPageState(path_objects).getUrl()).getHost())));
	}

	/**
	 * Checks if url is part of domain including sub-domains
	 *  
	 * @param domain_host host of {@link Domain domain}
	 * @param url 
	 * 
	 * @return true if url is external, otherwise false
	 * 
	 * @throws MalformedURLException
	 * @throws URISyntaxException 
	 */
	public static boolean isExternalLink(String domain_host, String url) throws MalformedURLException, URISyntaxException {
		//return ((!domain_host.contains(url) && !url.contains(domain_host)) && !isRelativeLink(url, domain_host)) || url.contains("////");
		return (!url.contains(domain_host) && !isRelativeLink(domain_host, url) ) || url.contains("////");
	}
	
	/**
	 * Returns true if link is empty or if it starts with a '/' and doesn't contain the domain host
	 * @param domain_host host (example: google.com)
	 * @param link_url link href value to be evaluated
	 * 
	 * @return true if link is empty or if it starts with a '/' and doesn't contain the domain host, otherwise false
	 * @throws URISyntaxException
	 */
	public static boolean isRelativeLink(String domain_host, String link_url) {
		assert domain_host != null;
		assert link_url != null;
		
		return link_url.isEmpty() 
				|| (link_url.charAt(0) == '/' && !link_url.contains(domain_host)) 
				|| (link_url.charAt(0) == '?' && !link_url.contains(domain_host))
				|| (link_url.charAt(0) == '#' && !link_url.contains(domain_host));
	}
	

	public static boolean isSubdomain(String domain_host, String new_host) throws URISyntaxException {
		assert domain_host != null;
		assert new_host != null;
		
		boolean is_contained = new_host.contains(domain_host) || domain_host.contains(new_host);
		boolean is_equal = new_host.equals(domain_host);
		boolean ends_with = new_host.endsWith(domain_host) || domain_host.endsWith(new_host);
		return is_contained && !is_equal && ends_with;
	}
	
	public static boolean isFile(String url) {
		assert url != null;
		
		return url.endsWith(".zip") 
				|| url.endsWith(".usdt") 
				|| url.endsWith(".rss") 
				|| url.endsWith(".svg") 
				|| url.endsWith(".pdf")
				|| isImageUrl(url);
	}
	
	
	/**
	 * Extracts a {@link List list} of link urls by looking up `a` html tags and extracting the href values
	 * 
	 * @param source valid html source
	 * @return {@link List list} of link urls
	 */
	public static List<String> extractLinkUrls(String source) {
		List<String> link_urls = new ArrayList<>();
		Document document = Jsoup.parse(source);
		Elements elements = document.getElementsByTag("a");
		
		for(Element element : elements) {
			String url = element.attr("href");
			if(!url.isEmpty()) {
				link_urls.add(url);
			}
		}
		return link_urls;
	}
	
	/**
	 * Extracts a {@link List list} of link urls by looking up `a` html tags and extracting the href values
	 * 
	 * @param source valid html source
	 * @return {@link List list} of link urls
	 */
	public static List<com.looksee.models.Element> extractLinks(List<com.looksee.models.Element> elements) {
		List<com.looksee.models.Element> links = new ArrayList<>();
		
		for(com.looksee.models.Element element : elements) {
			if(element.getName().equalsIgnoreCase("a")) {
				links.add(element);
			}
		}
		return links;
	}
	
	/**
	 *  check if link returns valid content ie. no 404 or page not found errors when navigating to it
	 * @param url
	 * @return
	 * @throws Exception 
	 */
	public static boolean doesUrlExist(URL url) throws Exception {
		assert(url != null);
		
		//perform check for http clients
		if("http".equalsIgnoreCase(url.getProtocol())){
			HttpURLConnection huc = (HttpURLConnection) url.openConnection();
			int responseCode = huc.getResponseCode();
			
			if (responseCode != 404) {
				return true;
			} else {
				return false;
			}
		}
		if("https".equalsIgnoreCase(url.getProtocol())){
			HttpsURLConnection https_client = getHttpsClient(url.toString());

			try {
				int responseCode = https_client.getResponseCode();

				if (responseCode != 404) {
					return true;
				} else {
					return false;
				}
			} catch(UnknownHostException e) {
				return false;
			}
			catch(SSLException e) {
				log.warn("SSL Exception occurred while checking if URL exists");
				return false;
			}
		}
		else if("mailto".equalsIgnoreCase(url.getProtocol())) {
			//TODO check if mailto address is vailid
		}
		else {
			// TODO handle image links
		}
		
		return false;
	}
	
	/**
	 *  check if link returns valid content ie. no 404 or page not found errors when navigating to it
	 * @param url
	 * @return
	 * @throws Exception 
	 */
	public static boolean doesUrlExist(String url_str) throws Exception {
		assert(url_str != null);
		
		if(url_str.startsWith("#") 
			|| BrowserUtils.isJavascript(url_str)
			|| url_str.startsWith("itms-apps:")
			|| url_str.startsWith("snap:")
			|| url_str.startsWith("tel:")
			|| url_str.startsWith("mailto:")
		) {
			return true;
		}
	
		URL url = new URL(url_str);
		//perform check for http clients
		if("http".equalsIgnoreCase(url.getProtocol())){
			HttpURLConnection huc = (HttpURLConnection) url.openConnection();
			int responseCode = huc.getResponseCode();
			
			if (responseCode != 404) {
				return true;
			} else {
				return false;
			}
		}
		if("https".equalsIgnoreCase(url.getProtocol())){
			HttpsURLConnection https_client = getHttpsClient(url.toString());

			try {
				int responseCode = https_client.getResponseCode();

				if (responseCode != 404) {
					return true;
				} else {
					return false;
				}
			} catch(UnknownHostException e) {
				return false;
			}
			catch(SSLException e) {
				log.warn("SSL Exception occurred while checking if URL exists");
				return false;
			}
		}
		else {
			// TODO handle image links
		}
		
		return false;
	}

	private static HttpsURLConnection getHttpsClient(String url) throws Exception {
		 
        // Security section START
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
 
                @Override
                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
 
                @Override
                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
            }};
 
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        // Security section END
        
        HttpsURLConnection client = (HttpsURLConnection) new URL(url).openConnection();
        //add request header
        client.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36");
        return client;
    }
	
	/**
	 * Checks if url string ends with an image suffix indicating that it points to an image file
	 * 
	 * @param href url to examine
	 * 
	 * @return true if any suffixes match, false otherwise
	 * 
	 * @pre href != nuill
	 */
	public static boolean isImageUrl(String href) {
		assert href != null;
		
		return href.endsWith(".jpg") || href.endsWith(".png") || href.endsWith(".gif") || href.endsWith(".bmp") || href.endsWith(".tiff") || href.endsWith(".webp") || href.endsWith(".bpg") || href.endsWith(".heif");
	}
	
	/**
	 * Opens stylesheet content and searches for font-family css settings
	 * 
	 * @param stylesheet_url
	 * @return
	 * @throws IOException
	 * 
	 * @pre stylesheet_url != null;
	 * 
	 */
	public static Collection<? extends String> extractFontFamiliesFromStylesheet(String stylesheet) {
		assert stylesheet != null;
		
		Map<String, Boolean> font_families = new HashMap<>();

		//extract text matching font-family:.*; from stylesheets
		//for each match, extract entire string even if it's a list and add string to font-families list
		String patternString = "font-family:(.*?)[?=;|}]";

        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(stylesheet);
        while(matcher.find()) {
        	String font_family_setting = matcher.group();
        	if(font_family_setting.contains("inherit")) {
        		continue;
        	}
        	font_family_setting = font_family_setting.replaceAll("'", "");
        	font_family_setting = font_family_setting.replaceAll("\"", "");
        	font_family_setting = font_family_setting.replaceAll(";", "");
        	font_family_setting = font_family_setting.replaceAll(":", "");
        	font_family_setting = font_family_setting.replaceAll(":", "");
        	font_family_setting = font_family_setting.replaceAll("}", "");
        	font_family_setting = font_family_setting.replaceAll("!important", "");
        	font_family_setting = font_family_setting.replaceAll("font-family", "");
        	
        	font_families.put(font_family_setting.trim(), Boolean.TRUE);
        }
        
        return font_families.keySet();
	}
	
	/**
	 * Retrieves {@link ElementStates} that contain text
	 * 
	 * @param element_states
	 * @return
	 */
	public static List<ElementState> getTextElements(List<ElementState> element_states) {
		assert element_states != null;
		
		List<ElementState> element_list = new ArrayList<>();
		for(ElementState element : element_states ) {
			if(element.getOwnedText() != null && !element.getOwnedText().trim().isEmpty()) {
				element_list.add(element);
			}
		}
		
		return element_list;
	}

	public static String getTitle(PageState page_state) {
		Document doc = Jsoup.parse(page_state.getSrc());
		
		return doc.title();
	}

	/**
	 * Extracts set of colors declared as background or text color in the css
	 * 
	 * @param stylesheet
	 * @return
	 */
	public static Collection<? extends ColorData> extractColorsFromStylesheet(String stylesheet) {
		assert stylesheet != null;
		
		List<ColorData> colors = new ArrayList<>();

		//extract text matching font-family:.*; from stylesheets
		//for each match, extract entire string even if it's a list and add string to font-families list
       for(String prop_setting : extractCssPropertyDeclarations("background-color", stylesheet)) {
    	   if(prop_setting.startsWith("#")) {
    		   
    		   Color color = hex2Rgb(prop_setting.trim().substring(1));
    		   colors.add(new ColorData(color.getRed() + ","+color.getGreen()+","+color.getBlue()));
    	   }
    	   else if( prop_setting.startsWith("rgb") ){
    		   colors.add(new ColorData(prop_setting));
    	   }
        }

        for(String prop_setting : extractCssPropertyDeclarations("color", stylesheet)) {
        	if(prop_setting.startsWith("#")) {
     		   Color color = hex2Rgb(prop_setting.trim().substring(1));
     		   colors.add(new ColorData(color.getRed() + ","+color.getGreen()+","+color.getBlue()));
     	   }
     	   else if( prop_setting.startsWith("rgb") ){
     		   colors.add(new ColorData(prop_setting));
     	   }
        }
        
        return colors;
	}
	
	/**
	 * Extracts css property settings from a string containing valid css
	 * @param prop
	 * @param css
	 * @return
	 */
	public static List<String> extractCssPropertyDeclarations(String prop, String css) {
		assert prop != null;
		assert css != null;
		
		String patternString = prop+":(.*?)[?=;|}]";
		List<String> settings = new ArrayList<>();

        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(css);
        while(matcher.find()) {
        	String setting = matcher.group();
        	if(setting.contains("inherit")
				|| setting.contains("transparent")) {
        		continue;
        	}
        	setting = setting.replaceAll("'", "");
        	setting = setting.replaceAll("\"", "");
        	setting = setting.replaceAll(";", "");
        	setting = setting.replaceAll(":", "");
        	setting = setting.replaceAll(":", "");
        	setting = setting.replaceAll("}", "");
        	setting = setting.replaceAll("!important", "");
        	setting = setting.replaceAll(prop, "");

        	settings.add(setting);
        }
        
        return settings;
	}

	
	/**
	 * Converts hexadecimal colors to RGB format
	 * @param color_str e.g. "#FFFFFF"
	 * @return 
	 */
	public static Color hex2Rgb(String color_str) {
		assert color_str != null;

		if(color_str.contentEquals("0")) {
			return new Color(0,0,0);
		}
		if(color_str.length() == 3) {
			color_str = expandHex(color_str);
		}
		
	    return new Color(
	            Integer.valueOf( color_str.substring( 0, 2 ), 16 ),
	            Integer.valueOf( color_str.substring( 2, 4 ), 16 ),
	            Integer.valueOf( color_str.substring( 4, 6 ), 16 ) );
	}

	private static String expandHex(String color_str) {
		String expanded_hex = "";
		for(int idx = 0; idx < color_str.length(); idx++) {
			expanded_hex += color_str.charAt(idx)  + color_str.charAt(idx);
		}
		
		return expanded_hex;
	}

	public static boolean isTextBold(String font_weight) {
		return font_weight.contentEquals("bold")
				|| font_weight.contentEquals("bolder")
				|| font_weight.contentEquals("700")
				|| font_weight.contentEquals("800")
				|| font_weight.contentEquals("900");
	}

	public static String getPageUrl(URL sanitized_url) {
		String path = sanitized_url.getPath();
    	if("/".contentEquals(path.strip())) {
    		path = "";
    	}
    	String page_url = sanitized_url.getHost() + path;
    	
    	return page_url.replace("www.", "");
	}

	/**
	 * Checks the http status codes received when visiting the given url
	 * 
	 * @param url
	 * @param title
	 * @param content
	 * @return
	 * @throws IOException
	 */
	public static int getHttpStatus(URL url) {
		int status_code = 500;
		try {
			if(url.getProtocol().contentEquals("http")) {
				HttpURLConnection con = (HttpURLConnection)url.openConnection();
				status_code = con.getResponseCode();
				//log.warn("HTTP status code = "+status_code);
				return status_code;
			}
			else if(url.getProtocol().contentEquals("https")) {
				HttpURLConnection con = (HttpURLConnection)url.openConnection();
				status_code = con.getResponseCode();
				log.warn("HTTPS status code = "+status_code);
				return status_code;		
			}
			else {
				log.warn("URL Protocol not found :: "+url.getProtocol());
			}
		}
	    catch(IOException e) {
	    	status_code = 404;
	    	e.printStackTrace();
	    }
		return status_code;
	}
	
	/**
	 * Checks if the server has certificates. Expects an https protocol in the url
	 * 
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public static boolean checkIfSecure(URL url) {
        log.warn("Checking if page is secure...."+url.toString());
        //dumpl all cert info
        //print_https_cert(con);
        boolean is_secure = false;
        try{
        	HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
        	con.connect();
        	is_secure = con.getServerCertificates().length > 0;
        }
        catch(Exception e) {
        	log.warn("an error was encountered while checking for SSL!!!!");
        	e.printStackTrace();
        }
        
        return is_secure;
	}
	
	private static void print_https_cert(HttpsURLConnection con){
	     
	    if(con!=null){
	            
	      try {
	                
			    System.out.println("Cipher Suite : " + con.getCipherSuite());
			    System.out.println("\n");
			                
			    Certificate[] certs = con.getServerCertificates();
			    for(Certificate cert : certs){
			       System.out.println("Cert Type : " + cert.getType());
			       System.out.println("Cert Hash Code : " + cert.hashCode());
			       System.out.println("Cert Public Key Algorithm : " 
			                                    + cert.getPublicKey().getAlgorithm());
			       System.out.println("Cert Public Key Format : " 
			                                    + cert.getPublicKey().getFormat());
			       System.out.println("\n");
			    }
		                
		    } catch (SSLPeerUnverifiedException e) {
		        e.printStackTrace();
		    }
	    }	    
   }

	public static boolean doesElementHaveBackgroundColor(WebElement web_element) {
		String background_color = web_element.getCssValue("background-color");
		return background_color != null && !background_color.isEmpty();
	}

	public static boolean doesElementHaveFontColor(WebElement web_element) {
		String font_color = web_element.getCssValue("color");
		return font_color != null && !font_color.isEmpty();
	}

	public static boolean isElementBackgroundImageSet(WebElement web_element) {
		String background_image = web_element.getCssValue("background-image");
		return background_image != null && !background_image.trim().isEmpty() && !background_image.trim().contentEquals("none");
	}

	public static double convertPxToPt(double pixel_size) {
		return pixel_size * 0.75;
	}

	public static boolean isJavascript(String href) {
		return href.startsWith("javascript:");
	}

	public static boolean isLargerThanViewport(Dimension element_size, int viewportWidth, int viewportHeight) {
		return element_size.getWidth() > viewportWidth || element_size.getHeight() > viewportHeight;
	}

	/**
	 * Handles extra formatting for relative links
	 * @param protocol TODO
	 * @param host
	 * @param href
	 * @return
	 * @throws MalformedURLException
	 */
	public static String formatUrl(String protocol, String host, String href) throws MalformedURLException {
		href = href.replaceAll(";", "").trim();
		if(href == null 
			|| href.isEmpty() 
			|| BrowserUtils.isJavascript(href)
			|| href.startsWith("itms-apps:")
			|| href.startsWith("snap:")
			|| href.startsWith("tel:")
			|| href.startsWith("mailto:")
		) {
			return href;
		}
		
		//URL sanitized_href = new URL(BrowserUtils.sanitizeUrl(href));
		//href = BrowserUtils.getPageUrl(sanitized_href);
		//check if external link
		if(BrowserUtils.isRelativeLink(host, href)) {
			href = protocol + "://" + host + href;
		}
		
		return href;
	}
}
