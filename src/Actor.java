import org.openqa.selenium.WebElement;


public class Actor implements Runnable {

	private String url = null;
	private WebElement element = null;
	private String action = null;
	
	public Actor(String url, WebElement element, String action) {
		this.url = url;
		this.element = element;
		this.action = action;
	}

	public void run() {
		//get a web browser driver and open the browser to the desired url
		//get the page
		//find the element
		//perform an action

	}

}
