import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;


public class ConcurrentNode<T> {
	public ConcurrentHashMap<ConcurrentNode<?>, Double> inputs;
	public ConcurrentHashMap<ConcurrentNode<?>, Double> outputs;
	public T data;
	
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
		outputs.put(node,  .0001);
	}
	
	public Double getOutputWeight(ConcurrentNode<?> node){
		return outputs.get(node);
	}
}
