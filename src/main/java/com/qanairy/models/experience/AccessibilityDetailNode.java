package com.qanairy.models.experience;

import com.qanairy.models.enums.AuditType;

/**
 * Defines an accessibility audit detail record. 
 */
public class AccessibilityDetailNode extends AuditDetail {

	private String node_label;
	private String explanation;
	private String selector;
	private String path;
	private String snippet;
	
	/**
	 * 
	 * @param node_label
	 * @param explanation
	 * @param type
	 * @param selector
	 * @param path
	 * @param snippet
	 */
	public AccessibilityDetailNode(
			String node_label, 
			String explanation, 
			String type, 
			String selector, 
			String path,
			String snippet) {
		setNodeLabel(node_label);
		setExplanation(explanation);
		setType(AuditType.create(type));
		setSelector(selector);
		setPath(path);
		setSnippet(snippet);
	}

	/** GETTERS AND SETTERS */
	public String getNodeLabel() {
		return node_label;
	}
	public void setNodeLabel(String node_label) {
		this.node_label = node_label;
	}
	public String getExplanation() {
		return explanation;
	}
	public void setExplanation(String explanation) {
		this.explanation = explanation;
	}
	public String getSelector() {
		return selector;
	}
	public void setSelector(String selector) {
		this.selector = selector;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public String getSnippet() {
		return snippet;
	}
	public void setSnippet(String snippet) {
		this.snippet = snippet;
	}
	
}
