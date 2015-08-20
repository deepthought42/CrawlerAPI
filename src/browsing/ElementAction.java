package browsing;

/**
 * Represents the state of performing an action on a {@link PageElement element}. 
 * 
 * @author Brandon Kindred
 *
 */
public class ElementAction {
	private PageElement element;
	private String action;
	private int elementIndex;
	
	public ElementAction(PageElement elem, String action, int elemIdx){
		this.element = elem;
		this.elementIndex = elemIdx;
		this.action = action;
	}
	
	public PageElement getPageElement(){
		return this.element;
	}
	
	public String getAction(){
		return this.action;
	}
	
	public int getElementIndex(){
		return this.elementIndex;
	}
	
	public boolean equals(ElementAction elemAction){
		System.out.print("Testing if ElementActions are equal..");
		if(elemAction.getPageElement().equals(this.element)
				&& elemAction.getAction().equals(this.action)){
			System.out.println("TRUE");
			return true;
		}
		System.out.println("FALSE");
		return false;
	}
}
