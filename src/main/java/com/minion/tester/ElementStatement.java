package com.minion.tester;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.qanairy.models.PageElement;

/**
 * Generates statements for finding an element
 *
 */
public class ElementStatement implements IStatementFactory {
	private static Logger log = LogManager.getLogger(ElementStatement.class);

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
