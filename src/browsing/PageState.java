package browsing;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PageState {
	private UUID uuid = null;
	private UUID pageUuid = null;
	private List<PageElement> elementsChanged = null;
	
	/**
	 * Instantiate a new PageState referencing the associated page via UUID
	 * @param pageUuid {@link Page} uuid that this page state is associated with
	 */
	public PageState(UUID pageUuid){
		this.uuid = UUID.randomUUID();
		this.pageUuid = pageUuid;
		this.elementsChanged = new ArrayList<PageElement>();
	}
	
	/**
	 * An element that has changed since last page state. 
	 * 
	 * @param elem
	 * @return if element was added succesfully or not
	 */
	public boolean addChangedPageElement(PageElement elem){
		return this.elementsChanged.add(elem);
	}
	
	public List<PageElement> getElementsChanged(){
		return this.elementsChanged;
	}
	
	public UUID getUuid(){
		return this.uuid;
	}
	
	public UUID getPageUuid(){
		return this.pageUuid;
	}
	
	/**
	 * 
	 * @param Object
	 * @return
	 */
	@Override
	public boolean equals(Object o){
		if (this == o) return true;
        if (!(o instanceof PageState)) return false;
        
        PageState that = (PageState)o;
        
		boolean isEqual = true;
		for(PageElement elem : this.elementsChanged){
			boolean hasMatch = false;
			for(PageElement elem2: that.getElementsChanged()){
				if(elem.equals(elem2)){
					hasMatch = true;
					break;
				}
			}
			if(!hasMatch){
				isEqual = false;
				break;
			}
		}
		System.out.println(Thread.currentThread().getName() + "----Page States are equal!");
		return isEqual;
	}
}
