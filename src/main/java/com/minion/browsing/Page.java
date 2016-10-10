package com.minion.browsing;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minion.persistence.IPage;
import com.minion.persistence.IPageElement;
import com.minion.persistence.IPersistable;
import com.minion.persistence.ITest;
import com.minion.persistence.OrientConnectionFactory;

/**
 * A reference to a web page 
 * 
 * @author Brandon Kindred
 *
 */
public class Page implements IPersistable<IPage> {
    private static final Logger log = LoggerFactory.getLogger(Page.class);

    private String id;
    
    private boolean landable = false;
	private String screenshot = null; 
	private String src = "";
	private URL url;
	
	private List<PageElement> elements;
	
	/**
	 * Creates a page instance that is meant to contain the information found using the driver passed
	 * 
	 * @param driver
	 * @param valid
	 * @throws MalformedURLException 
	 * @throws IOException
	 * @throws URISyntaxException 
	 */
	public Page(WebDriver driver) throws MalformedURLException, IOException{
		log.info("setting source");
		setSrc(driver.getPageSource());

		log.info("Page URL :: "+driver.getCurrentUrl());
		this.url = new URL(driver.getCurrentUrl().replace("/#",""));
		
		log.info("GETTING SCREENSHOT");
		this.screenshot = Browser.getScreenshot(driver);

		log.info("GETTING PAGE SOURCE");
		this.setSrc(driver.getPageSource());
		//Document doc = Jsoup.parse(this.src);
		//this.elements = doc.getAllElements(); 

		log.info("GETTING VISIBLE ELEMENTS");
		this.elements = getVisibleElements(driver, "//body");
		
		log.info("Page object created");
		
	}

	/**
	 * Creates a page instance that is meant to contain the information found using the driver passed
	 * 
	 * @param driver
	 * @param valid
	 * @throws MalformedURLException 
	 * @throws IOException
	 * @throws URISyntaxException 
	 */
	public Page(String pageSource, URL url, String base64Screenshot, List<PageElement> elements) throws MalformedURLException, IOException{
		

		log.info("Page URL :: "+url.toString());
		//driver.getCurrentUrl().replace("/#","")
		this.url = url;
		
		log.info("GETTING SCREENSHOT");
		this.screenshot = base64Screenshot; //Browser.getScreenshot(driver);

		log.info("GETTING PAGE SOURCE");
		this.setSrc(pageSource);
		//Document doc = Jsoup.parse(this.src);
		//this.elements = doc.getAllElements(); 

		log.info("GETTING VISIBLE ELEMENTS");
		this.elements = elements;//getVisibleElements(driver, "//body");
		
		log.info("Page object created");
		
	}
	
	 public String getId() {
		 return id;
	 }

	 public void setId(String id) {
		 this.id = id;
	 }
    
	/**
	 * 
	 * 
	 * @return the page of the source
	 */
	public String getSrc() {
		return this.src;
	}
	
	public void setSrc(String src) {
		this.src = cleanSrc(src);
	}
	
	public List<PageElement> getElements(){
		return this.elements;
	}
	
	public void setElements(List<PageElement> elements){
		this.elements = elements;
	}
	
	/**
	 * Checks if the page is able to be accessed directly as a landing page
	 * 
	 * @return
	 * @throws java.util.NoSuchElementException
	 * @throws UnhandledAlertException
	 * @throws IOException 
	 */
	public boolean checkIfLandable(Browser browser) throws java.util.NoSuchElementException, UnhandledAlertException, IOException{
		browser.getDriver().get(this.getUrl().toString());
		Page current_page = browser.getPage();
		if(current_page.equals(this)){
			landable = true;
		}
		else{
			landable = false;
		}
		return landable;
	}
	
	public boolean isLandable(){
		return this.landable;
	}
	
	private static String cleanSrc(String src){
		//src = src.replaceAll("\\s", "");
		
		src = src.replace("<iframe frameborder=\"0\" id=\"rufous-sandbox\" scrolling=\"no\" allowtransparency=\"true\" allowfullscreen=\"true\" style=\"position: absolute; visibility: hidden; display: none; width: 0px; height: 0px; padding: 0px; border: medium none;\"></iframe>",  "");
		src = src.replace("<canvas id=\"fxdriver-screenshot-canvas\" style=\"display: none;\" width=\"993\" height=\"493\"></canvas>","");
		src = src.replace("<canvas id=\"fxdriver-screenshot-canvas\" style=\"display: none;\" width=\"987\" height=\"491\"></canvas>","");
		src = src.replace("<canvas id=\"fxdriver-screenshot-canvas\" style=\"display: none;\" width=\"1252\" height=\"2284\"></canvas>","");
		src = src.trim();
		return src;
	}
	
