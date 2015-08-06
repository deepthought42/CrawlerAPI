package browsing;

/**
 * 
 * @author Brandon Kindred
 *
 */
public class PageAlert {
	private Page page = null;
	private String choice;
	
	/**
	 * 
	 * @param page
	 * @param alertChoice
	 * @pre {"accept","reject"}.contains(alertChoice)
	 */
	public PageAlert(Page page, String alertChoice){
		this.page = page;
		this.choice = alertChoice;
	}
	
	/**
	 * 
	 */
	public void performChoice(){
		if(choice.equals("accept")){
			
		}
		else{
			//reject
		}
	}
	
	public Page getPage(){
		return this.page;
	}
	
	public String getChoice(){
		return this.choice;
	}
}
