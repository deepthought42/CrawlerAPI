package browsing;

import org.openqa.selenium.Alert;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.UnsupportedCommandException;
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
	
	public static boolean isAlertPresent(WebDriver driver) throws UnsupportedCommandException 
	{ 
	    try 
	    { 
	        driver.switchTo().alert(); 
	        return true; 
	    }   // try 
	    catch (NoAlertPresentException Ex) 
	    { 
	        return false; 
	    }   // catch 
	} 
	
	/**
	 * 
	 * @param driver
	 * @return
	 */
	public static Alert getAlert(WebDriver driver){
	    try 
	    { 
	        return driver.switchTo().alert(); 
	    }   // try 
	    catch (NoAlertPresentException Ex) 
	    { 
	        return null; 
	    }   // catch 
	}
	
	public String getMessage() throws UnhandledAlertException{
		return this.message; 
	}
	
	public static String getMessage(Alert alert) throws UnhandledAlertException{
		return alert.getText(); 
	}
	
	@Override
	public boolean equals(Object o){
		if (this == o) return true;
        if (!(o instanceof PageAlert)) return false;
        
        PageAlert that = (PageAlert)o;
        
        boolean isEqual = false;
        isEqual = this.message.equals(that.getMessage());
        isEqual = this.page.equals(that.getPage());
        
        return isEqual;        
	}
}
