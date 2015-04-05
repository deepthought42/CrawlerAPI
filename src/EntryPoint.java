/**
 * 
 * @author Brandon Kindred
 *
 */
public class EntryPoint {
	public static void main(String[] args){
		String url = "localhost:3000";
		//TODO :: CHECK MEMORY FOR ENTRANCE PAGE WITH URL
		//GENERATE MOCK PAGE FOR TESTING
		BrowserActor actor = new BrowserActor(url);
		actor.run();
	}
}
