package com.qanairy.auth;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import com.auth0.client.mgmt.ManagementAPI;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

public class Auth0ManagementApi {
	private static Logger log = LoggerFactory.getLogger(Auth0Client.class);

	private static String client_id = "d0YAPCQl5rk8YhsDI1U5GaEfqvMSC5Ea";
	private static String client_secret = "kbHd7I5avP_d5jhofdhAcTGMJKYdNnnzgevoCddRSryv2EgLmrXvSz4aEqZBvfMp";
	private static String base_url = "https://qanairy.auth0.com/";
	private static String audience_url = base_url + "api/v2/";
	private static String api_token = "8hk4R5YJ4gO5xPZdjjMdy7YtUF8eA22F";
	private ManagementAPI mgmt_api;
	
	public Auth0ManagementApi(String access_token){
		this.mgmt_api = new ManagementAPI(base_url, api_token);
	}
	
	public static String getToken() throws UnirestException{
		HttpResponse<String> response1 = Unirest.post(base_url + "oauth/token")
					  .header("content-type", "application/json")
					  .body("{\"client_id\":\"" + client_id + "\",\"client_secret\":\"" + client_secret + "\",\"audience\":\"" + audience_url + "\",\"grant_type\":\"client_credentials\"}")
					  .asString();		

		return jsonMap(response1).get("access_token").toString();
	}
	
	public static Map<String, Object> jsonMap(HttpResponse<String> resp){
		Map<String, Object> map = new HashMap<String, Object>();

		try {
			ObjectMapper mapper = new ObjectMapper();
			
			// convert JSON string to Map
			map = mapper.readValue(resp.getBody(), new TypeReference<Map<String, String>>(){});
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return map;
	}
	
	public static HttpResponse<String> updateUserAppMetadata(String user_id, String app_metadata_body) throws UnirestException{
    	String token = getToken();
    	String request_url = audience_url+"users/" + user_id;
		HttpResponse<String> response = Unirest.patch(request_url)
				  .header("content-type", "application/json")
				  .header("Authorization", "Bearer " + token)
				  .body("{\"app_metadata\": " + app_metadata_body + "}")
				  .asString();
		
		return response;
	}

	public static HttpResponse<String> deleteUser(String user_id) throws UnirestException {
		String token = getToken();
		System.err.println("USER ID PASSED FOR DELETION :::::::::::::     "+user_id);
    	String request_url = audience_url+"users/" + user_id;
		HttpResponse<String> response = Unirest.delete(request_url)
				  .header("content-type", "application/json")
				  .header("Authorization", "Bearer " + token)
				  .asString();
		
		return response;
	}

	public ManagementAPI getApi() {
		return mgmt_api;
	}
}
