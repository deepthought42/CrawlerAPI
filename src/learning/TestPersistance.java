package learning;

import java.util.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;


public class TestPersistance {
	public static void main(String[] args){
		// Start EntityManagerFactory
		 EntityManagerFactory emf =
				 Persistence.createEntityManagerFactory("minion");
		 
		// First unit of work
		 try{
			 EntityManager em = emf.createEntityManager();
			 EntityTransaction tx = em.getTransaction();
			 tx.begin();
			 
			 ObjectDefinition objDef = new ObjectDefinition("td", "tag");
			 em.persist(objDef);
			
			 tx.commit();
			 em.clear();
			 em.close();
		 }catch(PersistenceException e){
			 e.printStackTrace();
		 }
		 
		// Second unit of work
		 EntityManager newEm = emf.createEntityManager();
		 EntityTransaction newTx = newEm.getTransaction();
		 newTx.begin();
		 List objDefs = newEm
		 .createQuery("select m from ObjectDefinition m order by m.name asc")
		 .getResultList();

		 System.out.println( objDefs.size() + " Object definition(s) found" );
		 for (Object m : objDefs) {
			 ObjectDefinition loadedMsg = (ObjectDefinition) m;
			 System.out.println(loadedMsg.toString());
		 }
		 newTx.commit();
		 newEm.clear();
		 newEm.close();

		 
		 // Shutting down the application
		 
		 emf.close();
	}

}
