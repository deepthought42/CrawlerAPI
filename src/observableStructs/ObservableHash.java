package observableStructs;

import java.util.HashMap;
import java.util.Observable;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Creates an Hash that is observable
 * @author Brandon Kindred
 *
 * @param <E>
 */
public class ObservableHash<K, E> extends Observable {
	
	HashMap<K, ConcurrentLinkedQueue<E>> queueHash = null;
	
	/**
	 * 
	 */
	public ObservableHash() {
		queueHash = new HashMap<K, ConcurrentLinkedQueue<E>>();
	}

	public int size() {
		return queueHash.size();
	}

	public boolean isEmpty() {
		return queueHash.isEmpty();
	}

	public HashMap<K, ConcurrentLinkedQueue<E>> getQueueHash(){
		return this.queueHash;
	}
	
	/**
	 *	{@inheritDoc}
	 */
	public ConcurrentLinkedQueue<E> put(K key, E value) {
		System.out.println("Adding queue to hash with key "+key);
		ConcurrentLinkedQueue<E> queue = queueHash.get(key);
		setChanged();
		if(queue == null){
			queue = new ConcurrentLinkedQueue<E>();
			queue.add(value);
			queueHash.put(key, queue);
		}
		else if(!queue.contains(value)){
			queue.add(value);
			queueHash.put(key, queue);
		}	
		notifyObservers(key);
		return queue;
	}

}
