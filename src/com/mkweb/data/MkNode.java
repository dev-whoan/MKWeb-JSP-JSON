package com.mkweb.data;

import java.util.ArrayList;

public class MkNode {
	private Object element;
	private Object parent;
	private ArrayList<MkNode> children;
	
	MkNode(){
		element = null;
		parent = null;
		children = null;
	}
	
	MkNode(Object element){
		this.element = element;
	}
	MkNode(Object element, Object parent){
		this.element = element;
		this.parent = parent;
	}
	MkNode(Object element, Object parent, ArrayList<MkNode> children){
		this.element = element;
		this.parent = parent;
		this.children = children;
	}
	
	public Object getElement() {	return this.element;	}
	public Object getParent() {	return this.parent;	}
	public ArrayList<MkNode> getChildren(){	return this.children;	}
	
	public void setElement(Object element) {	this.element = element;	}
	public void setParent(Object parent) {	this.parent = parent;	}
	public void setChildren(ArrayList<MkNode> children) {	this.children = children;	}
	
	public void clear() {
		this.element = null;
		this.parent = null;
		this.children = null;
	}
}
