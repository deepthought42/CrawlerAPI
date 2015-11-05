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
	 * Decomposes object into data fragments
	 * 
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public List<ObjectDefinition> decompose() throws IllegalArgumentException, IllegalAccessException, NullPointerException{
		List<ObjectDefinition> objDefList = new ArrayList<ObjectDefinition>();
		Class<?> objClass = this.object.getClass();
	    Field[] fields = objClass.getFields();
        //System.out.println("LIST CLASS:: "+ objClass);
	   // System.out.println("FIELD COUNT : "+ fields.length);
	    for(Field field : fields) {
	        Object value = field.get(this.object);
	        if(value!=null){
	        	ObjectDefinition objDef = null;
	        			        
		        if(value.getClass().equals(ArrayList.class)){
		        	ArrayList<?> list = ((ArrayList<?>)value);

		        	List<ObjectDefinition> decomposedList = decomposeArrayList(list);
	        		objDefList.addAll(decomposedList);
		        }
		        else if(value.getClass().equals(String[].class)){
		        	String[] array = (String[]) value;
		        	for(String stringVal : array){
		        		objDef = new ObjectDefinition(1, stringVal.toString(), stringVal.getClass().getCanonicalName().replace(".", "").replace("[","").replace("]",""));
		        		objDefList.add(objDef);
		            }
		        }
		        else if(value.getClass().equals(Object[].class)){
		        	Object[] array = (Object[]) value;
		        	List<ObjectDefinition> decomposedList = decomposeObjectArray(array);
		        	objDefList.addAll(decomposedList);
		        }
		        else{
	        		objDef = new ObjectDefinition(1, value.toString(), field.getType().getCanonicalName().replace(".", "").replace("[","").replace("]",""));
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
	private List<ObjectDefinition> decomposeObjectArray(Object[] array) throws IllegalArgumentException, IllegalAccessException{
    	List<ObjectDefinition> objDefList = new ArrayList<ObjectDefinition>();
		if(array == null || array.length == 0){
			return objDefList;
		}
    	
        for(Object object : array){
        	DataDefinition data_def = new DataDefinition(object);
        	objDefList.addAll(data_def.decompose());
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
	private List<ObjectDefinition> decomposeArrayList(ArrayList<?> list) throws IllegalArgumentException, IllegalAccessException, NullPointerException {
    	List<ObjectDefinition> objDefList = new ArrayList<ObjectDefinition>();
		if(list == null || list.isEmpty()){
			return objDefList;
		}
		
        for(Object object : list){
        	if(object != null){
        		DataDefinition data_def = new DataDefinition(object);
        		objDefList.addAll(data_def.decompose());
        	}
        }
		return objDefList;
	}
}
