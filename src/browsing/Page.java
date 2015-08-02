package browsing;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * 
 * @author Brandon Kindred
 *
 */
public class Page{
	private UUID uuid = null;	
	private WebDriver driver = null;
	private String src = "";
	private DateFormat date = null;
	private URL pageUrl = null;
	private ArrayList<PageElement> elements = new ArrayList<PageElement>();
	private Page prevPage;
	
	HashMap<PageElement, HashMap<String, Page>> elementActionMap = new HashMap<PageElement, HashMap<String, Page>>();

	/**
	 * Creates a page instance that is meant to contain the information found using the driver passed
	 * 
	 * @param driver
	 * @param date
	 * @param valid
	 * @throws MalformedURLException 
	 * @throws URISyntaxException 
	 */
	public Page(WebDriver driver, DateFormat date) throws MalformedURLException{
		this.uuid = UUID.randomUUID();
		this.driver = driver;
		this.src = driver.getPageSource();

		this.date = date;
		this.pageUrl = new URL(driver.getCurrentUrl());
		getVisibleElements(driver, this.elements, "//body");
	}
	
	public String getSrc() {
		return src;
	}
	
	public void setSrc(String src) {
		this.src = src;
	}

	public DateFormat getDate() {
		return date;
	}
	
	public void setDate(DateFormat date) {
		this.date = date;
	}
		
	public Page getPrevPage() {
		return prevPage;
	}
	
	public void setPrevPage(Page prevPage) {
		this.prevPage = prevPage;
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
	
	/**
	 * 
	 * @param page
	 * @return
	 */
	private List<diff_match_patch.Diff> getDiffList(Page page){
		diff_match_patch diff = new diff_match_patch();
		List<diff_match_patch.Diff> diffList = diff.diff_main(this.getSrc(), page.getSrc());
		List<diff_match_patch.Diff> actualDiffList = new ArrayList<diff_match_patch.Diff>();
		for(diff_match_patch.Diff diffItem : diffList){
			diff_match_patch.Operation diffOp = diffItem.operation;
			String diffTxt = diffItem.text.trim();
			if(diffOp.compareTo(diff_match_patch.Operation.EQUAL) != 0 && !(diffOp.compareTo(diff_match_patch.Operation.INSERT) == 0 && (diffTxt.equals("style=\"\"") || diffTxt.equals("\" style=\"") || diffTxt.equals(";") || diffTxt.trim().isEmpty()))){
				actualDiffList.add(diffItem);
			}
		}
		return actualDiffList;
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
				visiblePageElements.add(new PageElement(driver, element));
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
		//System.out.println("CURRENT XPATH AT START OF VISIBLE ELEMENT OP :::: "+xpath);
		List<WebElement> pageElements = getChildElements(xpath);
		//System.out.println("THERE ARE "+pageElements.size() + " CHILD ELEMENTS FOUND");
		HashMap<String, Integer> xpathHash = new HashMap<String, Integer>();
		String temp_xpath = xpath;
		for(WebElement elem : pageElements){
			if(elem.isDisplayed() && (elem.getAttribute("backface-visibility")==null || !elem.getAttribute("backface-visiblity").equals("hidden"))){
				PageElement pageElem = new PageElement(driver, elem, temp_xpath, xpathHash);
				pageElementList.add(pageElem);
				//System.out.println("Retrieving visible elements for element with xpath ---- "+pageElem.getXpath());
				getVisibleElements(driver, pageElementList, pageElem.getXpath());
			}
		}
	}
	
	/**
	 * Get immediate child elements for a given element
	 * @param elem	WebElement to get children for
	 * @return list of WebElements
	 */
	public List<WebElement> getChildElements(WebElement elem){
		return elem.findElements(By.xpath("*"));
	}
	
	/**
	 * Get immediate child elements for a given element
	 * @param elem	WebElement to get children for
	 * @return list of WebElements
	 */
	public List<WebElement> getChildElements(String xpath){
		return driver.findElement(By.xpath(xpath)).findElements(By.xpath("*"));
	}
	
	/**
	 * Checks if Pages are equal
	 * @param page the {@link Page} object to compare current page to
	 * 
	 * @pre page != null
	 * @return boolean value
	 */
	public boolean equals(Page page){
		return this.src.equals(page.src);
	}
	

	public String toString(){
		String pageString = "\n";
		
		pageString += "\tDATE :: "+ this.date + "\n";
		pageString += "\tELEMENT COUNT :: " +this.elements.size() + "\n";
		
		return pageString;
	}
	
	public UUID getUuid(){
		return this.uuid;
	}
}
