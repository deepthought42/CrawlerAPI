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
	
	@Override
	public boolean equals(Object o){
		if (this == o) return true;
        if (!(o instanceof ElementAction)) return false;
        
        ElementAction that = (ElementAction)o;
        
		if(that.getPageElement().equals(this.element)
				&& that.getAction().equals(this.action)){
			//System.out.print("Testing if ElementActions are equal..");
			//System.out.println("TRUE");
			return true;
		}
		return false;
	}
}
