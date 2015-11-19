package memory;

import java.util.ArrayList;

import com.tinkerpop.blueprints.Vertex;

/**
 * A list of unique objects of a designated type that are stored and loaded in a specific order as
 *  a way of maintaining the ability to grow a vertex of features that are always in the exact same
 *  order from run to run.
 * 
 * @author Brandon Kindred
 *
 * @param <E>
 */
public class Vocabulary{

	private ArrayList<String> valueList = new ArrayList<String>();
	private String label = null;
	
	/**
	 * A specifically ordered list of values of a certain type specified as the label
	 * 
	 * @param valueList
	 */
	public Vocabulary(ArrayList<String> valueList, String label) {
		this.valueList = valueList;
		this.label = label;
	}
	
	/**
	 * Appends an object of the vocabulary type to the end of the current vocabulary list
	 * 
	 * @param obj
	 * @return
	 */
	public boolean appendToVocabulary(String obj){
		return valueList.add(obj);
	}
	
	public String getLabel(){
		return this.label;
	}
	
	/**
	 * Saves vocabulary to a vertex in a graph Database;
	 */
	public void save(){
		Persistor persistor = new Persistor();
		Vertex v = persistor.addVertex(Vocabulary.class.getCanonicalName());
		v.setProperty("vocabulary", this.valueList);
		v.setProperty("label", this.label);		
		persistor.save();
	}
}
