package browsing;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


public class ConcurrentNode<T> {
	public ConcurrentHashMap<ConcurrentNode<?>, Double> inputs;
	public ConcurrentHashMap<ConcurrentNode<?>, Double> outputs;
	public T data;
	volatile AtomicInteger coloring = new AtomicInteger(0);
	
	public ConcurrentNode(T data){
		this.inputs = new ConcurrentHashMap<ConcurrentNode<?>, Double>();
		this.outputs = new ConcurrentHashMap<ConcurrentNode<?>, Double>();
		this.data = data;
	}
	
	public ConcurrentNode(
			ConcurrentHashMap<ConcurrentNode<?>, Double> inputMap, 
			ConcurrentHashMap<ConcurrentNode<?>, Double> outputMap, 
			T data)
	{
		this.inputs = inputMap;
		this.outputs = outputMap;
		this.data = data;
	}
	
	public void setData(T data){
		this.data = data;
	}
	
	public void addInput(ConcurrentNode<?> node){
		//.0001 chosen for assumption of behaving as an extremely low weight for initial connection
		inputs.put(node, .0001);
	}
	
	public Double getInputWeight(ConcurrentNode<?> node){
		return inputs.get(node);
	}
	
	public void addOutput(ConcurrentNode<?> node){
		outputs.put(node,  .9);
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
}
