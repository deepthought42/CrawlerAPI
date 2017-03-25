package com.minion.browsing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.qanairy.models.Action;


/**
 * Contains the Order of operations for actions. A lower value indicates that
 *   an action has less precedence.
 *
 */
public class ActionOrderOfOperations {
	public static HashMap<String, Integer> actionOrderOfOperationsMap 
		= new HashMap<String, Integer>();
	
	static List<List<Action>> action_lists = new ArrayList<List<Action>>();
	
	/*
	 * ACTION POLICIES
	 * <<<<<<<<<<<<>>>>>>>>>>>>>>>
	 * 
	 * Mouse over
	 * --------------------
	 * Click
	 * clickAt
	 * click and wait
	 * clickAtAndWait
	 * Click and hold
	 * Release
	 * Double Click
	 * -------------------------
	 * keypresses - all same policy
	 * --------------------
	 * drag and drop
	 * -------------------------
	 */
	static {
		List<Action> mouse_motion_actions = new ArrayList<Action>();
		mouse_motion_actions.add(new Action("mouse_over"));
		
		List<Action> click_actions = new ArrayList<Action>();
		click_actions.add(new Action("clickAndHold"));
		click_actions.add(new Action("click"));
		click_actions.add(new Action("clickAt"));
		//click_actions.add(new Action("clickAndWait"));
		//click_actions.add(new Action("clickAtAndWait"));
		click_actions.add(new Action("release"));
		click_actions.add(new Action("doubleClick"));
		
		List<Action> keyboard_actions = new ArrayList<Action>();
		keyboard_actions.add(new Action("sendKeys"));
		
		
		List<Action> complex_actions = new ArrayList<Action>();
		complex_actions.add(new Action("dragAndDrop"));
		
		action_lists.add(mouse_motion_actions);
		action_lists.add(click_actions);
		action_lists.add(keyboard_actions);
		action_lists.add(complex_actions);
	}
	
	public static Integer getOrderOfOperationForAction(String actionName){
		return actionOrderOfOperationsMap.get(actionName);
	}

	public static List<List<Action>> getActionLists() {
		return action_lists;
	}
}
