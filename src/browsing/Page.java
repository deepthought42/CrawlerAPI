package browsing;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.frames.FramedTransactionalGraph;
import com.tinkerpop.frames.Property;

import persistence.IPage;

/**
 * A reference to a web page 
 * 
 * @author Brandon Kindred
 *
 */
public class Page implements PathObject {
    private static final Logger log = Logger.getLogger(Page.class);

    private boolean landable = false;
	private String screenshot = null; 
	private String src = "";
	public String date = null;
	public final URL url;
	
	private final List<PageElement> elements;
	
	/**
	 * Creates a page instance that is meant to contain the information found using the driver passed
	 * 
	 * @param driver
	 * @param date
	 * @param valid
	 * @throws MalformedURLException 
	 * @throws IOException
	 * @throws URISyntaxException 
	 */
	public Page(WebDriver driver, DateFormat date) throws MalformedURLException, IOException{
		setSrc(driver.getPageSource());
		
		this.date = date.format(new Date());
		this.url = new URL(driver.getCurrentUrl().replace("/#","/"));
		this.screenshot = Browser.getScreenshot(driver);
	
		//Document doc = Jsoup.parse(this.src);
		//this.elements = doc.getAllElements(); 
		this.elements = getVisibleElements(driver, "//body");
	}
	
	/**
	 * 
	 * 
	 * @return the page of the source
	 */
    @Property("src")
	public String getSrc() {
		return this.src;
	}
	
	public void setSrc(String src) {
		this.src = cleanSrc(src);
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
	
	@Property("landable")
	public boolean isLandable(){
		return this.landable;
	}
	
	/**
	 * 
	 * @param isLandable
	 */
	 @Property("landable")
	public void setLandable(boolean isLandable){
		this.landable = isLandable;
	}
	
	private static String cleanSrc(String src){
		//src = src.replaceAll("\\s", "");
		
		src = src.replace("<iframe frameborder=\"0\" id=\"rufous-sandbox\" scrolling=\"no\" allowtransparency=\"true\" allowfullscreen=\"true\" style=\"position: absolute; visibility: hidden; display: none; width: 0px; height: 0px; padding: 0px; border: medium none;\"></iframe>",  "");
		src = src.replace("<canvas id=\"fxdriver-screenshot-canvas\" style=\"display: none;\" width=\"993\" height=\"493\"></canvas>","");
		src = src.replace("<canvas id=\"fxdriver-screenshot-canvas\" style=\"display: none;\" width=\"987\" height=\"491\"></canvas>","");
		src = src.trim();
		return src;
	}

    @Property("url")
	public URL getUrl(){
		return this.url;
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
		for(WebElement elem : pageElements){
			try{
				if(elem.isDisplayed() && (elem.getAttribute("backface-visibility")==null || !elem.getAttribute("backface-visiblity").equals("hidden"))){
					PageElement pageElem = new PageElement(driver, elem, xpath, ActionFactory.getActions(), new HashMap<String, Integer>(), PageElement.extractedAttributes(elem, (JavascriptExecutor)driver));
					elementList.add(pageElem);
				}
			}catch(StaleElementReferenceException e){
				log.error(e);
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
        log.info(this.screenshot.equals(that.screenshot));
        log.info(this.getSrc().length() == that.getSrc().length());
    	log.info("PAGE URLs ARE EQUAL? :: "+this.url.equals(that.url));

    	log.info("PAGE SRCs ARE EQUAL? :: "+this.getSrc().equals(that.getSrc()));
    	//return (this.getSrc().equals(that.getSrc()) || this.getSrc().length() == that.getSrc().length() || this.screenshot.equals(that.screenshot));
		return (//this.elements.size() == that.elements.size() &&
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
       // for(PageElement element : elements){
       // 	hash = hash * 13 + element.hashCode();
       // }
        return hash;
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Page data() {
		return this;
	}
	
	/**
	 * Converts Page to IPage for persistence
	 * @param page
	 */
	public IPage convertToRecord(FramedTransactionalGraph<OrientGraph> framedGraph ){
		IPage page = framedGraph.addVertex(UUID.randomUUID(), IPage.class);
		page.setLandable(this.isLandable());
		page.setScreenshot(this.getUrl());
		page.setSrc(this.getSrc());
		page.setUrl(this.getUrl());
		
		return page;
	}
}
