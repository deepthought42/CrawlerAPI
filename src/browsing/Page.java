package browsing;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import learning.State;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

/**
 * A reference to a web page 
 * 
 * @author Brandon Kindred
 *
 */
public class Page implements State {
	public String screenshot = null; 
	public WebDriver driver = null;
	public String src = "";
	//public String date = null;
	public URL pageUrl = null;
	public ArrayList<PageElement> elements = new ArrayList<PageElement>();
	
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
		this.driver = driver;
		this.src = driver.getPageSource();
		//this.date = date.format(new Date());
		this.pageUrl = new URL(driver.getCurrentUrl());
		this.screenshot = Browser.getScreenshot(driver);
		getVisibleElements(driver, this.elements, "//body");
	}
	
	public String getSrc() {
		return src;
	}
	
	public void setSrc(String src) {
		this.src = src;
	}

	public URL getUrl(){
		return this.pageUrl;
	}
	
	/**
	 * Retrieves list of {@link PageElement page elements} for current Page
	 * @return
	 */
	public ArrayList<PageElement> getElements(){
		return this.elements;
	}
	
	public void refreshElements(){
		this.getVisibleElements(driver, this.elements, "//body");
	}
	
	@SuppressWarnings("unused")
	private void printDiffList(List<diff_match_patch.Diff> diffList){
		for(diff_match_patch.Diff item : diffList){
			diff_match_patch.Operation diffOp = item.operation;
			String diffTxt = item.text;
			System.out.println("OPERATION :: " + diffOp);
			System.out.println("TEXT :: " + diffTxt);
			System.out.println("-------------------------------------------");
		}
	}

	/**
	 * retreives all leaf elements on a given page
	 * @param driver
	 * @return
	 */
	public List<PageElement> getVisibleLeafElements(WebDriver driver){
		List<WebElement> pageElements = driver.findElements(By.cssSelector("*"));

		//reduce element list to only visible elements
		List<PageElement> visiblePageElements = new ArrayList<PageElement>();
		for(WebElement element: pageElements){
			List<WebElement> childElements = element.findElements(By.xpath("./*"));
			if(element.isDisplayed() && childElements.isEmpty() && (!element.getAttribute("id").equals("") || (element.getAttribute("name") != null && !element.getAttribute("name").equals("")))){
				visiblePageElements.add(new PageElement(driver, element, this, null));
			}
		}
		return visiblePageElements;
	}
	
	/**
	 * retreives all elements on a given page that are visible. In this instance we take 
	 *  visible to mean that it is not currently set to {@css display: none} and that it
	 *  is visible within the confines of the screen. If an element is not hidden but is also 
	 *  outside of the bounds of the screen it is assumed hidden
	 *  
	 * @param driver
	 * @return list of webelements that are currently visible on the page
	 */
	public void getVisibleElements(WebDriver driver, List<PageElement> pageElementList, String xpath){
		List<WebElement> childElements = getChildElements(xpath);
		//TO MAKE BETTER TIME ON THIS PIECE IT WOULD BE BETTER TO PARALELLIZE THIS PART
		HashMap<String, Integer> xpathHash = new HashMap<String, Integer>();
		String temp_xpath = xpath;
		for(WebElement elem : childElements){
			if(elem.isDisplayed() && (elem.getAttribute("backface-visibility")==null || !elem.getAttribute("backface-visiblity").equals("hidden"))){
				PageElement pageElem = new PageElement(driver, elem, temp_xpath, xpathHash);
				pageElementList.add(pageElem);
				try{
					getVisibleElements(driver, pageElementList, pageElem.getXpath());
				}catch(WebDriverException e){
					//e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Get immediate child elements for a given element
	 * @param elem	WebElement to get children for
	 * @return list of WebElements
	 */
	public List<WebElement> getChildElements(WebElement elem){
		return elem.findElements(By.xpath("./*"));
	}
	
	/**
	 * Get immediate child elements for a given element
	 * @param elem	WebElement to get children for
	 * @return list of WebElements
	 */
	public List<WebElement> getChildElements(String xpath) throws WebDriverException{
		return driver.findElements(By.xpath(xpath+"/*"));
	}
	
	/**
	 * Checks if Pages are equal
	 * @param page the {@link Page} object to compare current page to
	 * 
	 * @pre page != null
	 * @return boolean value
	 */
	@Override
	public boolean equals(Object o){
		if (this == o) return true;
        if (!(o instanceof Page)) return false;
        
        Page that = (Page)o;
		return (this.elements.size() == that.elements.size()) && this.pageUrl.equals(that.pageUrl) && this.screenshot.equals(that.screenshot);
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
        for(PageElement element : elements){
        	hash = hash * 13 + element.hashCode();
        }
        return hash;
    }
}
