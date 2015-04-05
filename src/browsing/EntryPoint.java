package browsing;
/**
 * 
 * @author Brandon Kindred
 *
 */
public class EntryPoint {
	public static void main(String[] args){
		String url = "localhost:3000";

		BrowserActor actor = new BrowserActor(url);
		actor.run();
	}
}
