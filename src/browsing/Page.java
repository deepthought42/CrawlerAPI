package browsing;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import util.Timing;


public class Page{
	
	private WebDriver driver = null;
	private String src = "";
	private DateFormat date = null;
	private boolean isValid = false;
	private String pageUrl = "";
	private ArrayList<PageElement> elements = new ArrayList<PageElement>();
	private Page prevPage;
	
	HashMap<PageElement, HashMap<String, Page>> elementActionMap = new HashMap<PageElement, HashMap<String, Page>>();
	HashMap<ElementActionSequence, Page> elementActionSequencenMap = new HashMap<ElementActionSequence, Page>();

	/**
	 * Creates a page instance that is meant to contain the information found using the driver passed
	 * 
	 * @param driver
	 * @param date
	 * @param valid
	 */
	public Page(WebDriver driver, DateFormat date, boolean valid){
		this.driver = driver;
		this.src = driver.getPageSource();

		this.date = date;
		this.isValid = valid;
		this.pageUrl = driver.getCurrentUrl();
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
	public boolean isValid() {
		return isValid;
	}
	public void setValid(boolean valid) {
		this.isValid = valid;
	}
	public Page getPrevPage() {
		return prevPage;
	}
	public void setPrevPage(Page prevPage) {
		this.prevPage = prevPage;
	}
	
	public String getUrl(){
		return this.pageUrl;
	}
	
	public List<PageElement> getElements(){
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
	
	private void addToSequenceMap(ElementActionSequence seq, Page page){
		elementActionSequencenMap.put(seq, page);
		System.out.println(seq.actionSequence + " : ACTION AND ELEMENT SEQUENCE ADDED TO ELEMENT ACTION MAP");
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
			if(elem.isDisplayed()){
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
	 * 
	 * @param elementActionMap
	 * @param currElemActionSeq
	 * @param page
	 * @return
	 */
	public boolean hasElementActionResponseAlreadyBeenEncountered(
				HashMap<ElementActionSequence, Page> elementActionMap, 
				ElementActionSequence currElemActionSeq, 
				Page page)
	{
		int[] currElemSeq = currElemActionSeq.elementSequence;
		int[] currActionSeq = currElemActionSeq.actionSequence;
		boolean exists = false;
		for(ElementActionSequence seq : elementActionMap.keySet()){
			int[] knownElemSeq = seq.elementSequence;
			int[] knownActionSeq = seq.actionSequence;
			int knownLen = knownElemSeq.length-1;


			for(int knownIdx = 0; knownIdx <= knownLen; knownIdx++){
				if(knownElemSeq.length <= currElemSeq.length){
					if(knownActionSeq[knownLen-knownIdx] == currActionSeq[currActionSeq.length-1-knownIdx] 
							&& knownElemSeq[knownLen-knownIdx] == currElemSeq[currElemSeq.length-1-knownIdx]
							&& elementActionMap.get(seq).equals(page)){
						exists = true;
						break;
					}
				}
			}
		}
		return exists;
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
		String pageString = "";
		
		pageString += this.date + "\n";
		pageString += this.isValid + "\n";
		pageString += this.elements.size() + "\n";
		
		return pageString;
	}
}
