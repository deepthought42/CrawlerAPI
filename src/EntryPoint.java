
public class EntryPoint {
	public static void main(String[] args){
		String url = "localhost:3000";
		
		Actor actor = new Actor(url);
		actor.run();
	}
}
