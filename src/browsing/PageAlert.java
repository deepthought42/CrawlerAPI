package browsing;

import org.openqa.selenium.Alert;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.WebDriver;

/**
 * 
 * @author Brandon Kindred
 *
 */
public class PageAlert {
	private Page page = null;
	private String choice;
	private String message;
	
	/**
	 * 
	 * @param page
	 * @param alertChoice
	 * @pre page!=null
	 * @pre alertChoice != null
	 * @pre {"accept","reject"}.contains(alertChoice)
	 * @pre message != null;
	 */
	public PageAlert(Page page, String alertChoice, String message){
		this.page = page;
		this.choice = alertChoice;
		this.message = message;
	}
	
	/**
	 * 
	 * @param driver
	 */
	public void performChoice(WebDriver driver){
		try{
			Alert alert = driver.switchTo().alert();
			if(choice.equals("accept")){
				alert.accept();
			}
			else{
				alert.dismiss();
			}
		}
		catch(NoAlertPresentException nae){
			System.err.println( "Alert not present");
		}
	}
	
	public Page getPage(){
		return this.page;
	}
	
	public String getChoice(){
		return this.choice;
	}
}
