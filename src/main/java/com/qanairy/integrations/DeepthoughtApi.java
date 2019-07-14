package com.qanairy.integrations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qanairy.models.Form;

public class DeepthoughtApi {
	private static Logger log = LoggerFactory.getLogger(DeepthoughtApi.class);

	public static void predict(Form form) throws UnsupportedOperationException, IOException{
		ObjectMapper mapper = new ObjectMapper();

        //Object to JSON in String
        String form_json = mapper.writeValueAsString(form);
        
        log.info("Requesting prediction for form from RL system");
	  	
	  	CloseableHttpClient client = HttpClients.createDefault();
	    HttpPost httpPost = new HttpPost("http://198.211.117.122:9080/rl/predict");
	 
	    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
	    builder.addTextBody("json_object", form_json);
	    builder.addTextBody("input_vocab_label", "html");
	    builder.addTextBody("output_vocab_label", "form_type");
	    builder.addTextBody("new_output_features", Arrays.toString(Arrays.stream(form.getTypeOptions()).map(Enum::name).toArray(String[]::new)));
	    
	    HttpEntity multipart = builder.build();
	    httpPost.setEntity(multipart);

	    CloseableHttpResponse response = client.execute(httpPost);

	  	log.info("Recieved status code from RL :: "+response.getStatusLine().getStatusCode());
	  	log.info("REPSONE ENTITY CONTENT ::   " +response.getEntity().getContent().toString());
	  	int status = response.getStatusLine().getStatusCode();
	  	
  		String rl_response = "";
        switch (status) {
            case 200:
            case 201:
            	
                BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line+"\n");
                }
                br.close();
                rl_response = sb.toString();
                log.info("Response received from RL system :: "+rl_response);	
                break;
            case 500:
            	return;
        }
	    client.close();
        
        log.info("form tag :: "+form.getFormTag());
        log.info("form tax xpath :: "+form.getFormTag().getXpath());
        JSONObject obj = new JSONObject(rl_response);
        log.info("RL RESPONSE OBJ :: " + obj);
        JSONArray prediction = obj.optJSONArray("prediction");
        long memory_id = obj.getLong("id");
        
        if (prediction == null) { /*...*/ }

	     // Create an int array to accomodate the numbers.
	     double[] weights = new double[prediction.length()];

	     // Extract numbers from JSON array.
	     for (int i = 0; i < prediction.length(); ++i) {
	         weights[i] = prediction.optDouble(i);
	     }			

	     form.setMemoryId(memory_id);
	  	 form.setPredictions(weights);
	}
	
	public static void learn(Form form) throws UnsupportedOperationException, IOException{
		log.info("FORM ::    "+form);
		log.info("FORM MEMORY ID   :::   "+form.getMemoryId());
		log.info("feature value :: "+form.getType());
	  	log.info("Requesting prediction for form from RL system");
	  	
	  	CloseableHttpClient client = HttpClients.createDefault();
	    HttpPost httpPost = new HttpPost("http://198.211.117.122:9080/rl/learn");
	 
	    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
	    builder.addTextBody("memory_id", form.getMemoryId().toString());
	    builder.addTextBody("feature_value", form.getType().toString());
	    
	    //builder.addTextBody("isRewarded", Boolean.toString(isRewarded));
	    //builder.addTextBody("new_output_features", Arrays.toString(Arrays.stream(form.getTypeOptions()).map(Enum::name).toArray(String[]::new)));
	    
	    HttpEntity multipart = builder.build();
	    httpPost.setEntity(multipart);

	    CloseableHttpResponse response = client.execute(httpPost);

	  	log.info("Recieved status code from RL :: "+response.getStatusLine().getStatusCode());
	  	log.info("REPSONE ENTITY CONTENT ::   " +response.getEntity().getContent().toString());
	  	int status = response.getStatusLine().getStatusCode();
	  	
  		String rl_response = "";
        switch (status) {
            case 200:
            case 201:
            	
                BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line+"\n");
                }
                br.close();
                rl_response = sb.toString();
                log.info("Response received from RL system :: "+rl_response);	
                break;
            case 400:
            	log.info("***********************************************************");
            	log.info("RL returned a 400");
            	log.info("***********************************************************");
            case 500:
            	return;
        }
	    client.close();
	}
}
