package com.minion.memory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Defines static methods to handle the decomposition of it's data into their constituent pieces.
 * 
 * @author Brandon Kindred
 *
 */
public class DataDecomposer {
	
	/**
	 * Decomposes object into data fragments
	 * 
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public static List<Object> decomposeObject(Object obj) throws IllegalArgumentException, IllegalAccessException, NullPointerException{
		List<Object> objList = new ArrayList<Object>();
		Class<?> objClass = obj.getClass();
	    Field[] fields = objClass.getFields();
	    
	    for(Field field : fields) {
	        Object value = field.get(obj);
	        if(value!=null){
	        	ObjectDefinition objDef = null;
	        			        
		        if(value.getClass().equals(ArrayList.class)){
		        	ArrayList<?> list = ((ArrayList<?>)value);
		        	//return all elements of array
		        	objList.addAll(list);
		        }
		        else if(value.getClass().equals(String[].class)){
		        	String[] array = (String[]) value;
		        	for(String stringVal : array){
		        		objDef = new ObjectDefinition(stringVal.toString(), stringVal.getClass().getCanonicalName().replace(".", "").replace("[","").replace("]",""));
		        		objList.add(objDef);
		            }
		        }
		        else if(value.getClass().equals(Object[].class)){
		        	Object[] array = (Object[]) value;
		        	for(Object object : array){
		        		objList.add(object);
		            }
		        }
		        else{
	        		objDef = new ObjectDefinition(value.toString(), field.getType().getCanonicalName().replace(".", "").replace("[","").replace("]",""));
		        	objList.add(objDef);
		        }
	        }
	    }
		return objList;
	}
	
	/**
	 * Decomposes object into data fragments
	 * 
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public static List<Object> decompose(Object obj) throws IllegalArgumentException, IllegalAccessException, NullPointerException{
		List<Object> objDefList = new ArrayList<Object>();
		
		Class<?> objClass = obj.getClass();
	    Field[] fields = objClass.getFields();
        //System.out.println("LIST CLASS:: "+ objClass);
	   // System.out.println("FIELD COUNT : "+ fields.length);
	    for(Field field : fields) {
	        Object value = field.get(obj);
	        if(value!=null){
	        	ObjectDefinition objDef = null;
	        			        
		        if(value.getClass().equals(ArrayList.class)){
		        	ArrayList<?> list = ((ArrayList<?>)value);
		        	//return all elements of array
		        	List<Object> decomposedList = decomposeArrayList(list);
	        		objDefList.addAll(decomposedList);
		        }
		        else if(value.getClass().equals(String[].class)){
		        	String[] array = (String[]) value;
		        	for(String stringVal : array){
		        		objDef = new ObjectDefinition(stringVal.toString(), stringVal.getClass().getCanonicalName().replace(".", "").replace("[","").replace("]",""));
		        		objDefList.add(objDef);
		            }
		        }
		        else if(value.getClass().equals(Object[].class)){
		        	Object[] array = (Object[]) value;
		        	List<Object> decomposedList = decomposeObjectArray(array);
		        	objDefList.addAll(decomposedList);
		        }
		        else{
	        		objDef = new ObjectDefinition(value.toString(), field.getType().getCanonicalName().replace(".", "").replace("[","").replace("]",""));
		        	objDefList.add(objDef);
		        }
	        }
	    }
		return objDefList;
	}
	
	/**
	 * Decomposes an array of Objects into memory blocks
	 * @param array
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	private static List<Object> decomposeObjectArray(Object[] array) throws IllegalArgumentException, IllegalAccessException{
    	List<Object> objDefList = new ArrayList<Object>();
		if(array == null || array.length == 0){
			return objDefList;
		}
    	
        for(Object object : array){
        	objDefList.addAll(DataDecomposer.decompose(object));
        }
		return objDefList;
	}

	/**
	 * Iterates over ArrayList of objects, and decomposes each object
	 * 
	 * @param list
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	private static List<Object> decomposeArrayList(ArrayList<?> list) throws IllegalArgumentException, IllegalAccessException, NullPointerException {
    	List<Object> objDefList = new ArrayList<Object>();
		if(list == null || list.isEmpty()){
			return objDefList;
		}
		
        for(Object object : list){
        	if(object != null){
        		objDefList.addAll(DataDecomposer.decompose(object));
        	}
        }
		return objDefList;
	}
}
