import java.util.ArrayList;
import java.util.HashMap;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.JavascriptExecutor;

public class PageElement {

	private WebElement element;
	private String tagName;
	private String text;
	private ArrayList<Attribute> attributes = new ArrayList<Attribute>();
	
	//map loaded with k,v where k=propertyName, and v=propertyValue
	private HashMap<String, String> cssValues = new HashMap<String,String>();
	
	//transfer list to enum class
	private String[] attributeList = {"id", "class", "name", "style"};
	private String[] cssList = {"backface-visibility", "visible", "display", "position", "color", "font-family", "width", "height", "left", "right", "top", "bottom", "transform"};

	/**
	 * Constructs a PageElement.
	 * @param driver
	 * @param elem
	 */
	public PageElement(WebDriver driver, WebElement elem){
		this.element = elem;
		this.tagName = elem.getTagName();
		this.text    = elem.getText();
		
		loadAttributes();
		loadCssProperties();
	}
	
	public void loadAttributes(){
		for(String attributeString : attributeList){
			//get attribute
			Attribute attr = new Attribute(attributeString, this.element.getAttribute(attributeString));
			this.attributes.add(attr);
		}
	}
	
	
	public void loadCssProperties(){
		for(String propertyName : cssList){
			//get attribute
			if(this.element.getCssValue(propertyName) != null){
				//System.out.println(propertyName + " : " + this.element.getCssValue(propertyName));
				this.cssValues.put(propertyName, this.element.getCssValue(propertyName));	
			}			
		}	
	}

	public WebElement getElement() {
		return this.element;
	}
	
	public String getTagName(){
		return this.tagName;
	}

	public String getText(){
		return this.text;
	}

	public ArrayList<Attribute> getAttributes() {
		return this.attributes;
	}
	
	
	public boolean cssMatches(PageElement elem){
		for(String propertyName : cssValues.keySet()){
			if(!cssValues.get(propertyName).equals(elem.cssValues.get(propertyName))){
				return false;
			}
		}
		System.err.println("ALL PROPERTIES MATCH");

		return true;
	}
	
	
	public boolean equals(PageElement elem){
		ArrayList<Attribute> oldPageElementAttributes = this.getAttributes();
		ArrayList<Attribute> newPageElementAttributes = elem.getAttributes();
		boolean areElementsEqual =  true;
		
		if(!this.getTagName().equals(elem.getTagName())
				|| !this.getText().equals(elem.getText()))
		{
			areElementsEqual = false;
		}
		
		if(areElementsEqual 
				&& oldPageElementAttributes.size() == newPageElementAttributes.size())
		{
			for(int attrIdx = 0; attrIdx < oldPageElementAttributes.size(); attrIdx++)
			{
				//System.err.println("OLD PAGE ATTRIBUTE :: " + oldPageElementAttributes.get(attrIdx).getName() + " -- " + oldPageElementAttributes.get(attrIdx).getVal());
				//System.err.println("NEW PAGE ATTRIBUTE :: " + newPageElementAttributes.get(attrIdx).getName() + " -- " + newPageElementAttributes.get(attrIdx).getVal());
				 areElementsEqual = oldPageElementAttributes.get(attrIdx).equals(newPageElementAttributes.get(attrIdx));
				 if(!areElementsEqual){
					 break;
				 }
			}
		}
		
		this.cssMatches(elem);
		System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++");

		return areElementsEqual;
	}
	
	public String toString(){
		String pageElementString = "";
		
		pageElementString += this.tagName + "\n";
		pageElementString += this.text + "\n";
		
		return pageElementString;
	}
}
