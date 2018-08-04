package com.minion.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.data.neo4j.util.IterableUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qanairy.config.WebSecurityConfig;
import com.qanairy.models.FormRecord;
import com.qanairy.models.enums.FormType;
import com.qanairy.models.repository.FormRecordRepository;

/**
 *	API for interacting with {@link User} data
 */
@RestController
@RequestMapping("/form_record")
public class FormRecordController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private FormRecordRepository form_record_repo;
	
    @Autowired
    protected WebSecurityConfig appConfig;
    
    /**
     * Retrieves {@link FormRecord account} with a given key
     * 
     * @param key account key
     * @return {@link FormRecord account}
     */
    @PreAuthorize("hasAuthority('read:form_records')")
    @RequestMapping(method = RequestMethod.GET)
    public List<FormRecord> getAll() {
        logger.info("finding all form records");      
        return IterableUtils.toList(form_record_repo.findAll());
    }
    
    /**
     * Retrieves {@link FormRecord account} with a given key
     * 
     * @param key account key
     * @return {@link FormRecord account}
     * @throws IOException 
     */
    @PreAuthorize("hasAuthority('read:form_records')")
    @RequestMapping(method = RequestMethod.POST)
    public List<FormRecord> setFormType(@RequestParam(value="key", required=true) String key,
										@RequestParam(value="form_type", required=true) String form_type) throws IOException {
    	
    	FormRecord form_record = form_record_repo.findByKey(key);
    	form_record.setFormType(FormType.valueOf(form_type));
    	form_record = form_record_repo.save(form_record);
    	
    	//send form with label for learning to RL system
    	ObjectMapper mapper = new ObjectMapper();

        //Object to JSON in String
        String form_json = mapper.writeValueAsString(form_record);
        
	  	byte[] out = form_json.getBytes(StandardCharsets.UTF_8);
	  	int length = out.length;

	  	System.err.println("Sending form record for learning for form from RL system");
	  	URL url = new URL("https://rl.qanairy.com/learn");
	  	URLConnection con = url.openConnection();
	  	HttpURLConnection http = (HttpURLConnection)con;
	  	http.setFixedLengthStreamingMode(length);
	  	http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
	  	http.connect();
	  	try(OutputStream os = http.getOutputStream()) {
	  	    os.write(out);
	  	}

	  	int status = http.getResponseCode();
	  	System.err.println("Recieved status code from RL :: "+status);
  		String rl_response = "";

        switch (status) {
            case 200:
            case 201:
                BufferedReader br = new BufferedReader(new InputStreamReader(http.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line+"\n");
                }
                br.close();
                rl_response = sb.toString();
                System.err.println("Response received from RL system :: "+rl_response);
        }

    	
    	//send form to have tests created by form test discovery
    	
        
        
        
        
        logger.info("setting form type");
        return IterableUtils.toList(form_record_repo.findAll());
    }
}

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
class FormRecordExistsException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7200878662560716216L;

	public FormRecordExistsException() {
		super("This account already exists.");
	}
}