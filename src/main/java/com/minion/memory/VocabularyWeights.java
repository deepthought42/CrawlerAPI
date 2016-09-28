package com.minion.memory;

import java.util.ArrayList;
import java.util.HashMap;
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
public class VocabularyWeights{

	//private ArrayList<String> valueList = null;
	//private ArrayList<Float> weights = null;
	private HashMap<String, HashMap<String, Float>> vocabulary_weights= null;
	//private ArrayList<ArrayList<Float>> actions = null; 

	private String label = null;
	
	/**
	 * Generates an empty list with the given label as the list name.
	 * 
	 * @param valueList
	 */
	public VocabularyWeights(String listLabel) {
		//this.valueList = new ArrayList<String>();
		this.label = listLabel;
		//this.weights = new ArrayList<Float>();
		this.setVocabulary_weights(new HashMap<String, HashMap<String, Float>>());
	}
	
	/**
	 * A specifically ordered list of values of a certain type specified as the label
	 * 
	 * @param valueList
	 */
	public VocabularyWeights(ArrayList<String> valueList, String label) {
		//this.valueList = valueList;
		this.label = label;
		//this.weights = new ArrayList<Float>(valueList.size());
	}
	
	/**
	 * Appends an object of the vocabulary type to the end of the current vocabulary list if it 
	 *  doesn't yet exist in the valueList
	 * 
	 * @param obj
	 * @return
	 */
	public void appendToVocabulary(String obj){
		if(this.vocabulary_weights.containsKey(obj)){
			return;
		}
		vocabulary_weights.put(obj, new HashMap<String, Float>());
	}
	
	/**
	 * Saves vocabulary to a vertex in a graph Database;
	 */
	public void save(){
		OrientDbPersistor persistor = new OrientDbPersistor();
		Vertex v = persistor.addVertexType(VocabularyWeights.class);
		v.setProperty("vocabulary", this.vocabulary_weights);
		v.setProperty("label", this.label);		
		persistor.save();
	}
	
	/**
	 * Loades vocabulary from a vertex in a graph Database, into a 1 dimensional array;
	 */
	public static VocabularyWeights load(String label){
		OrientDbPersistor persistor = new OrientDbPersistor();
		ArrayList<String> vocabList = new ArrayList<String>();

		Iterator<Vertex> vIter = persistor.findVertices("label", label).iterator();
		if(!vIter.hasNext()){
			return new VocabularyWeights(vocabList, label);
		}
		String vocabulary = vIter.next().getProperty("vocabulary");
		
		String[] vocabArray = vocabulary.split(",");
		for(String word : vocabArray){
			vocabList.add(word);
		}
		
		return new VocabularyWeights(vocabList, label);
	}

	
	public String getLabel(){
		return this.label;
	}

	/**
	 * Appends a weight to the end of the weights list
	 * @param d
	 */
	public void appendToWeights(String key, String action_key, float d) {
		HashMap<String, Float> action_weights = this.vocabulary_weights.get(key);
		if(!action_weights.containsKey(action_key)){
			action_weights.put(action_key, d);
		}
	}

	public HashMap<String, HashMap<String, Float>> getVocabulary_weights() {
		return vocabulary_weights;
	}

	public void setVocabulary_weights(HashMap<String, HashMap<String, Float>> vocabulary_weights) {
		this.vocabulary_weights = vocabulary_weights;
	}
}
