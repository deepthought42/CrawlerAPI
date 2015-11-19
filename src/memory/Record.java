package memory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * A record is a means of saving/retrieving a vocabulary from a file
 * 
 * @author Brandon Kindred
 *
 * @param <E>
 */
public class Record {
	
	/**
	 * Reads {@link Vocabulary} from a file using a given path
	 * 
	 * @param file
	 * @param vocab
	 * @throws IOException 
	 */
	public static void load(String path, Vocabulary<?> vocab) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(path));
		String s=null;
        while((s=reader.readLine())!=null && (s = s.trim()).length() > 0){
           String f[] = s.split(",");
           System.out.println("STRING RETRIEVED FROM FILE :: " +s);
        }  reader.close();
	}
	
	/**
	 * Writes vocabulary to a file using a given path
	 * 
	 * @param file
	 * @param vocab
	 * @throws IOException 
	 */
	public static void save(String path, Vocabulary<?> vocab) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(path));
		writer.write(vocab.toString());
		
		writer.close();
	}

}
