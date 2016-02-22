package api;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that defines endpoints to access data for path's experienced in the past
 * @author Brandon Kindred
 */
@RestController
@RequestMapping("/pastPaths")
public class PastPathExperienceController {
	
	@RequestMapping("/")
	public String getPastExperiences() {
		 // The name of the file to open.
        String fileName = "temp.txt";

        // This will reference one line at a time
        String line = null;
        BufferedReader bufferedReader = null;
        StringBuffer lines = new StringBuffer();
        try {
            // FileReader reads text files in the default encoding.
            FileReader fileReader = new FileReader(fileName);

            // Always wrap FileReader in BufferedReader.
            bufferedReader = new BufferedReader(fileReader);

            while((line = bufferedReader.readLine()) != null) {
                lines.append("|||" + line);
            }   

            // Always close files.
            bufferedReader.close();         
        }
        catch(FileNotFoundException ex) {
            System.out.println(
                "Unable to open file '" + 
                fileName + "'");                
        }
        catch(IOException ex) {
            System.out.println(
                "Error reading file '" 
                + fileName + "'");                  
            // Or we could just do this: 
            // ex.printStackTrace();
        }

		return lines.toString();
	}
}
