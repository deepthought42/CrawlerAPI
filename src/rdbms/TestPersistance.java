package rdbms;

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
		 ObjectDefinition objDef1 = null;
		 try{
			 EntityManager em = emf.createEntityManager();
			 EntityTransaction tx = em.getTransaction();
			 tx.begin();
			 
			 objDef1 = new ObjectDefinition("p", "tag");
			 em.persist(objDef1);
			
			 tx.commit();
			 em.clear();
			 em.close();
		 }catch(PersistenceException e){
			 e.printStackTrace();
		 }
		 
		// First unit of work
		 ObjectDefinition objDef2 = null;
		 try{
			 EntityManager em = emf.createEntityManager();
			 EntityTransaction tx = em.getTransaction();
			 tx.begin();
			 
			 objDef2 = new ObjectDefinition("b", "tag");
			 em.persist(objDef2);
			
			 tx.commit();
			 em.clear();
			 em.close();
		 }catch(PersistenceException e){
			 e.printStackTrace();
		 }
		 
		 try{
			 EntityManager em = emf.createEntityManager();
			 EntityTransaction tx = em.getTransaction();
			 tx.begin();
			 ObjectDefinition objDef10 = new ObjectDefinition(5, "i", "tag");
			 ObjectDefinition objDef11 = new ObjectDefinition(6, "head", "tag");
			 ObjectAssociationId associationId = new ObjectAssociationId(objDef10.getId(), objDef11.getId());
			 ObjectDefinitionAssociation object_association = new ObjectDefinitionAssociation();
			 object_association.setId(associationId);
			 object_association.setObject1Definition(objDef10);
			 object_association.setObject2Definition(objDef11);
			 object_association.setWeight(.00001);
			 object_association.setCount(1);
		        
			 objDef1.getObjectAssociations().add(object_association);
			 em.persist(object_association);

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
