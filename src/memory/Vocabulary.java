package memory;

import java.util.ArrayList;

/**
 * A list of unique objects of a designated type that are stored and loaded in a specific order as
 *  a way of maintaining the ability to grow a vertex of features that are always in the exact same
 *  order from run to run.
 * 
 * @author Brandon Kindred
 *
 * @param <E>
 */
public class Vocabulary<E> {

	private ArrayList<E> valueList = new ArrayList<E>();
	private String label = null;
	private String filePath = null;
	
	/**
	 * A specifically ordered list of values of a certain type specified as the label
	 * 
	 * @param valueList
	 */
	public Vocabulary(ArrayList<E> valueList, String label) {
		this.valueList = valueList;
		this.label = label;
		this.filePath = "./records/vocabularies/"+label+".txt";
	}
	
	/**
	 * Appends an object of the vocabulary type to the end of the current vocabulary list
	 * 
	 * @param obj
	 * @return
	 */
	public boolean appendToVocabulary(E obj){
		return valueList.add(obj);
	}

	public String getFilePath(){
		return this.filePath;
	}
	
	public String getLabel(){
		return this.label;
	}
}
