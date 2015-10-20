package memory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class DataDefinition {
	private Object object;
	
	/**
	 * 
	 * @param obj
	 */
	public DataDefinition(Object obj) {
	   this.object = obj;
	}
	
	/**
	 * Decomposes object down into data fragments
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public List<ObjectDefinition> decompose() throws IllegalArgumentException, IllegalAccessException, NullPointerException{
		List<ObjectDefinition> objDefList = new ArrayList<ObjectDefinition>();
		Class<?> objClass = this.object.getClass();
	    Field[] fields = objClass.getFields();
	    System.out.println("FIELD COUNT : "+ fields.length);
	    for(Field field : fields) {
	        String name = field.getName();
	        Object value = field.get(this.object);
	        if(value!=null){
	        	
	        	ObjectDefinition objDef = null;
	        			        
		        if(value.getClass().equals(ArrayList.class)){
		        	ArrayList<?> list = ((ArrayList<?>)value);
		        	List<ObjectDefinition> decomposedList = decomposeArrayList(list);
		        	if(decomposedList != null){
		        		objDefList.addAll(decomposedList);
		        	}
		        }
		        else if(value.getClass().equals(String[].class)){
		        	String[] array = (String[]) value;
		        	objDefList.addAll(decomposeStringArray(array));
		        }
		        else{
		        	objDef = new ObjectDefinition(1, value.toString(), field.getType().getCanonicalName().replace(".", "").replace("[","").replace("]",""));
		        	objDefList.add(objDef);
			        //System.out.println("CLASS :: "+value.getClass().getName() + " ;; NAME :: "+field.getName() +"; VALUE :: "+objDef.getValue());
		        }
		       // System.out.println("CLASS :: "+value.getClass().getName() + " ;; NAME :: "+field.getName());

	        }
	    }
	    System.out.println("ELEMENTS DECOMPOSED");
		return objDefList;
	}
	
	/**
	 * Decomposes an array into memory blocks
	 * @param array
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	private List<ObjectDefinition> decomposeStringArray(String[] array) throws IllegalArgumentException, IllegalAccessException{
		if(array == null || array.length == 0){
			return null;
		}
		System.out.println("Array seen.");
    	List<ObjectDefinition> objDefList = new ArrayList<ObjectDefinition>();
    	
    	Class<?> listClass = array[0].getClass();
        System.out.println("LIST CLASS:: "+ listClass);
        for(String object : array){
        	DataDefinition data_def = new DataDefinition(object);
        	objDefList = data_def.decompose();
        }
		return objDefList;
	}

	/**
	 * 
	 * 
	 * @param list
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	private List<ObjectDefinition> decomposeArrayList(ArrayList<?> list) throws IllegalArgumentException, IllegalAccessException, NullPointerException {
		if(list == null || list.isEmpty()){
			return null;
		}
		System.out.println("ArrayList seen.");
    	List<ObjectDefinition> objDefList = new ArrayList<ObjectDefinition>();
    	
    	Class<?> listClass = list.get(0).getClass();
        System.out.println("LIST CLASS:: "+ listClass);
        for(Object object : list){
        	System.out.println("DECOMPOSING LIST OBJECT :: " + object);
        	DataDefinition data_def = new DataDefinition(object);
        	objDefList = data_def.decompose();
        }
		return objDefList;
	}
}
