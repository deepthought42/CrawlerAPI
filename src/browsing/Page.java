package browsing;
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
	private List<PageElement> elements = null;
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
		this.elements = this.getVisibleElements(driver);
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
	
	public List<PageElement> getElements(){
		return this.elements;
	}
	
	public void refreshElements(){
		this.elements = this.getVisibleElements(driver);
	}
	
	/**
	 * 
	 * @param driver
	 * @param elemSequences
	 */
	public void findActionsThatProduceValidResults(WebDriver driver, List<int[]> elemSequences){
		String[] actions = ActionFactory.getActions();

		System.out.println("------------------------------------------------------------------------------------------");
		boolean refreshRequired = false;
		for(int[] elementSequence : elemSequences){
			int[] actionSeq = new int[elementSequence.length];
			//initialize actions in sequence to the action at index 0;
			for(int idx = 0; idx< actionSeq.length;idx++){
				actionSeq[idx] = 0;
			}
			
			//DO WHILE THERE ARE MORE ELEMENT-ACTION SEQUENCES TO EVALUTATE
			boolean moreActionSequences = true;

			do{
			
				System.out.print("ACTION :: ");
				DiffHandler.print(actionSeq);
				System.out.print("ELEMENT :: ");
				DiffHandler.print(elementSequence);
				
				
				if(refreshRequired){
					//System.err.println("Refreshing page!");
					try{
						driver.navigate().refresh();
					}catch(Exception e){
						Timing.pauseThread(1000);
						driver.navigate().refresh();
					}

					this.setSrc(driver.getPageSource());
					this.refreshElements();
					refreshRequired = false;
				}

				//PageElement[] elems = DiffHandler.convertToPageElements(driver, this.elements, elementSequence);

				//perform action sequence for element sequence
				for(int idx = 0; idx < elementSequence.length; idx++){
					try{
						ActionFactory.execAction(driver, this.elements.get(idx).getElement() , actions[actionSeq[idx]]);
						
						Page newPage = new Page(driver, DateFormat.getDateInstance(), false);

						List<diff_match_patch.Diff> actualDiffList = getDiffList(newPage);
						
						if(actualDiffList.size() > 0){
							System.err.println("Something changed!");
							refreshRequired = true;
							ElementActionSequence seq = new ElementActionSequence(actionSeq, elementSequence);
							if(idx == elementSequence.length-1
									&& !this.hasElementActionResponseAlreadyBeenEncountered(elementActionSequencenMap, seq, newPage)){
								addToSequenceMap(seq, newPage);
							}

							break;
						}
					}
					catch(Exception e){	
						break;
					}
				}
				actionSeq = DiffHandler.generateNextPermutation(actionSeq, actions.length-1);
				
				if(actionSeq[0] >= actions.length){
					moreActionSequences = false;
					refreshRequired = true;
				}

			}while(moreActionSequences);
		}
	}
	
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
	public List<PageElement> getVisibleElements(WebDriver driver){
		//initialize list for visible page elements
		List<PageElement> visiblePageElements = new ArrayList<PageElement>();
		
		//find all immediate children of body element
		WebElement body = driver.findElement(By.xpath("//body"));
		List<WebElement> pageElements = getChildElements(body);
		List<WebElement> childPageElements;

		int i = 1;
		while(!pageElements.isEmpty()){
			childPageElements = new ArrayList<WebElement>();
			//iterate through elements, if element is visible then load in child elements and recurse
			for(WebElement element : pageElements){
				//System.out.println("Finding all elements that are direct children of the " + element.getTagName() +"[id='" + element.getAttribute("id") + "']" + " tag");

				/**
				 * Should go through each element and check for a number of attributes, 
				 * 	ie (display, visiblity, backface-visibility, etc)
				 * 
				 */
				if((element.isDisplayed())){
					childPageElements.addAll(getChildElements(element));
					visiblePageElements.add(new PageElement(this.driver, element));
				}
				i++;
			}
			//clear list and add all newly found child page elements to it
			pageElements.clear();
			pageElements.addAll(childPageElements);
		}
		System.out.println("ALL VISIBLE ELEMENT FOUND! THERE WERE :: " + visiblePageElements.size());
		return visiblePageElements;
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
	 * @param page
	 * @return
	 */
	public boolean equals(Page page){
		boolean isEqual = false;
		if(this.getElements().size() == page.getElements().size()){
			for(int idx = 0; idx < this.elements.size(); idx++){
				try{	
					System.out.println("Checking if elements are equal...");
					isEqual = this.elements.get(idx).equals(page.elements.get(idx));
					if(!isEqual){
						break;
					}
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		return isEqual;
	}
	

	public String toString(){
		String pageString = "";
		
		pageString += this.date + "\n";
		pageString += this.isValid + "\n";
		pageString += this.elements.size() + "\n";
		
		return pageString;
	}
}
