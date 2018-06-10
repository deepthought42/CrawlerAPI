package com.minion.tester;

import org.slf4j.LoggerFactory;

import com.qanairy.models.PageElement;

import org.slf4j.Logger;


/**
 * Generates statements for finding an element
 *
 */
public class ElementStatement implements IStatementFactory {
	private static Logger log = LoggerFactory.getLogger(ElementStatement.class);

	/**
	 * {@inheritDoc}
	 */
	public String generateStatement(Object o) {
		if(o instanceof PageElement){
			PageElement element = (PageElement)o;
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
