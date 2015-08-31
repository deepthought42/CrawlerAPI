package learning;

import java.util.ArrayList;

import browsing.PageElement;

/**
 * Intended to hold the knowledge matrix for ElementAction combinations.
 * 
 * @Note: this should be made more generic. THE FOLLOWING METHOD IS ONLY PROOF OF CONCEPT.----------
 * @author Brandon Kindred
 *
 */
public class ElementActionValueMatrix {

	private double[][] matrix = null;
	private ArrayList<PageElement> elements = null;
	private ArrayList<String> actions = null;
	
	/**
	 * 
	 * @param x rows
	 * @param y	columns
	 */
	public ElementActionValueMatrix(ArrayList<PageElement> elements, ArrayList<String> actions) {
		this.setElements(elements);
		this.setActions(actions);
		setMatrix(new double[elements.size()][actions.size()]);
	}

	public double[][] getMatrix() {
		return matrix;
	}

	public void setMatrix(double[][] matrix) {
		this.matrix = matrix;
	}

	public ArrayList<PageElement> getElements() {
		return elements;
	}

	public void setElements(ArrayList<PageElement> elements) {
		this.elements = elements;
	}

	public ArrayList<String> getActions() {
		return actions;
	}

	public void setActions(ArrayList<String> actions) {
		this.actions = actions;
	}
}
