package structs;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.web.ServerProperties.Session;

import browsing.PageElement;

/**
 * Tracks element action sequences by session
 * 
 * @author Brandon Kindred
 *
 */
public class SessionSequenceTracker {
	private Map<String, ElementActionSequenceMapper> sessionSequences;
	private static SessionSequenceTracker instance = null;

	/**
	 * 
	 */
	protected SessionSequenceTracker() {
		sessionSequences = new HashMap<String, ElementActionSequenceMapper>();
	}
	
	/**
	 * 
	 * @return
	 */
    public ElementActionSequenceMapper getSequencesForSession(String session_key){
	   return this.sessionSequences.get(session_key);
    }
   
    /**
	 * Add session to tracker with empty hash map for element actions
	 */
    public void addSessionSequences(String session_key){
    	this.sessionSequences.put(session_key, new ElementActionSequenceMapper());
    }
    
	/**
	 * @return singleton instance of session sequence tracker
	 */
	public synchronized static SessionSequenceTracker getInstance() {
      if(instance == null) {
         instance = new SessionSequenceTracker();
      }
      return instance;
   }	   
}
