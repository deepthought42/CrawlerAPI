import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;



public class DiffHandler {

		public static void main(String[] args){
			
			FirefoxProfile firefoxProfile = new FirefoxProfile();
			
			WebDriver driver = new FirefoxDriver(firefoxProfile);
			String url = "localhost:8080/adminweb/app";
			driver.get(url);
			//WebElement email = driver.findElement(By.id("username"));
			//WebElement password = driver.findElement(By.id("password"));
			//email.sendKeys("root");
			//password.sendKeys("ciscotxbu");
			String pageSrc = driver.getPageSource();
					
			//create list of all possible actions
			Page page = new Page(driver, pageSrc, url, DateFormat.getDateInstance(), false);
			

			//reduce element list to only visible elements
			List<PageElement> visibleLeafElements = page.getVisibleLeafElements(driver);
			System.err.println("VISIBLE ELEMENT LIST SIZE :: " + visibleLeafElements.size());
			List<int[]> elementSequences = new ArrayList<int[]>();
			
			for(int size = 1; size <= visibleLeafElements.size(); size++){
				//array that will contain elementList indices for combinatorics to create all possible element action sequences using any subset of elements available
				int[] elementIndices = new int[size];
				//initilize elementIndices
				for(int i = 0; i < elementIndices.length; i++){
					elementIndices[i] = i;
				}
				do{
					//print(elementIndices);
					//add webElement Array to elementSequences List
					elementSequences.add(elementIndices.clone());
										
					//increment elementIndicies for next value combination
					for(int j = elementIndices.length-1; j >= 0; j--){
						elementIndices[j] = getNextLowestValueNotYetPresent(elementIndices, elementIndices[j], visibleLeafElements.size()-1);
						if(elementIndices[j] < visibleLeafElements.size() && elementIndices[j] != -1){
							break;
						}
						else if(j > 0 && elementIndices[j] == -1){
							getNextHighestCombinatorial(elementIndices, visibleLeafElements.size()-1);
							break;
						}
					}
				}while(elementIndices[0] < visibleLeafElements.size() && elementIndices[0] != -1);
			}
		
			
			page.findActionsThatProduceValidResults(driver, elementSequences);
			
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
