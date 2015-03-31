import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;

import util.Timing;



public class DiffHandler {

		public static void main(String[] args){
			
			FirefoxProfile firefoxProfile = new FirefoxProfile();
			
			WebDriver driver = new FirefoxDriver(firefoxProfile);
			String url = "localhost:3000/ideas";
			driver.get(url);
			//WebElement email = driver.findElement(By.id("username"));
			//WebElement password = driver.findElement(By.id("password"));
			//email.sendKeys("root");
			//password.sendKeys("ciscotxbu");

			String pageSrc = driver.getPageSource();
					
			//create list of all possible actions
			Page page = new Page(driver, pageSrc, url, DateFormat.getDateInstance(), false);

			//CREATE NODE FOR PAGE
			ConcurrentNode<Page> currentPageNode = new ConcurrentNode<Page>(page);

			//FOR CURRENT PAGE CREATE NODES FOR ALL ELEMENTS THAT ARE CURRENTLY VISIBLE. 
			List<PageElement> visibleLeafElements = currentPageNode.data.getVisibleElements(driver);
			System.out.println("VISIBLE LEAF ELEMENTS FOR THIS PAGE :: "+ visibleLeafElements.size());
			for(PageElement elem : visibleLeafElements){
				ConcurrentNode<PageElement> element = new ConcurrentNode<PageElement>(elem);
				currentPageNode.addOutput(element);

				/*for(Attribute attribute : elem.getAttributes()){
					System.out.println("name = '"+attribute.getName()+
						", val = "+ attribute.getVal());
				}
				System.out.println("tag name :: "+element.data.getTagName());
				System.out.println("-------------------------------------------------------");
				*/
				//System.err.println("ELEMENT WEIGHT :: " + currentPageNode.getOutputWeight(element));
			}
			System.out.println("----------------------------------------------------");
			System.err.println("loaded up elements. there were " + currentPageNode.getOutputs().size());
			System.out.println("----------------------------------------------------");
			
			//FOR EACH NODE ADD ALL ACTIONS THAT ARE POSSIBLE FOR AN ELEMENT

			ConcurrentHashMap<ConcurrentNode<?>, Double> outputHash = currentPageNode.getOutputs();
			outputHash.keys();
			Iterator iter = outputHash.entrySet().iterator();
			while(iter.hasNext()){
				Map.Entry pair = (Map.Entry)iter.next();
				System.out.println(pair.getKey() + " = " + pair.getValue());
				ConcurrentNode<PageElement> pageElement = (ConcurrentNode<PageElement>)pair.getKey();
				ArrayList<Attribute> attributeList = pageElement.data.getAttributes();
				
				//ITERATE OVER EACH ACTION IN AN ELEMENT NODE.

				String[] actions = ActionFactory.getActions();
				for(String action : actions){
					ConcurrentNode<String> actionNode = new ConcurrentNode<String>(action);
					pageElement.addOutput(actionNode);
					try{
						ActionFactory.execAction(driver, pageElement.data, actionNode.data);
					}
					catch(StaleElementReferenceException e){
						System.err.println("A SYSTEM ERROR WAS ENCOUNTERED WHILE PERFORMING ACTION : "+
								actionNode.data + ". ");
					}
					System.err.println("ACTION :: " +actionNode.data);
					//IF THE ACTION RESULTS IN ANY SORT OF CHANGE TO THE PAGE(# OF VISIBLE ELEMENTS,
					//			STYLING ON CURRENTLY VISIBLE ELEMENTS, ATTRIBUTES OF CURRENT ELEMENTS)
					
					//Page postActionPage = new Page(driver, pageSrc, url, DateFormat.getDateInstance(), false);
					page.refreshElements();
					List<PageElement> visibleElements = page.getVisibleElements(driver);
						//DID THE NUMBER OF VISIBLE ELEMENTS CHANGE?
					System.out.println("NEW VISIBLE ELEMENT NUMBER :: " + visibleElements.size());
					if(visibleElements.size() != visibleLeafElements.size()){
						actionNode.addOutput(new ConcurrentNode<Page>(page));
						driver.navigate().refresh();
						System.err.println("The number of visible elements has changed. So a new state is coming");
					}
					else{
						//Are the elements the same in both lists
						for(PageElement element : visibleLeafElements){
							if(visibleElements.contains(element)){
								//System.out.println("VISIBLE ELEMENT IN BOTH LISTS");
								visibleElements.remove(element);
							}
							
						}
							//DID ANY OF THE ATTRIBUTES OF THE CURRENTLY VISIBLE ELEMENTS CHANGE?
						
						if(pageElement.data.getAttributes().equals(attributeList)){
							for(Attribute attribute : pageElement.data.getAttributes()){
								int attrIdx = attributeList.indexOf(attribute);
								Attribute attr2 = attributeList.get(attrIdx);
								
								if(!attribute.equals(attr2)){
									System.out.println("ATTRIBUTES DO NOT MATCH");
								}
							}
							System.err.println("Attributes LIST match");
						}
						
						//CHECK FOR CSS MATCHES
						// in order to check for css matches we will need to first
						// find all css values that are attributed to the element at hand
						// for both the current version and the previous version of the element
						else{
							System.err.println("Attributes LIST did not match");
						}
					}
					//DID THE STYLING ON ANY OF THE CURRENTLY VISIBLE ELEMENTS CHANGE?(THIS MIGHT NOT BE NECESSARY IF IT IS INCLUDED IN ATTRIBUTES)

						
				}
		        //iter.remove(); // avoids a ConcurrentModificationException
			}
			
			
			//Get all elements within page
			//go to each element and perform each action

			for(ElementActionSequence elem : page.elementActionSequencenMap.keySet()){
				System.out.print("ELEMENTs :: ");
				print(elem.elementSequence);
				System.out.println("Actions :: ");
				print(elem.actionSequence);
				System.out.println("----------------------------------------------------------------------");
			}

			//create new Page node
			//repeat block until no new pages are found.
			//block end

			
			driver.close();
		}
		
