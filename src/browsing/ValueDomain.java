package browsing;

import java.util.ArrayList;
import java.util.Random;

public class ValueDomain {
	
	private ArrayList<String> values = new ArrayList<String>();
	private String alphabet="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private String specialCharacters = alphabet+"+={}/\\:;!@#$%^&*()~|<>?[]-_";
	
	public ValueDomain(){
		//add empty string as bare minimum
		values.add("");
	}
	
	/**
	 * Adds 100 random long values to values of domain
	 */
	public void addRandomRealNumbers(){
		Random random = new Random();
		for(int i= 0; i< 100; i++){
			values.add(Long.toString(random.nextLong()));
		}
	}
	
	/**
	 * Adds 100 random decimal values to values of domain
	 */
	public void addRandomDecimals(){
		Random random = new Random();
		
		for(int i= 0; i< 100; i++){
			values.add(Long.toString(random.nextLong()) + Long.toString(random.nextLong()));
		}
	}
	
	/**
	 * Adds 100 random alphabetic strings to values of domain
	 */
	public void addRandomAlphabeticStrings(){
		Random random = new Random();
		
		for(int i= 0; i< 100; i++){
			StringBuilder valueSeq = new StringBuilder();
			
			for(long j = 0L; j< random.nextLong(); j++){
				int  n = random.nextInt(52) + 1;
				valueSeq.append(alphabet.charAt(n));
			}
			values.add(valueSeq.toString());
		}
	}
	

	/**
	 * Adds 100 random specialCharacter strings to values of domain
	 */
	public void addRandomSpecialCharacterAlphabeticStrings(){
		Random random = new Random();
		
		for(int i= 0; i< 10; i++){
			StringBuilder valueSeq = new StringBuilder();
			
			for(int j = 0; j< random.nextInt(); j++){
				int  n = random.nextInt(79) + 1;
				valueSeq.append(specialCharacters.charAt(n));
			}
			values.add(valueSeq.toString());
		}
	}
	
	public ArrayList<String> getValues(){
		return this.values;
	}
}
