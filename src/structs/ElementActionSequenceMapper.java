package structs;

import java.util.HashMap;
import java.util.Map;

import browsing.PageElement;
import browsing.actions.Action;


/**
 * Holds elements in a map, that is keyed by element action sequence and content. 
 * 
 * @author Brandon Kindred
 *
 */
public class ElementActionSequenceMapper {
	private Map<String, PageElement> elementActionHash;	
	
	/**
	 * Creates a new instance of the tracker
	 */
	public ElementActionSequenceMapper(){
		elementActionHash = new HashMap<String, PageElement>();
	}
	
	/**
	 * Adds a new entry to the element action path
	 * 
	 * @param elem
	 * @param action
	 */
	public void addElementActionSequence(PageElement elem, Action action){
		String key = generateHash(elem, action);
		if(!this.elementActionHash.containsKey(key)){
			this.elementActionHash.put(key, elem);
		}
	}
	
	/**
	 * Generates a key with the format xpath:::content_hash:::action_name where
	 * the content_hash is a hash of the text within the element.
	 * 
	 * @param elem
	 * @param action
	 * @return
	 */
	private String generateHash(PageElement elem, Action action){
		int content_hash = elem.hashCode();
		String xpath = elem.getXpath();
		String action_str = action.getName();
		
		return xpath+":::"+content_hash+":::"+action_str;
	
	}
	
	/**
	 * Checks if element action sequence exists.
	 * 
	 * @param elem
	 * @param action
	 * @return
	 */
	public boolean containsElementAction(PageElement elem, Action action){
		String hash_key = generateHash(elem, action);
		return  this.elementActionHash.containsKey(hash_key);
	}
	
	/** 
	 * @return hash of element action sequences
	 */
	public Map<String, PageElement> getElementActionHash(){
		return elementActionHash;
	}
}
