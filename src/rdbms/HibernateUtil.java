package rdbms;

import org.hibernate.*;
import org.hibernate.cfg.*;
public class HibernateUtil {
	 private static SessionFactory sessionFactory;
	 static {
		 try {
			 sessionFactory=new Configuration()
			 .configure()
			//Listing 2.6 The HibernateUtil class for startup and SessionFactory handling
			//Starting a Hibernate project 57
			 .buildSessionFactory();
		 } catch (Throwable ex) {
			 throw new ExceptionInInitializerError(ex);
		 }
	 }
	 public static SessionFactory getSessionFactory() {
		 // Alternatively, you could look up in JNDI here
		 return sessionFactory;
	 }
	 public static void shutdown() {
		 // Close caches and connection pools
		 getSessionFactory().close();
	 }
}