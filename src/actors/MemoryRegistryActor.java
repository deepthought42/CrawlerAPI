package actors;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.tinkerpop.blueprints.Vertex;

import akka.actor.UntypedActor;
import memory.DataDecomposer;
import memory.ObjectDefinition;
import memory.OrientDbPersistor;
import memory.Vocabulary;
import browsing.ActionFactory;
import browsing.Page;
import structs.Message;
import tester.Test;
import tester.TestRecord;

/**
 * Handles the saving of records into orientDB
 * 
 * @author Brandon Kindred
 *
 */
public class MemoryRegistryActor extends UntypedActor{
    private static final Logger log = Logger.getLogger(BrowserActor.class);

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof Message){
			Message<?> acct_msg = (Message<?>)message;
			//Retains lists of productive, unproductive, and unknown value {@link Path}s.
			if(acct_msg.getData() instanceof Page){
				Page page = (Page)acct_msg.getData();
				List<Object> decomposed_list = DataDecomposer.decompose(page);
				OrientDbPersistor persistor = new OrientDbPersistor();
				
				for(Object objDef : decomposed_list){
					//if object definition value doesn't exist in vocabulary 
					// then add value to vocabulary
					Vocabulary vocabulary = new Vocabulary(new ArrayList<String>(), "page");
					vocabulary.appendToVocabulary(((ObjectDefinition)objDef).getValue());
					persistor.findAndUpdateOrCreate(vocabulary, ActionFactory.getActions());
				}
			}
			else if(acct_msg.getData() instanceof Test){
				Test test = (Test)acct_msg.getData();

				//check if test with key already exists.
				OrientDbPersistor persistor = new OrientDbPersistor();
				Iterator<Vertex> vertex_iter = persistor.findVertices(test).iterator();//findByKey(test.getKey());
				// if record already exists then create TestRecord and append it to records for test
				if(vertex_iter.hasNext()){
					log.info("Test already exists....adding test as record");
					Test prev_test = (Test)vertex_iter.next();					
					TestRecord record = new TestRecord(test.getPath(), new Date(), prev_test.getPath().equals(test.getPath()));
					prev_test.addTestRecord(record);
					
				}
				else{
					log.info("Saving test for the first time");
					Date date = new Date();
					TestRecord record = new TestRecord(test.getPath(), date, true);
					log.info("TEST RECORD :: "+record);
					log.info("TEST PATH :: "+test.getPath());
					log.info("DATE :: " + date);
					
					test.addTestRecord(record);
					persistor.findAndUpdateOrCreate(test);
					//persistor.addVertexType(record, OrientDbPersistor.getProperties(record));
					//persistor.createVertex(test);
					//persistor.save();
				}
			}
		}
		else unhandled(message);
	}
}