	/**
	 * Retreives all elements on a given page that are visible. In this instance we take 
	 *  visible to mean that it is not currently set to {@css display: none} and that it
	 *  is visible within the confines of the screen. If an element is not hidden but is also 
	 *  outside of the bounds of the screen it is assumed hidden
	 *  
	 * @param driver
	 * @return list of webelements that are currently visible on the page
	 */
	public static List<PageElement> getVisibleElements(WebDriver driver, String xpath) 
															 throws WebDriverException {
		
		List<WebElement> pageElements = driver.findElements(By.cssSelector("*"));
		log.info("page elements found :: " +pageElements.size());
		//TO MAKE BETTER TIME ON THIS PIECE IT WOULD BE BETTER TO PARALELLIZE THIS PART
		ArrayList<PageElement> elementList = new ArrayList<PageElement>();
		if(pageElements.size() <= 0){
			return elementList;
		}

		int counter = 0;
		for(WebElement elem : pageElements){
			try{
				log.info("checking visibily and extracting attributes for element " + counter++);
				Date start = new Date();
				if(elem.isDisplayed() && (elem.getAttribute("backface-visibility")==null || !elem.getAttribute("backface-visiblity").equals("hidden"))){
					PageElement pageElem = new PageElement(elem, xpath, ActionFactory.getActions(), new HashMap<String, Integer>(), PageElement.extractedAttributes(elem, (JavascriptExecutor)driver));
					elementList.add(pageElem);
				}
				Date end = new Date();
				
				log.info("All attributes extracted in " + ((end.getTime() - start.getTime())/1000.0) + " seconds");
				
			}catch(StaleElementReferenceException e){
				log.error(e.toString());
			}
		}
		
		
		return elementList;
	}	
		
	/**
	 * Checks if Pages are equal
	 * @param page the {@link Page} object to compare current page to
	 * 
	 * @pre page != null
	 * @return boolean value
	 * 
	 * @NOTE :: TODO :: add in ability to differentiate screenshots
	 */
	@Override
	public boolean equals(Object o){
		if (this == o) return true;
        if (!(o instanceof Page)) return false;
        
        Page that = (Page)o;
        log.info(this.elements.size() + " :: "+ that.elements.size());
        log.info("Do screenshots match? : "+this.screenshot.equals(that.screenshot));
        log.info("sources match? : " +(this.getSrc().length() == that.getSrc().length()));
        
    	log.info("PAGE URLs ARE EQUAL? :: "+this.url+"=="+that.url +" :: ");
    	log.info("urls equal?" + this.url.equals(that.url));

    	log.info("PAGE SRCs ARE EQUAL? :: "+this.getSrc().equals(that.getSrc()));
    	//return (this.getSrc().equals(that.getSrc()) || this.getSrc().length() == that.getSrc().length() || this.screenshot.equals(that.screenshot));
		return (this.elements.size() == that.elements.size() &&
				this.url.equals(that.url) 
				&& this.getSrc().equals(that.getSrc())
				&& this.screenshot.equals(that.screenshot));
				
	}
	
	/**
	 * {@inheritDoc} 	
	 */
	@Override
	public String toString(){
		return this.getSrc();
	}
	
	

	/**
	 * {@inheritDoc}
	 */
	@Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 5 + url.hashCode();
        hash = hash * 17 + src.hashCode();
        hash = hash * 31 + screenshot.hashCode();
        for(PageElement element : elements){
        	hash = hash * 13 + element.hashCode();
        }
        return hash;
    }
	
	/**
	 * Converts Page to IPage for persistence
	 * @param page
	 */
	public IPage convertToRecord(OrientConnectionFactory connection){
		IPage page = connection.getTransaction().addVertex("class:"+IPage.class.getCanonicalName()+","+UUID.randomUUID(), IPage.class);
		page.setLandable(this.isLandable());
		page.setScreenshot(this.getScreenshot());
		page.setSrc(this.getSrc());
		page.setUrl(this.getUrl().toString());
		List<IPageElement> elements = new ArrayList<IPageElement>();
		for(PageElement elem : this.elements){
			IPageElement page_elem_persist = elem.convertToRecord(connection);
			elements.add(page_elem_persist);
		}
		page.setElements(elements);
		page.setKey(this.generateKey());
		return page;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String generateKey() {
		return this.src.hashCode() + "::";
	}

	public URL getUrl(){
		return this.url;
	}
	
	public void setUrl(URL url){
		this.url = url;
	}
	
	public void setLandable(boolean isLandable){
		this.landable = isLandable;
	}
	
	public String getScreenshot(){
		return this.screenshot;
	}
	
	public void setScreenshot(String url){
		this.screenshot = url;
	}
	
	@Override
	public IPersistable<IPage> create() {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		
		this.convertToRecord(orient_connection);
		orient_connection.save();
		
		return this;
	}

	@Override
	public IPersistable<IPage> update(IPage existing_obj) {
		Iterator<IPage> page_iter = this.findByKey(this.generateKey()).iterator();
		int cnt=0;
		while(page_iter.hasNext()){
			page_iter.next();
			cnt++;
		}
		log.info("# of existing records with key "+this.generateKey() + " :: "+cnt);
		
		OrientConnectionFactory connection = new OrientConnectionFactory();
		IPersistable<IPage> page = null;
		if(cnt == 0){
			connection.getTransaction().addVertex("class:"+IPage.class.getCanonicalName()+","+UUID.randomUUID(), IPage.class);
			this.convertToRecord(connection);
		}
		
		connection.save();
		
		return this;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterable<IPage> findByKey(String generated_key) {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		return orient_connection.getTransaction().getVertices("key", generated_key, IPage.class);
	}

	public Iterable<ITest> findByUrl(String pageUrl) {
		// TODO Auto-generated method stub
		return null;
	}
}
