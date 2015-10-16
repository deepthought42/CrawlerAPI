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
	 * 
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public List<ObjectDefinition> decompose() throws IllegalArgumentException, IllegalAccessException{
		List<ObjectDefinition> objDefList = new ArrayList<ObjectDefinition>();
		Class<?> objClass = this.object.getClass();
	    System.out.println("Printing fields...");
	    Field[] fields = objClass.getFields();
	    //System.out.println("FIELD COUNT : "+ fields.length);
	    for(Field field : fields) {
	        String name = field.getName();
	        Object value = field.get(this.object);
	        if(value!=null){
	        	
	        	ObjectDefinition objDef = null;
	        			        
		        if(value.getClass().equals(ArrayList.class)){
		        	ArrayList<?> list = ((ArrayList<?>)value);
		        	List<ObjectDefinition> objList = decomposeArrayList(list);
		        	if(objList == null){
		        		continue;
		        	}
		        	objDefList.addAll(objList);
		        	
		        }
		        else{
		        	objDef = new ObjectDefinition(1, value.toString(), field.getType().getCanonicalName().replace(".", "").replace("[","").replace("]",""));
		        	objDef.setValue(value.toString());
		        	objDefList.add(objDef);
			        //System.out.println("CLASS :: "+value.getClass().getName() + " ;; NAME :: "+field.getName() +"; VALUE :: "+objDef.getValue());
		        }
		       // System.out.println("CLASS :: "+value.getClass().getName() + " ;; NAME :: "+field.getName());

	        }
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
	private List<ObjectDefinition> decomposeArrayList(ArrayList<?> list) throws IllegalArgumentException, IllegalAccessException{
		if(list == null || list.isEmpty()){
			return null;
		}
		System.out.println("ArrayList seen.");
    	ArrayList<ObjectDefinition> objDefList = new ArrayList<ObjectDefinition>();
    	
    	Class<?> listClass = list.get(0).getClass();
    	Field[] listFields = listClass.getFields();
        System.out.println("LIST CLASS:: "+ listClass);
        for(Object object : list){
		    for(Field listField : listFields) {
		    	String fieldName = listField.getName();
		        Object fieldValue = listField.get(object);
		       // System.out.println(fieldName + ": " + fieldValue);
		    }
		    ObjectDefinition objDef = new ObjectDefinition(1, object.toString(), object.getClass().getCanonicalName().replace(".", "").replace("[","").replace("]",""));
        	objDefList.add(objDef);
        }
		return new ArrayList<ObjectDefinition>();
	}
}
