package memory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONObject;

import browsing.PathObject;
import structs.Path;

public class PastExperience {
	private ArrayList<Path> paths = null;
	private int value = 0;
	private Boolean useful = null;
	
	public PastExperience() {
		paths = new ArrayList<Path>();
	}
	
	public void appendToPaths(Path path){
		this.paths.add(path);
	}
	
	/**
	 * Appends a path to the end of a path record
	 * 
	 * @param path	{@link Path} to append to list
	 * @param isValuable indicator of if path is seen as valuable, invaluable, or unknown value
	 */
	public void appendToPaths(Path path, Boolean isValuable){
		this.paths.add(path);
		
		boolean last_id_set=false;
		int last_id = 0;
		JSONObject jsonObj = new JSONObject();
		for(PathObject<?> pathObj : path.getPath()){
			int objHash =  pathObj.getData().hashCode();
			JSONObject pathJson = new JSONObject();
			pathJson.append("id", pathObj.getData().hashCode());
			//pathJson.append("string", pathObj.getData().toString());
			pathJson.append("canonicalClassName", pathObj.getData().getClass().getCanonicalName());
			
			jsonObj.append("nodes", pathJson);
			
			if(last_id_set){
				JSONObject edgeObject = new JSONObject();
				edgeObject.append("id", last_id+""+objHash);
				edgeObject.append("from", last_id);
				edgeObject.append("to", objHash);
				jsonObj.append("edges", edgeObject);
			}
			last_id = objHash;
			last_id_set = true;
		}
		
		jsonObj.append("productive", isValuable);
	
		List<String> lines = Arrays.asList(jsonObj.toString());
		java.nio.file.Path file = Paths.get("/home/deepthought/workspace/WebTestVisualizer/MinionLogs/PathRecords.txt");
		try {
			Files.write(file, lines, Charset.forName("UTF-8"), StandardOpenOption.APPEND);
		} catch (NoSuchFileException e) {
			try {
				Files.write(file, lines, Charset.forName("UTF-8"));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public ArrayList<Path> getPaths(){
		return this.paths;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	public Boolean getUseful() {
		return useful;
	}

	public void setUseful(Boolean useful) {
		this.useful = useful;
	}

}
