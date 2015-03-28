import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.JavascriptExecutor;

public class PageElement {

	private WebElement element;
	private String tagName;
	private String text;
	private ArrayList<Attribute> attributes = new ArrayList<Attribute>();
	//transfer list to enum class
	private String[] attributeList = {"id", "class", "name", "style"};
	
	public PageElement(WebDriver driver, WebElement elem){
		this.element = elem;
		this.tagName = elem.getTagName();
		this.text    = elem.getText();

		for(String attributeString : attributeList){
			//get attribute
			Attribute attr = new Attribute(attributeString, this.element.getAttribute(attributeString));
			this.attributes.add(attr);
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
