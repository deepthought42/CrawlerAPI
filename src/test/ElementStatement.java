package test;

import browsing.PageElement;

/**
 * Generates statements for finding an element
 * @author Brandon Kindred
 *
 */
public class ElementStatement implements IStatementFactory {

	/**
	 * {@inheritDoc}
	 */
	public String generateStatement(Object o) {
		if(o instanceof PageElement){
			PageElement element = (PageElement)o;
			System.out.println("I SHOULD BE GENERATING AN ELEMENT TEST...");
			return "document.evaluate("+ element.getXpath() +", document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null ).singleNodeValue";
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public String generateStatement(Object[] o) {
		// TODO Auto-generated method stub
		return null;
	}


}
