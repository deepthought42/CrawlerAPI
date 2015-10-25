package memory;


/**
 * A state consists of an identifier and an image formatted to base64
 * 
 * @author Brandon Kindred
 */
public class MemoryState {
	/**
	 * Identifier is meant to identify the state
	 * TO DO :: Make identifier be a hashsum of the contained objects
	 */
	public int identifier = 0;
	public String base64_img = null;
		
	
	/**
	 * 
	 * @param objects
	 */
	public MemoryState(int identifier, String base_64) {
		this.setIdentifier(identifier);
		this.setBase64Img(base_64);
	}

	
	
	/**
	 * 
	 * @return
	 */
	public int getIdentifier() {
		return this.identifier;
	}

	/**
	 * 
	 * @param identifier
	 */
	public void setIdentifier(int identifier) {
		this.identifier = identifier;
	}
	
	/**
	 * 
	 * @param base_64
	 */
	public void setBase64Img(String base_64){
		this.base64_img = base_64;
	}
	
	/**
	 * 
	 * @return
	 */
	public String getBase64Img(){
		return this.base64_img;
	}
}
