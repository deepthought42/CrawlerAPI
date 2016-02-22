package memory;

import java.util.ArrayList;
import java.util.Iterator;

import com.tinkerpop.blueprints.Vertex;

/**
 * A list of unique objects of a designated type that are stored and loaded in a specific order as
 *  a way of maintaining the ability to grow a vertex of features that are always in the exact same
 *  order from run to run.
 * 
 * @author Brandon Kindred
 *
 */
public class Vocabulary{

	private ArrayList<String> valueList = null;
	private ArrayList<Float> weights = null;
	//private ArrayList<ArrayList<Float>> actions = null; 

	private String label = null;
	
	/**
	 * Generates an empty list with the given label as the list name.
	 * 
	 * @param valueList
	 */
	public Vocabulary(String listLabel) {
		this.valueList = new ArrayList<String>();
		this.label = listLabel;
		this.weights = new ArrayList<Float>();
	}
	
	/**
	 * A specifically ordered list of values of a certain type specified as the label
	 * 
	 * @param valueList
	 */
	public Vocabulary(ArrayList<String> valueList, String label) {
		this.valueList = valueList;
		this.label = label;
		this.weights = new ArrayList<Float>(valueList.size());
	}
	
	/**
	 * Appends an object of the vocabulary type to the end of the current vocabulary list if it 
	 *  doesn't yet exist in the valueList
	 * 
	 * @param obj
	 * @return
	 */
	public boolean appendToVocabulary(String obj){
		if(this.valueList.contains(obj)){
			return false;
		}
		return valueList.add(obj);
	}
	
	/**
	 * Saves vocabulary to a vertex in a graph Database;
	 */
	public void save(){
		OrientDbPersistor<Vocabulary> persistor = new OrientDbPersistor<Vocabulary>();
		Vertex v = persistor.addVertexType(Vocabulary.class.getCanonicalName());
		v.setProperty("vocabulary", this.valueList);
		v.setProperty("label", this.label);		
		persistor.save();
	}
	
	/**
	 * Loades vocabulary from a vertex in a graph Database, into a 1 dimensional array;
	 */
	public static Vocabulary load(String label){
		OrientDbPersistor<Vocabulary> persistor = new OrientDbPersistor<Vocabulary>();
		ArrayList<String> vocabList = new ArrayList<String>();

		Iterator<Vertex> vIter = persistor.find("label", label).iterator();
		if(!vIter.hasNext()){
			return new Vocabulary(vocabList, label);
		}
		String vocabulary = vIter.next().getProperty("vocabulary");
		
		String[] vocabArray = vocabulary.split(",");
		for(String word : vocabArray){
			vocabList.add(word);
		}
		
		return new Vocabulary(vocabList, label);
	}

	public ArrayList<String> getValueList() {
		return this.valueList;
	}
	
	public String getLabel(){
		return this.label;
	}

	public ArrayList<Float> getWeights(){
		return this.weights;
	}
	
	/**
	 * Appends a weight to the end of the weights list
	 * @param d
	 */
	public void appendToWeights(float d) {
		this.weights.add(d);
	}
}
