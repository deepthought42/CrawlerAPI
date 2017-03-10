package com.minion.browsing;

import java.util.Iterator;
import java.util.Random;

import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import com.qanairy.persistence.OrientDbPersistor;
import com.qanairy.rl.memory.ObjectDefinition;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

/**
 * 
 * @author Brandon Kindred
 *
 */
public class ActionFactory {
	private static String[] actions = {"click",
								"doubleClick",
								"mouseover"};/*,
								"scroll"};
								"sendKeys"};*/
	private static Actions builder;
	
	public ActionFactory(WebDriver driver){
		builder = new Actions(driver);
	}

	/**
	 * 
	 * @param driver
	 * @param elem
	 * @param action
	 */
	public void execAction(WebElement elem, String input, String action) throws WebDriverException{
		if(action.equals("click")){
			builder.click(elem);
		}
		else if(action.equals("clickAndHold")){
			builder.clickAndHold(elem);
		}
		//Context click clicks select/options box
		else if(action.equals("contextClick")){
			builder.contextClick(elem);
		}
		else if(action.equals("doubleClick")){
			builder.doubleClick(elem);
		}
		else if(action.equals("dragAndDrop")){
			//builder.dragAndDrop(source, target);
		}
		else if(action.equals("keyDown")){
			//builder.keyDown();
		}
		else if(action.equals("keyUp")){
			//builder.keyUp(theKey);
		}
		else if(action.equals("release")){
			builder.release(elem);
		}
		else if(action.equals("sendKeys")){
			builder.sendKeys(elem, Keys.chord(Keys.CONTROL, input, Keys.DELETE));
			builder.sendKeys(elem, "Some src Val");
		}
		else if(action.equals("mouseover")){
			builder.moveToElement(elem);
		}
		builder.perform();
	}
	
	
	
	/**
	 * The list of actions possible
	 * @return
	 */
	public static String[] getActions(){
		return actions;
	}

	/**
	 * Predicts best action based on disparate action information
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	public static int predict(ObjectDefinition obj) throws IllegalArgumentException, IllegalAccessException {
		double[] action_weight = new double[actions.length];
		Random rand = new Random();

		//COMPUTE ALL EDGE PROBABILITIES
		for(int index = 0; index < actions.length; index++){
			OrientDbPersistor orientPersistor = new OrientDbPersistor();
			Iterator<Vertex> vertices = orientPersistor.findVertices(obj).iterator();
			if(!vertices.hasNext()){
				return rand.nextInt(actions.length);
			}
			Vertex vertex = vertices.next();

			Iterable<Edge> edges = vertex.getEdges(Direction.OUT, actions[index]);
			if(edges.iterator().hasNext()){
				for(Edge edge : edges){
					if(edge.getLabel().isEmpty()){
						//guess the label
					}
					else{
						String label = edge.getLabel();
						int action_count = edge.getProperty("count");
						int probability = edge.getProperty("probability");
						System.out.println("Label :: "+label+" ; count :: "+ action_count + " ; P() :: " + probability + "%");	
					}
				}
			}
			else{
				System.out.println("+++   No edges found. Setting weight randomly ++");
				action_weight[index] = rand.nextDouble();
			}
		}
		
		//Call predict method and get anticipated reward for given action against all datums
		//	-- method needed for prediction 
		//START PREDICT METHOD
			
		//Flip a coin to determine whether we should exploit/optimize or explore
		double coin = rand.nextDouble();
		if(coin > .5){
			//Get Best action_weight prediction
			double max = -1.0;
			int maxIdx = 0;
		    for(int j = 0; j < action_weight.length; j++){
		    	if(action_weight[j] > max){
		    		System.err.println("MAX WEIGHT FOR NOW :: "+max);
		    		max=action_weight[j];
		    		maxIdx = j;
		    	}
		    }
		    
		    System.out.println("-----------    max computed action is ....." + actions[maxIdx]);
		    return maxIdx;
		}
		else{
			System.err.println("Coin was flipped and exploration was chosen. OH MY GOD I HAVE NO IDEA WHAT TO DO!");
			return 1;
		}
		//END PREDICT METHOD
	}
}
