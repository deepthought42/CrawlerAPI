package structs;
import java.util.Observable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * 
 * @author Brandon Kindred
 *
 * @param <T>
 */
public class ConcurrentNode<T> extends Observable {
	private ConcurrentHashMap<ConcurrentNode<?>, Double> inputs;
	private ConcurrentHashMap<ConcurrentNode<?>, Double> outputs;
	public T data;
	public Class<?> type;
	volatile AtomicBoolean isEntryNode = new AtomicBoolean(false);
	
	public ConcurrentNode(T data){
		this.inputs = new ConcurrentHashMap<ConcurrentNode<?>, Double>();
		this.outputs = new ConcurrentHashMap<ConcurrentNode<?>, Double>();
		this.data = data;
		this.type = data.getClass();
	}
	
	public ConcurrentNode(
			ConcurrentHashMap<ConcurrentNode<?>, Double> inputMap, 
			ConcurrentHashMap<ConcurrentNode<?>, Double> outputMap, 
			T data)
	{
		this.inputs = inputMap;
		this.outputs = outputMap;
		this.data = data;
		this.type = data.getClass();
	}
	
	public ConcurrentNode() {
		this.inputs = new ConcurrentHashMap<ConcurrentNode<?>, Double>();
		this.outputs = new ConcurrentHashMap<ConcurrentNode<?>, Double>();
		this.data = null;
	}

	public void setData(T data){
		this.data = data;
	}
	
	public void addInput(ConcurrentNode<?> node){
		//.0001 chosen for assumption of behaving as an extremely low weight for initial connection
		inputs.put(node, .0001);
		setChanged();
        notifyObservers();                                                                  // makes the observers print null
	}
	
	public Double getInputWeight(ConcurrentNode<?> node){
		return inputs.get(node);
	}
	
	public void addOutput(ConcurrentNode<?> node){
		outputs.put(node,  .9);
		setChanged();
        notifyObservers(this);                                                                  // makes the observers print null
	}
	
	public Double getOutputWeight(ConcurrentNode<?> node){
		return outputs.get(node);
	}
	
	public ConcurrentHashMap<ConcurrentNode<?>, Double> getOutputs(){
		return this.outputs;
	}
	
	public T getData(){
		return this.data;
	}
	
	public Class<? extends Object> getType(){
		return this.type;
	}
}