		public void generateMap(WebDriver driver, List<PageElement> elements, String[] actions){
			List<PageElement> reducedElementsList = new ArrayList<PageElement>();
			Collections.copy(reducedElementsList, elements);
			for(PageElement element : elements){
				try{
					ActionFactory.execAction(driver, element, actions[0]);
				}catch(Exception e){}
				reducedElementsList.remove(element);
			}
		}
		
		public static void print(int[] arr){
			String val = "";
			for(int i = 0; i < arr.length; i++){
				val += arr[i] + ", ";
			}
			System.out.println(val);
		}
		
		public static void print(String[] arr){
			String val = "";
			for(int i = 0; i < arr.length; i++){
				val += arr[i] + ", ";
			}
			System.out.println(val);
		}
		
		public static int getNextLowestValueNotYetPresent(int[] arr, int currVal, int maxVal){
			boolean exists = false;
			for(int val = 0; val <= maxVal; val++){
				for(int idx = 0; idx < arr.length; idx++){
					if(arr[idx] == val){
						exists = true;
						break;
					}
				}
				
				if(!exists && val > currVal){
					return val;
				}
				exists = false;
			}

			return -1;
			
		}
		
		public static int[] generateNextPermutation(int[] sequence, int maxVal){
			for(int idx = sequence.length-1; idx >= 0; idx--){
				
				//if current index is equal to maxVal then set to 0 and increment previous
				if(sequence[idx] >= maxVal && idx > 0){
					sequence[idx] = 0;
					sequence[idx-1]++;
					if(sequence[idx-1] <= maxVal){
						break;
					}
				}
				else{
					sequence[idx]++;
					break;
				}
			}
			return sequence;
		}
		
		public static boolean isValuePresent(int[] arr, int maxVal){
			for(int idx = 0; idx < arr.length; idx++){
				if(arr[idx] == maxVal){
					return true;
				}
			}
			return false;
		}
		
		/*
		 * Finds the next highest combinatorial by making multiple passes. All values that are equal
		 * to -1 have overran the maximum allowed value and indicate that the value that appears
		 * in the index to the left needs to be incremented. This continues until an index is found 
		 * who's value doesn't overrun the maximum allowed value. Once this is done then each index
		 * to the right is set to the next lowest possible number available. Combinations do not have 
		 * repeated values
		 */
		public static void getNextHighestCombinatorial(int[] indexes, int maxVal){	
			do{
				int highestIdxIncremented = -1;
				for(int idx = 0; idx < indexes.length ; idx++){
					if(indexes[idx] == -1 && idx>0){
						
						if(idx-1 > highestIdxIncremented){
							indexes[idx-1] = getNextLowestValueNotYetPresent(indexes, indexes[idx-1], maxVal);
						}
						if(indexes[idx-1] == -1){
							highestIdxIncremented = idx-1;
							break;
						}
						highestIdxIncremented = idx;
						indexes[idx] = getNextLowestValueNotYetPresent(indexes, indexes[idx], maxVal);
					}
				}
			}while(isValuePresent(indexes, -1) && indexes[0] != -1);
		}
		
		public static PageElement[] convertToPageElements(List<PageElement> elements, int[] elementSequenceIndices){
			PageElement[] elementSequence = new PageElement[elementSequenceIndices.length];
			for(int idx = 0; idx< elementSequenceIndices.length; idx++){
				elementSequence[idx] = elements.get(elementSequenceIndices[idx]);
			}
			
			return elementSequence;
		}
}
