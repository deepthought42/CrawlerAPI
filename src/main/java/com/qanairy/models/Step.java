package com.qanairy.models;

/**
 * A set of Steps
 */
public class Step extends LookseeObject {

	private PageState start_page;
	private ElementState element;
	private Action action;
	private PageState end_page;
	
	public Step(PageState start_page, ElementState element, Action action, PageState end_page) {
		assert start_page != null;
		assert element != null;
		assert action != null;
		assert end_page != null;
		
		setStartPage(start_page);
		setElement(element);
		setAction(action);
		setEndPage(end_page);
	}
	
	@Override
	public String generateKey() {
		return start_page.getKey() + element.getKey() + action.getKey() + end_page.getKey();
	}


	public PageState getStartPage() {
		return start_page;
	}


	public void setStartPage(PageState start_page) {
		this.start_page = start_page;
	}


	public ElementState getElement() {
		return element;
	}


	public void setElement(ElementState element) {
		this.element = element;
	}


	public Action getAction() {
		return action;
	}


	public void setAction(Action action) {
		this.action = action;
	}


	public PageState getEndPage() {
		return end_page;
	}


	public void setEndPage(PageState end_page) {
		this.end_page = end_page;
	}

}
