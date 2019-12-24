package com.qanairy.models;


import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.openqa.selenium.Alert;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;

/**
 * Represents an Alert or Confirmation pop-up that is triggered by javascript within the page
 * 
 */
public class PageAlert implements PathObject, Persistable {
	private static Logger log = LoggerFactory.getLogger(PageAlert.class);

	@GeneratedValue
	@Id
	private Long id;

	private String choice;
	private String message;
	private String type;
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
	public PageAlert(String alertChoice, String message){
		this.choice = alertChoice;
		this.message = message;
		this.setType("PageAlert");
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
        
        return this.message.equals(that.getMessage());
   	}

	@Override
	public PageAlert clone() {
		return new PageAlert(this.getChoice(), this.getMessage());
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
		return this.type;
	}

	@Override
	public String generateKey() {
		return "alert::"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(this.getMessage());
	}

	@Override
	public void setType(String type) {
		this.type = "PageAlert";
	}
	
	public Long getId() {
		return this.id;
	}
}
