package browsing.actions;

import java.util.Iterator;
import java.util.UUID;

import org.apache.log4j.Logger;

import browsing.IObjectValuationAccessor;
import browsing.PathObject;
import persistence.IAction;
import persistence.IPersistable;
import persistence.OrientConnectionFactory;
import tester.Test;

/**
 * Defines an action in name only
 * 
 * @author Brandon Kindred
 *
 */
public class Action implements PathObject, IPersistable<IAction>, IObjectValuationAccessor{
	private static final Logger log = Logger.getLogger(Test.class);

	private final String name;
	private final String key;
	/**
	 * 
	 * @param action_name
	 */
	public Action(String action_name) {
		this.name = action_name;
		this.key = generateKey();
	}
	
	/**
	 * @return the name of this action
	 */
	public String getName(){
		return this.name;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString(){
		return this.name;
	}
	
	@Override
	public int hashCode(){
		return this.name.hashCode();
	}

	public double getCost() {
		// TODO Auto-generated method stub
		return 0;
	}

	public double getReward() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Action data() {
		return this;
	}

	public String getKey() {
		return this.key;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey() {
		return this.name;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IAction convertToRecord(OrientConnectionFactory connection) {
		IAction action = connection.getTransaction().addVertex(UUID.randomUUID(), IAction.class);
		action.setName(this.name);
		action.setKey(this.key);
		return action;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IPersistable<IAction> create() {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		
		this.convertToRecord(orient_connection);
		orient_connection.save();
		
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IPersistable<IAction> update(IAction existing_obj) {
		Iterator<IAction> action_iter = this.findByKey(this.generateKey()).iterator();
		int cnt=0;
		while(action_iter.hasNext()){
			action_iter.next();
			cnt++;
		}
		log.info("# of existing records with key "+this.getKey() + " :: "+cnt);
		
		OrientConnectionFactory connection = new OrientConnectionFactory();
		IPersistable<IAction> action = null;
		if(cnt == 0){
			action = connection.getTransaction().addVertex(UUID.randomUUID(), IAction.class);	
		}
		
		action = this.convertToRecord(connection);
		connection.save();
		
		return action;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterable<IAction> findByKey(String generated_key) {
		OrientConnectionFactory orient_connection = new OrientConnectionFactory();
		return orient_connection.getTransaction().getVertices("key", generated_key, IAction.class);
	}
}
