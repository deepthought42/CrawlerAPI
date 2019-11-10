package com.qanairy.models;


import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import org.openqa.selenium.Alert;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;

/**
 *
 */
public class PageAlert implements PathObject, Persistable {
	private static Logger log = LoggerFactory.getLogger(PageAlert.class);

	public PageState page = null;
	public String choice;
	public String message;
	public String type;
	
	private String key;
	
	/**
	 * 
	 * @param page
	 * @param alertChoice
	 * @pre page!=null
	 * @pre alertChoice != null
	 * @pre {"accept","reject"}.contains(alertChoice)
	 * @pre message != null;
	 */
	public PageAlert(PageState page, String alertChoice, String message){
		this.page = page;
		this.choice = alertChoice;
		this.message = message;
		this.setKey(generateKey());
	}
	
	/**
	 * 
	 * @param driver
	 */
	public void performChoice(WebDriver driver){
		try{
			Alert alert = driver.switchTo().alert();
			if("accept".equals(choice)){
				alert.accept();
			}
			else{
				alert.dismiss();
			}
		}
		catch(NoAlertPresentException nae){
			log.warn( "Alert not present");
		}
	}
	
	public PageState getPage(){
		return this.page;
	}
	
	public String getChoice(){
		return this.choice;
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

	@Override
	public PathObject clone() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getKey() {
		return this.key;
	}

	@Override
	public void setKey(String key) {
		this.key = key;
	}

	@Override
	public String getType() {
		return "PageAlert";
	}

	@Override
	public void setType(String type) {
		this.type = "PageAlert";
	}

	@Override
	public String generateKey() {
		return "alert::"+this.getPage().getKey()+org.apache.commons.codec.digest.DigestUtils.sha256Hex(this.getMessage());
	}
}
