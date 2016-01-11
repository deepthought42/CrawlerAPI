
import actors.ResourceManagementActor;
import actors.WorkAllocationActor;
import observableStructs.ObservableHash;
import structs.Path;

/**
 * Initializes the system and launches it. 
 * @author Brandon Kindred
 *
 */
public class EntryPoint {
	public static void main(String[] args){
		
		ResourceManagementActor resourceManager = new ResourceManagementActor(3);
		ObservableHash<Integer, Path> hashQueue = new ObservableHash<Integer, Path>();

		String url = "http://127.0.0.1:3000";
		//String url = "http://brandonkindred.ninja/blog";
		//String url = "http://www.ideabin.io";
		System.out.print("INITIALIZING ACTOR...");
		System.out.println("TOTAL CORES AVAILABLE : "+Runtime.getRuntime().availableProcessors());
		
		System.out.print("Initializing page monitor...");
		WorkAllocationActor workAllocator = new WorkAllocationActor(resourceManager, url);
		
	}
}
