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
	private String url = "";
	private DateFormat date = null;
	private boolean isValid = false;
	private List<PageElement> elements = null;
	private Page prevPage;
	
	HashMap<WebElement, HashMap<String, Page>> elementActionMap = new HashMap<WebElement, HashMap<String, Page>>();
	HashMap<ElementActionSequence, Page> elementActionSequencenMap = new HashMap<ElementActionSequence, Page>();

	/**
	 * 
	 * 
	 * @param driver
	 * @param src
	 * @param url
	 * @param date
	 * @param valid
	 */
	public Page(WebDriver driver, String src, String url, DateFormat date, boolean valid){
		this.driver = driver;
		this.src = src;
		this.url = url;
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
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
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
		this.elements = this.getVisibleLeafElements(driver);
	}
	
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
				Timing.pauseThread(1);
				
				System.out.print("ACTION :: ");
				DiffHandler.print(actionSeq);
				System.out.print("ELEMENT :: ");
				DiffHandler.print(elementSequence);
				
				
				if(refreshRequired){
					//System.err.println("Refreshing page!");
					try{
						driver.navigate().refresh();
					}catch(Exception e){
						Timing.pauseThread(2);
						driver.navigate().refresh();
					}

					this.setSrc(driver.getPageSource());
					this.refreshElements();
					refreshRequired = false;
				}

				PageElement[] elems = DiffHandler.convertToPageElements(this.elements, elementSequence);

				//perform action sequence for element sequence
				for(int idx = 0; idx < elementSequence.length; idx++){
					try{
						ActionFactory.execAction(driver, elems[idx] , actions[actionSeq[idx]]);
						Timing.pauseThread(1);
						
						Page newPage = new Page(driver, driver.getPageSource(), url, DateFormat.getDateInstance(), false);

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
			System.err.println("CHILD ELEMENT LIST SIZE :: " + childElements.size());
			if(element.isDisplayed() && childElements.isEmpty() && (!element.getAttribute("id").equals("") || (element.getAttribute("name") != null && !element.getAttribute("name").equals("")))){
				visiblePageElements.add(new PageElement(driver, element));
			}
		}
		return visiblePageElements;
	}
	
	/**
	 * retreives all elements on a given page that are visible
	 * @param driver
	 * @return
	 */
	public List<PageElement> getVisibleElements(WebDriver driver){
		List<WebElement> pageElements = driver.findElements(By.cssSelector("*"));

		//reduce element list to only visible elements
		List<PageElement> visiblePageElements = new ArrayList<PageElement>();
		//iterate over every element and grab only those that are currently displayed
		for(WebElement element: pageElements){
			if(element.isDisplayed()){
				visiblePageElements.add(new PageElement(driver, element));
			}
		}
		return visiblePageElements;
	}
	
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
	
	public boolean equals(Page page){
		boolean isEqual = false;
		if(this.getElements().size() == page.getElements().size())
			for(int idx = 0; idx < this.elements.size(); idx++){
				try{
									
					isEqual = this.elements.get(idx).equals(page.elements.get(idx));
					if(!isEqual){
						break;
					}
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		return isEqual;
	}
	
	public String toString(){
		String pageString = "";
		
		pageString += this.url + "\n";
		pageString += this.date + "\n";
		pageString += this.isValid + "\n";
		pageString += this.elements.size() + "\n";
		
		return pageString;
	}
}
