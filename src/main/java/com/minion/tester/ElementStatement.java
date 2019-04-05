package com.minion.tester;

import org.slf4j.LoggerFactory;

import com.qanairy.models.PageElementState;

import org.slf4j.Logger;


/**
 * Generates statements for finding an element
 *
 */
public class ElementStatement implements IStatementFactory {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(ElementStatement.class);

	/**
	 * {@inheritDoc}
	 */
	public String generateStatement(Object o) {
		if(o instanceof PageElementState){
			PageElementState element = (PageElementState)o;
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
