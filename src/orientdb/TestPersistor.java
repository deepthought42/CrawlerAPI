package orientdb;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.util.ArrayList;

import browsing.Browser;
import browsing.Page;
import browsing.PageElement;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;


public class TestPersistor {

	public static void main(String[] args) {
		Persistor persistor = new Persistor();
		
		OrientGraph graph = persistor.getGraph();
		Vertex from_vertex = persistor.addVertex();
		Vertex to_vertex = persistor.addVertex();
		Edge edge = persistor.addEdge(from_vertex, to_vertex);
		
        try {
            Browser browser = new Browser("localhost:3000");
			Page page = new Page(browser.getDriver(), null);
			printFields(page);

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		createPersons(graph);
        displayAllVertices(getAllVertices(graph, "Person"), "name");


	}
	
	public static void printFields(Object obj) throws Exception {
	    Class<?> objClass = obj.getClass();
	    System.out.println("Printing fields...");
	    Field[] fields = objClass.getFields();
	    System.out.println("FIELD COUNT : "+ fields.length);
	    for(Field field : fields) {
	        String name = field.getName();
	        Object value = field.get(obj);
	        if(value!=null){
	        //System.out.print("VALUE :: " + value);
	        System.out.println("; CLASS :: "+value.getClass().getName());
	        
	        if(value.getClass().equals(ArrayList.class)){
	        	System.out.println("ArrayList seen.");
	        	ArrayList<?> list = ((ArrayList)value);
	        	Class<?> listClass = list.get(0).getClass();
	        	Field[] listFields = listClass.getFields();
		        System.out.println("LIST CLASS:: "+ listClass);
	    	    for(Field listField : listFields) {
	    	    	String fieldName = listField.getName();
	    	        Object fieldValue = listField.get(list.get(0));
    		        System.out.println(fieldName + ": " + fieldValue);

	    	    }
	        	if(((ArrayList)value).get(0).getClass().equals(PageElement.class)){
	        		System.out.println("PageElement seen.");
	        	}
	        }
	        }
	       //System.out.println(name + ": " + value);
	    }
	}
	
	private static void createPersons(OrientGraph graph) {
        String [] persons = {"John", "Jack", "Ryan"};

        if (graph.getVertexType("Person") == null){
            graph.createVertexType("Person");
            System.out.println("Created person vertex type");
        }

        for (int i = 0; i < persons.length; i++) {
            try {
                Vertex v = graph.addVertex("class:Person");
                v.setProperty("name", persons[i]);
                graph.commit();
            }
            catch (Exception e) {
                graph.rollback();
                System.out.println("Error while creating the persons. Had to roll back");
            }
        }       
        System.out.println("Done creating vertices...");
    }
	
	private static Iterable<Vertex> getAllVertices(OrientGraph graph, String classname) {
        return graph.getVerticesOfClass(classname);
    }
	
	private static void displayAllVertices(Iterable<Vertex> it, String propertyName) {
        System.out.println("The vertices in the graph are:");
        for (Vertex v: it)
            System.out.println(v + " " + v.getProperty("name"));
    }
}
