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

import learning.State;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

/**
 * A reference to a web page 
 * 
 * @author Brandon Kindred
 *
 */
public class Page implements State, PathObject {
	private String screenshot = null; 
	private String src = "";
	public String date = null;
	public URL pageUrl = null;
	private final List<WebElement> web_elements;
	private final Elements elements;
	
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
		this.src = driver.getPageSource();
		this.date = date.format(new Date());
		this.pageUrl = new URL(driver.getCurrentUrl());
		this.screenshot = Browser.getScreenshot(driver);
		this.web_elements = driver.findElements(By.xpath("//"));
		
		Document doc = Jsoup.parse(this.src);
		this.elements = doc.getAllElements(); 
			//	getVisibleElements(driver, "//body");
	}
	
	/**
	 * 
	 * 
	 * @return the page of the source
	 */
	public String getSrc() {
		this.src = src.replaceAll("\\s", "");
		return src.replace("<canvasid=\"fxdriver-screenshot-canvas\"style=\"display:none;\"width=\"1000\"height=\"720\"></canvas>","");
	}
	
	public void setSrc(String src) {
		this.src = src;
	}

	public URL getUrl(){
		return this.pageUrl;
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
		
		List<WebElement> pageElements = driver.findElements(By.xpath(xpath + "//*"));
		//TO MAKE BETTER TIME ON THIS PIECE IT WOULD BE BETTER TO PARALELLIZE THIS PART
		ArrayList<PageElement> elementList = new ArrayList<PageElement>();
		if(pageElements.size() <= 0){
			return elementList;
		}
		for(WebElement elem : pageElements){
			
			if(elem.isDisplayed() && (elem.getAttribute("backface-visibility")==null || !elem.getAttribute("backface-visiblity").equals("hidden"))){
				PageElement pageElem = new PageElement(driver, elem, xpath, ActionFactory.getActions(), new HashMap<String, Integer>(), PageElement.extractedAttributes(elem, (JavascriptExecutor)driver));
				elementList.add(pageElem);
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

		return (this.elements.size() == that.elements.size()) 
				&& this.pageUrl.equals(that.pageUrl) 
				&& this.getSrc().equals(that.getSrc())
				&& this.screenshot.equals(that.screenshot);
	}
	
	public String toString(){
		return this.getSrc();
	}

	public Object getObject() {
		return this;
	}
	
	@Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 5 + pageUrl.hashCode();
        hash = hash * 17 + src.hashCode();
        hash = hash * 31 + screenshot.hashCode();
       // for(PageElement element : elements){
       // 	hash = hash * 13 + element.hashCode();
       // }
        return hash;
    }

	@Override
	public Page getData() {
		return this;
	}
}
