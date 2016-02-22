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
	
	public void appendToPaths(Path path, Boolean isValuable){
		this.paths.add(path);
		StringBuilder pathString = new StringBuilder();
		pathString.append(path.getClass().getCanonicalName().toString());
	
		for(PathObject<?> pathObj: path.getPath()){
			pathString.append(pathObj.getData().getClass().getCanonicalName());
			pathString.append("::");
			pathString.append(pathObj.getData().toString());
			pathString.append("::");
			pathString.append(pathObj.getData().hashCode());
			pathString.append(";;");
		}
	
		String valuability = isValuable.toString();
		List<String> lines = Arrays.asList(pathString.toString(), valuability);
		java.nio.file.Path file = Paths.get("/home/deepthought/MinionLogs/PathRecords.txt");
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
			// TODO Auto-generated catch block
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
