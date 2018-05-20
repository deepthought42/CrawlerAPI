package com.minion.browsing;

import java.util.ArrayList;
import java.util.List;

public class PageElementNode<T> {
	private List<PageElementNode<T>> children = new ArrayList<PageElementNode<T>>();
	private PageElementNode<T> parent = null;
	private T data = null;

	public PageElementNode(T data) {
        this.data = data;
    }

    public PageElementNode(T data, PageElementNode<T> parent) {
        this.data = data;
        this.parent = parent;
    }

    public List<PageElementNode<T>> getChildren() {
        return children;
    }

    public void setParent(PageElementNode<T> parent) {
        parent.addChild(this);
        this.parent = parent;
    }

    public void addChild(T data) {
        PageElementNode<T> child = new PageElementNode<T>(data);
        child.setParent(this);
        this.children.add(child);
    }

    public void addChild(PageElementNode<T> child) {
        child.setParent(this);
        this.children.add(child);
    }

    public T getData() {
        return this.data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public boolean isRoot() {
        return (this.parent == null);
    }

    public boolean isLeaf() {
        if(this.children.size() == 0) 
            return true;
        else 
            return false;
    }

    public void removeParent() {
        this.parent = null;
    }
}
