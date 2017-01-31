package com.qanairy.rl.learning;

import java.util.List;

import com.qanairy.rl.memory.ObjectDefinition;
import com.tinkerpop.blueprints.Vertex;

/**
 * Predicts reward for a given Object based on memory entries
 *
 * @param <T> The object type that will have the reward predicted
 */
public interface Predict<T> {
	
	/**
	 * Predicts which object type is most likely to result in a high reward
	 * 
	 * @return The object most likely to result in best reward, or the exploratory choice
	 */
	public T predict(List<Vertex> list);
	
	/**
	 * Returns the predicted reward to be had based on best reward for the object type, or
	 * 	 exploratory choice
	 * 
	 * @param obj
	 * @return
	 */
	public double predict(ObjectDefinition obj);
}
