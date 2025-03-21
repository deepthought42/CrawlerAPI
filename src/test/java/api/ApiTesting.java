package api;

import static com.jayway.restassured.RestAssured.*;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.restassured.response.Response;

public class ApiTesting
{
	
	protected static final String grantTypeValue ="client_credentials";
	protected static final String clientIdValue ="jEhM2ZhIWwy49YiJsNE9g9YtLEEn3Vxw";
	protected static final String clientSecretValue ="XUtP8AOw4cRKTpJJT_BWjUgfFnrEq4TRjA8oSkVeRBTbIPbeNIrXVoydc5EefRqm";
	protected static final String audienceValue = "https://staging-api.qanairy.com";
	protected static final String scopeValue = "";
	protected static final String endpointURI = "https://staging-qanairy.auth0.com";
	protected static final String oauthURI = endpointURI + "/oauth/token";
	
	protected static final String domainsURI = endpointURI + "/domains";
	
	protected static final String JSON = "application/json";
	protected static final String XML = "application/xml";
	protected static String accessTokenValue = "";
	private static final Logger LOGGER = LoggerFactory.getLogger("API Testing");

	@Test
	public void Test_GetAccessToken() throws Exception {
		LOGGER.info("Starting Test_GetAccessToken");
		Response resp= given()
				.formParam("client_id", clientIdValue)
				.formParam("client_secret", clientSecretValue)
				.formParam("audience", audienceValue)
				.formParam("grant_type", grantTypeValue)
//				.formParam("scope", scopeValue)
				.when()
				.post(oauthURI);
		resp.prettyPeek();
		//Assert.assertEquals( resp.getStatusCode(), 200 );
		
		//accessTokenValue = resp.then().contentType(JSON).extract().path("access_token");
		LOGGER.info("Got access token successfully");
		LOGGER.info("Completed Test_GetAccessToken");
	}
	
	/*@Test(description ="This test sends wrong grant_type to get the access token.Negative Test")
	
	public void Test_WrongGrantAccessToken() throws Exception {
		LOGGER.info("Starting Test_WrongGrantAccessToken");
		given()
			.formParam("grant_type", "")
			.formParam("client_id", clientIdValue)
			.formParam( "client_secret", clientSecretValue)
			.formParam("scope", scopeValue)
		.when()
			.post(oauthURI)
			.prettyPeek()
		.then()
			.statusCode(400)
			.body("error", equalTo("unsupported_grant_type"))
			.body("message", equalTo("The authorization grant type is not supported by the authorization server."))
			.body("hint", equalTo("Check the `grant_type` parameter"));
		LOGGER.info("Completed Test_WrongGrantAccessToken");
						
	}
	
	@Test(description ="This test sends wrong Client id to get the access token.Negative Test")
	
	public void Test_WrongClientIdAccessToken() throws Exception {
		LOGGER.info("Starting Test_WrongClientIdAccessToken");
		given()
			.formParam("grant_type",grantTypeValue )
			.formParam("client_id", 3)
			.formParam( "client_secret", clientSecretValue)
			.formParam("scope", scopeValue)
		.when()
			.post(oauthURI)
			.prettyPeek()
		.then()
			.statusCode(401)
			.body("error", equalTo("invalid_client"))
			.body("message", equalTo("Client authentication failed"));
		LOGGER.info("Completed Test_WrongClientIdAccessToken");
						
	}
	
	@Test(description ="This test sends wrong Client Secret to get the access token.Negative Test")
	public void Test_WrongClientSecretAccessToken() throws Exception {
		LOGGER.info("Starting Test_WrongClientSecretAccessToken");
		given()
			.formParam("grant_type", grantTypeValue)
			.formParam("client_id", clientIdValue)
			.formParam( "client_secret", "abdc1234")
			.formParam("scope", scopeValue)
		.when()
			.post(oauthURI)
			.prettyPeek()
		.then()
			.statusCode(401)
			.body("error", equalTo("invalid_client"))
			.body("message", equalTo("Client authentication failed"));
		LOGGER.info("Completed Test_WrongClientSecretAccessToken");
			
	}
	
	@Test(description ="This test sends wrong Scope to get the access token.Negative Test")
	public void Test_WrongScopeAccessToken() throws Exception {
		LOGGER.info("Starting Test_WrongScopeAccessToken");
		given()
			.formParam("grant_type", grantTypeValue)
			.formParam("client_id", clientIdValue)
			.formParam( "client_secret", "abdc1234")
			.formParam("scope", "Read")
		.when()
			.post(oauthURI)
			.prettyPeek()
		.then()
			.statusCode(401)
			.body( "error", equalTo("invalid_client"))
			.body( "message", equalTo("Client authentication failed"));
		LOGGER.info("Completed Test_WrongScopeAccessToken");
	}

	@Test(dependsOnMethods="Test_GetAccessToken",description ="This test verifies the message(time out value, token type =Bearer) upon getting the access token.Positive Test")
	public void Test_GetAccessTokenValidateMessage() throws Exception {
		LOGGER.info("Starting Test_GetAccessTokenValidateMessage");
		given()
			.formParam("grant_type", grantTypeValue)
			.formParam("client_id", clientIdValue)
			.formParam( "client_secret", clientSecretValue)
			.formParam("scope", scopeValue)
		.when()
			.post(oauthURI)
			.prettyPeek()
		.then()
			.statusCode( 200 )
			.body( "token_type", equalTo("Bearer") )
			.body("expires_in" , lessThanOrEqualTo(3600));
			//.body( hasKey("access_token"));
		LOGGER.info("Completed Test_GetAccessTokenValidateMessage");
	}
*/	
	//################################ End of Authentication test Cases #################################################################
	 
    //################################ Start of Login test Cases ########################################################################	
	
	@Test()//dependsOnMethods="Test_GetAccessToken",description="Login.  It doesnt emulate any user login.")
	public void Test_Login() throws Exception {
		LOGGER.info("Started Test_Login");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(domainsURI)
			.prettyPeek()
		//.then()
		//	.statusCode(200)
		//	.body("logged_in", equalTo(true))
			;
		LOGGER.info("Completed Test_Login");
	}

	/*
	@Test(dependsOnMethods="Test_GetAccessToken",description="Multiple times login request without logout.")
	public void Test_MultiLogin() throws Exception {
		LOGGER.info("Started Test_MultiLogin");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(loginURI);
		
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(loginURI)
			.prettyPeek()
		.then()
			.statusCode(202)
			.body("logged_in", equalTo(true));
		LOGGER.info("Completed Test_MultiLogin");
	}

	@Test(dependsOnMethods="Test_GetAccessToken",description="Attempt to login with the wrong access token.")
	public void Test_LoginUsingWrongToken() throws Exception {
		LOGGER.info("Started Test_LoginUsingWrongToken");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer wrong token")
		.when()
			.get(loginURI)
			.prettyPeek()
		.then()
			.statusCode(401)
			.body("error", equalTo("Unauthenticated."));
		LOGGER.info("Completed Test_LoginUsingWrongToken");
	}
	
		@Test(dependsOnMethods="Test_GetAccessToken",description="Login test to see if XML response is returned.")
	public void Test_LoginWithXMLResponse() throws Exception {
		LOGGER.info("Started Test_LoginWithXMLResponse");
		given()
			.headers( "Accept", XML, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(loginURI)
			.prettyPeek()
		.then()
			.statusCode(202)
			.body("logged_in", equalTo(true))
			.header( "Content-Type", XML );
		LOGGER.info("Completed Test_LoginWithXMLResponse");
	}
	
	//#################################### End of LogIn Test Cases  ###########################################################
	 
	//##################################### Start of Logout test Cases  #######################################################
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Logout test to verify the Status code as 200.  It doesnt emulate any user logout.")
	public void Test_Logout() throws Exception {
		LOGGER.info("Started Test_Logout");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(logoutURI)
			.prettyPeek()
		.then()
			.statusCode(200);
		LOGGER.info("Completed Test_Logout");
	}

	@Test(dependsOnMethods="Test_GetAccessToken",description="This test verified the logout message after logout.")
	public void Test_LogoutMessage() throws Exception {
		LOGGER.info("Started Test_LogoutMessage");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(logoutURI)
			.prettyPeek()
		.then()
			.statusCode(200)
			.body("logged_in", equalTo(false));
		LOGGER.info("Completed Test_LogoutMessage");
	}

	@Test(dependsOnMethods="Test_GetAccessToken",description="This test verifies the logout message after multiple times logout request.")
	public void Test_MultiLogout() throws Exception {
		LOGGER.info("Started Test_MultiLogout");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(logoutURI);
		
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(logoutURI)
			.prettyPeek()
		.then()
			.statusCode(200)
			.body("logged_in", equalTo(false));
		LOGGER.info("Completed Test_MultiLogout");
	}

	@Test(dependsOnMethods="Test_GetAccessToken",description="Attempt to logout with the wrong access token.")
	public void Test_LogoutUsingWrongToken() throws Exception {
		LOGGER.info("Started Test_LogoutUsingWrongToken");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer wrong token")
		.when()
			.get(logoutURI)
			.prettyPeek()
		.then()
			.statusCode(401)
			.body("error", equalTo("Unauthenticated."));
		LOGGER.info("Completed Test_LogoutUsingWrongToken");
	}
	
		@Test(dependsOnMethods="Test_GetAccessToken",description="Logout test to see if XML response is returned.")
	public void Test_LogoutWithXMLResponse() throws Exception {
		LOGGER.info("Started Test_LogoutWithXMLResponse");
		given()
			.headers( "Accept", XML, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(logoutURI)
			.prettyPeek()
		.then()
			.statusCode(200)
			.header( "Content-Type", XML )
			.body("logged_in", equalTo(false));
		LOGGER.info("Completed Test_LogoutWithXMLResponse");
	}
	
	//########################################### End of LogOut Test Cases ############################################################
	 
	//##########################################  Start of Asset test cases  ######################################################## 
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Exercise the asset API and returns all assets.")
	public void Test_GetAllAssets() throws Exception {
		LOGGER.info("Started Test_GetAllAssets");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(assetURI)
			.prettyPeek()
		.then()
			.statusCode(200)
			.body("assets", hasSize(5))
			.body("get(0).asset_id", equalTo("12342"))
			.body("get(1).asset_id", equalTo("12341"))
			.body("get(2).asset_id", equalTo("12344"))
			.body("get(3).asset_id", equalTo("12343"))
			.body("get(4).asset_id", equalTo("12345"));
		LOGGER.info("Completed Test_GetAllAssets");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Exercise the asset HEAD API for returning all assets.")
	public void Test_HeadAllAssets() throws Exception {
		LOGGER.info("Started Test_HeadAllAssets");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.head(assetURI)
			.prettyPeek()
		.then()
			.statusCode(200)
			.header("Content-Type", JSON);
		LOGGER.info("Completed Test_HeadAllAssets");
	}
	

	@Test(dependsOnMethods="Test_GetAccessToken",description="Exercise the asset API and returns all assets with XML response.")
	public void Test_GetAllAssetsXMLResponse() throws Exception {
		LOGGER.info("Started Test_GetAllAssetsXMLResponse");
		given()
			.headers( "Accept", XML, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(assetURI)
			.prettyPeek()
		.then()
			.statusCode(200)
			.body("assets", hasSize(5))
			.body("get(0).asset_id", equalTo("12342"))
			.body("get(1).asset_id", equalTo("12341"))
			.body("get(2).asset_id", equalTo("12344"))
			.body("get(3).asset_id", equalTo("12343"))
			.body("get(4).asset_id", equalTo("12345"));
		LOGGER.info("Completed Test_GetAllAssetsXMLResponse");
	}
	

	@Test(dependsOnMethods="Test_GetAccessToken",description="Exercise the asset API by search by ID.")
	public void Test_GetAssetById() throws Exception {
		LOGGER.info("Started Test_GetAssetById");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(assetURI + "/12342")
			.prettyPeek()
		.then()
			.statusCode(200)
			.body("assets", hasSize(1))
			.body("get(0).asset_id", equalTo("12342"))
			.body("get(0).text", equalTo("a lovely old man smiling"));

		LOGGER.info("Completed Test_GetAssetById");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Exercise the asset HEAD API by searching by ID.")
	public void Test_HeadAssetById() throws Exception {
		LOGGER.info("Started Test_HeadAssetById");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.head(assetURI + "/12342")
			.prettyPeek()
		.then()
			.statusCode(200)
			.header( "Content-Type", JSON );

		LOGGER.info("Completed Test_HeadAssetById");
	}

	@Test(dependsOnMethods="Test_GetAccessToken",description="Exercise the asset API by search by ID, but with the wrong token.")
	public void Test_GetAssetByIdWithWrongToken() throws Exception {
		LOGGER.info("Started Test_GetAssetByIdWithWrongToken");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer wrong token")
		.when()
			.get(assetURI + "/12342")
			.prettyPeek()
		.then()
			.statusCode(401)
			.body("error", equalTo("Unauthenticated."));

		LOGGER.info("Completed Test_GetAssetByIdWithWrongToken");
	}

	@Test(dependsOnMethods="Test_GetAccessToken",description="Exercise the asset API by search by a  non existant ID.")
	public void Test_GetAssetByWrongID() throws Exception {
		LOGGER.info("Started Test_GetAssetByWrongID");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(assetURI + "/99999")
			.prettyPeek()
		.then()
			.statusCode(204)   
			.body("$", empty());

		LOGGER.info("Completed Test_GetAssetByWrongID");
	}


	
	//##################################### End of Retrieve Assets TestCases ################################################

    //#################################### Start of Search All test cases  ##################################################
	 
	@Test(dependsOnMethods="Test_GetAccessToken",description="All parameters sent.Positive test")
	public void Test_SearchWithAllParameters() throws Exception {
		LOGGER.info("Started Test_SearchWithAllParameters");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(searchURI + "?query=cherry&sort=asc&limit=2")
			.prettyPeek()
		.then()
			.statusCode( 200 )
			.body( "search", hasSize(2) );
		
		LOGGER.info("Completed Test_SearchWithAllParameters");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="No parameters sent.Negative test")
	public void Test_SearchWithNoParameters() throws Exception {
		LOGGER.info("Started Test_SearchWithNoParameters");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(searchURI)
			.prettyPeek()
		.then()
			.statusCode( 200 ); 
		LOGGER.info("Completed Test_SearchWithNoParameters");
	}
			
	@Test(dependsOnMethods="Test_GetAccessToken",description="Testing search functionality using symbols.Alternate test")
	public void Test_SearchWithQuestionMark() throws Exception {
		LOGGER.info("Started Test_SearchWithQuestionMark");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(searchURI + "?query=&#63;")
			.prettyPeek()
		.then()
			.statusCode( 200 )
			.body( "search", hasSize(1) )
			.body("get(0).asset_id", equalTo("12345"))
			.body("get(0).text", equalTo("¿Estás cansado?"));
		
		LOGGER.info("Completed Test_SearchWithQuestionMark");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Testing search functionality using symbols.Alternate test")
	public void Test_SearchWithReverseQuestionMark() throws Exception {
		LOGGER.info("Started Test_SearchWithReverseQuestionMark");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(searchURI + "?query=¿")
			.prettyPeek()
		.then()
			.statusCode( 200 )
			.body( "search", hasSize(1) )
			.body("get(0).asset_id", equalTo("12345"))
			.body("get(0).text", equalTo("¿Estás cansado?"));
		
		LOGGER.info("Completed Test_SearchWithReverseQuestionMark");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Testing search functionality using numbers.Alternate test")
	public void Test_SearchWithNumbers() throws Exception {
		LOGGER.info("Started Test_SearchWithNumbers");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(searchURI + "?query=1986")
			.prettyPeek()
		.then()
			.statusCode( 200 )
			.body( "search", hasSize(1) )
			.body("get(0).asset_id", equalTo("12343"))
			.body("get(0).text", equalTo("This cherry red boat from 1986 is one of a kind"));
		
		LOGGER.info("Completed Test_SearchWithNumbers");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Testing search functionality using IDs.Alternate test")
	public void Test_SearchWithID() throws Exception {
		LOGGER.info("Started Test_SearchWithID");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(searchURI + "?query=12345")
			.prettyPeek()
		.then()
			.statusCode( 200 )
			.body( "search", hasSize(1) )
			.body("get(0).asset_id", equalTo("12345"))
			.body("get(0).text", equalTo("¿Estás cansado?"));
		
		LOGGER.info("Completed Test_SearchWithID");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Testing search functionality using partial IDs.Alternate test")
	public void Test_SearchWithPartialID() throws Exception {
		LOGGER.info("Started Test_SearchWithPartialID");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(searchURI + "?query=123")
			.prettyPeek()
		.then()
			.statusCode( 200 )
			.body( "search", hasSize(5) );
		
		LOGGER.info("Completed Test_SearchWithPartialID");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Testing search functionality using dates.Alternate test")
	public void Test_SearchWithDate() throws Exception {
		LOGGER.info("Started Test_SearchWithDate");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(searchURI + "?query=Monday, May 8, 2017 11:55:13 PM")
			.prettyPeek()
		.then()
			.statusCode( 200 )
			.body( "search", hasSize(2) )
			.body("get(0).asset_id", equalTo("12345"))
			.body("get(1).asset_id", equalTo("12344"));
		
		LOGGER.info("Completed Test_SearchWithDate");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Testing search functionality using text and number")
	public void Test_SearchWithTextAndNumbers() throws Exception {
		LOGGER.info("Started Test_SearchWithTextAndNumbers");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(searchURI + "?query=123&query=lovely")
			.prettyPeek()
		.then()
			.statusCode( 200 )
			.body( "search", hasSize(1) )
			.body("get(0).asset_id", equalTo("12342"));
		
		LOGGER.info("Completed Test_SearchWithTextAndNumbers");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Testing search functionality using text and limit value.")  
	public void Test_SearchWithTextAndLimitThree() throws Exception {
		LOGGER.info("Started Test_SearchWithTextAndLimitThree");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(searchURI + "?query=123&limit=3")
			.prettyPeek()
		.then()
			.statusCode( 200 )
			.body( "search", hasSize(3) );
		
		LOGGER.info("Completed Test_SearchWithTextAndLimitThree");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Testing search functionality sort as ascending")
	public void Test_SearchWithAllAssetsSortedAscending() throws Exception {
		LOGGER.info("Started Test_SearchWithAllAssetsSortedAscending");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(searchURI + "?sort=asc")
			.prettyPeek()
		.then()
			.statusCode( 200 )
			.body("search", hasSize(5))
			.body("get(0).asset_id", equalTo("12341"))
			.body("get(1).asset_id", equalTo("12342"))
			.body("get(2).asset_id", equalTo("12343"))
			.body("get(3).asset_id", equalTo("12344"))
			.body("get(4).asset_id", equalTo("12345"));
		
		LOGGER.info("Completed Test_SearchWithAllAssetsSortedAscending");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Testing search functionality with sort as descending.")
	public void Test_SearchWithAllAssetsSortedDescending() throws Exception {
		LOGGER.info("Started Test_SearchWithAllAssetsSortedDescending");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(searchURI + "?sort=desc")
			.prettyPeek()
		.then()
			.statusCode( 200 )
			.body("search", hasSize(5))
			.body("get(0).asset_id", equalTo("12345"))
			.body("get(1).asset_id", equalTo("12344"))
			.body("get(2).asset_id", equalTo("12343"))
			.body("get(3).asset_id", equalTo("12342"))
			.body("get(4).asset_id", equalTo("12341"));
		
		LOGGER.info("Completed Test_SearchWithAllAssetsSortedDescending");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Testing search functionality with sort as ascending and limit 3")
	public void Test_SearchWithAllAssetsSortedAscendingAndLimitThree() throws Exception {
		LOGGER.info("Started Test_SearchWithAllAssetsSortedAscendingAndLimitThree");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(searchURI + "?sort=asc&limit=3")
			.prettyPeek()
		.then()
			.statusCode( 200 )
			.body("search", hasSize(3))
			.body("get(0).asset_id", equalTo("12341"))
			.body("get(1).asset_id", equalTo("12342"))
			.body("get(2).asset_id", equalTo("12343"));
		
		LOGGER.info("Completed Test_SearchWithAllAssetsSortedAscendingAndLimitThree");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Testing search functionality with sort as descending and limit 3")
	public void Test_SearchWithAllAssetsSortedDescendingAndLimitThree() throws Exception {
		LOGGER.info("Started Test_SearchWithAllAssetsSortedDescendingAndLimitThree");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(searchURI + "?sort=desc&limit=3")
			.prettyPeek()
		.then()
			.statusCode( 200 )
			.body("search", hasSize(3))
			.body("get(0).asset_id", equalTo("12345"))
			.body("get(1).asset_id", equalTo("12344"))
			.body("get(2).asset_id", equalTo("12343"));
		
		LOGGER.info("Completed Test_SearchWithAllAssetsSortedDescendingAndLimitThree");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Testing search functionality with sort as descending and limit 0")
	public void Test_SearchWithDescendingLimitZero() throws Exception {
		LOGGER.info("Started Test_SearchWithDescendingLimitZero");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(searchURI + "?sort=desc&limit=0")
			.prettyPeek()
		.then()
			.statusCode( 200 )
			.body("search", hasSize(0));
		
		LOGGER.info("Completed Test_SearchWithDescendingLimitZero");
	}

	@Test(dependsOnMethods="Test_GetAccessToken",description="Testing search functionality using limit 0")
	public void Test_SearchWithLimitZero() throws Exception {
		LOGGER.info("Started Test_SearchWithLimitZero");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(searchURI + "?limit=0")
			.prettyPeek()
		.then()
			.statusCode( 200 )
			.body("search", hasSize(0));
		
		LOGGER.info("Completed Test_SearchWithLimitZero");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Testing search functionality with sort as ascending and limit 0")
	public void Test_SearchWithAscendingLimitZero() throws Exception {
		LOGGER.info("Started Test_SearchWithAscendingLimitZero");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(searchURI + "?sort=asc&limit=0")
			.prettyPeek()
		.then()
			.statusCode( 200 )
			.body("search", hasSize(0));
		
		LOGGER.info("Completed Test_SearchWithAscendingLimitZero");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Testing search functionality using number and limit 0")
	public void Test_SearchWithSearchLimitZero() throws Exception {
		LOGGER.info("Started Test_SearchWithSearchLimitZero");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(searchURI + "?query=123&limit=0")
			.prettyPeek()
		.then()
			.statusCode( 200 )
			.body("search", hasSize(0));
		
		LOGGER.info("Completed Test_SearchWithSearchLimitZero");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Testing search functionality using number and limit 4")
	public void Test_SearchWithSearchLimitFour() throws Exception {
		LOGGER.info("Started Test_SearchWithSearchLimitFour");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(searchURI + "?query=123&limit=4")
			.prettyPeek()
		.then()
			.statusCode( 200 )
			.body("search", hasSize(4));
		
		LOGGER.info("Completed Test_SearchWithSearchLimitFour");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Testing search functionality using number and limit 1")
	public void Test_SearchWithSearchLimitOne() throws Exception {
		LOGGER.info("Started Test_SearchWithSearchLimitOne");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(searchURI + "?query=123&limit=1")
			.prettyPeek()
		.then()
			.statusCode( 200 )
			.body("search", hasSize(1));
		
		LOGGER.info("Completed Test_SearchWithSearchLimitOne");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Testing search functionality using number and limit 5")
	public void Test_SearchWithSearchLimitFive() throws Exception {
		LOGGER.info("Started Test_SearchWithSearchLimitFive");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(searchURI + "?query=123&limit=5")
			.prettyPeek()
		.then()
			.statusCode( 200 )
			.body("search", hasSize(5));
		
		LOGGER.info("Completed Test_SearchWithSearchLimitFive");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Testing search functionality using number and limit 10")
	public void Test_SearchWithSearchLimitTen() throws Exception {
		LOGGER.info("Started Test_SearchWithSearchLimitTen");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(searchURI + "?query=123&limit=10")
			.prettyPeek()
		.then()
			.statusCode( 200 )
			.body("search", hasSize(5));
		
		LOGGER.info("Completed Test_SearchWithSearchLimitTen");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Testing search functionality using number and limit 11")
	public void Test_SearchWithSearchLimitEleven() throws Exception {
		LOGGER.info("Started Test_SearchWithSearchLimitEleven");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(searchURI + "?query=123&limit=11")
			.prettyPeek()
		.then()
			.statusCode( 422 );
		
		LOGGER.info("Completed Test_SearchWithSearchLimitEleven");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Testing search functionality with max text capacity")
	public void Test_SearchWithSearchMaxText() throws Exception {
		LOGGER.info("Started Test_SearchWithSearchMaxText");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(searchURI + "?query=http://interview-testing-api.webdamdb.com/images/r")
			.prettyPeek()
		.then()
			.statusCode( 200 )
			.body("search", hasSize(2))
			.body( "get(0).asset_id", equalTo("12344") )
			.body( "get(1).asset_id", equalTo("12343") );
		
		LOGGER.info("Completed Test_SearchWithSearchMaxText");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Testing search functionality with max capacity for query")
	public void Test_SearchWithSearchAboveMaxText() throws Exception {
		LOGGER.info("Started Test_SearchWithSearchAboveMaxText");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(searchURI + "?query=http://interview-testing-api.webdamdb.com/images/re")
			.prettyPeek()
		.then()
			.statusCode( 422 );
		
		LOGGER.info("Completed Test_SearchWithSearchAboveMaxText");
	}
	
		@Test(dependsOnMethods="Test_GetAccessToken",description="No parameters sent.Negative test")
	public void Test_ImageValidation() throws Exception {
		LOGGER.info( "Started Test_ImageValidation" );
		Response response =
			given()
				.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
			.when()
				.get(searchURI)
				.prettyPeek();
		
		JSONArray values = new JSONArray(response.getBody().asString().trim());

		for(int i = 0; i < values.length(); i++) {
		  String thumbnailURL = values.getJSONObject(i).getString("thumbnail");
		  LOGGER.info( "Thumbnail URL " + i + ": " + thumbnailURL);
		  
		  Response imageResponse = given()
				  					.when()
				  						.get(thumbnailURL);
		  
		  softAssert.assertEquals( imageResponse.getStatusCode(), 200, "Assertion failed for thumbnail URL " + i + ": " + thumbnailURL);
		  	  
		}
		
		softAssert.assertAll();
		LOGGER.info( "Completed Test_ImageValidation" );
	}
	
	//new tests here
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Testing search functionality with limit as negative")
	public void Test_SearchWithDescendingNegativeLimit() throws Exception {
		LOGGER.info("Started Test_SearchWithDescendingLimitZero");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(searchURI + "?sort=desc&limit=-1")
			.prettyPeek()
		.then()
			.statusCode( 400 );
		
		LOGGER.info("Completed Test_SearchWithDescendingNegativeLimit");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Testing search functionality based on order of parameters.")
	public void Test_SearchOrderOfParameters() throws Exception {
		LOGGER.info("Started Test_SearchOrderOfParameters");
		Response firstResponse = given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(searchURI + "?query=human&limit=3&sort=asc");
		
		Response secondResponse = given()
				.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
			.when()
				.get(searchURI + "?query=human&sort=asc&limit=3");
		
		Response thirdResponse = given()
				.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
			.when()
				.get(searchURI + "?limit=3&query=human&sort=asc");
		
		Response fourthResponse = given()
				.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
			.when()
				.get(searchURI + "?limit=3&sort=asc&query=human");
		
		Response fifthResponse = given()
				.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
			.when()
				.get(searchURI + "?sort=asc&query=human&limit=3");
		
		Response sixthResponse = given()
				.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
			.when()
				.get(searchURI + "?sort=asc&limit=3&query=human");
		
		firstResponse.then().statusCode(200);
		secondResponse.then().statusCode(200);
		thirdResponse.then().statusCode(200);
		fourthResponse.then().statusCode(200);
		fifthResponse.then().statusCode(200);
		sixthResponse.then().statusCode(200);
		
		Assert.assertEquals(firstResponse.getBody().asString(), secondResponse.getBody().asString());
		Assert.assertEquals(secondResponse.getBody().asString(), thirdResponse.getBody().asString());
		Assert.assertEquals(thirdResponse.getBody().asString(), fourthResponse.getBody().asString());
		Assert.assertEquals(fourthResponse.getBody().asString(), fifthResponse.getBody().asString());
		Assert.assertEquals(fifthResponse.getBody().asString(), sixthResponse.getBody().asString());
		
		LOGGER.info("Completed Test_SearchOrderOfParameters");
	}	
  
	@Test(dependsOnMethods="Test_GetAccessToken",description="Testing search functionality by alternative sort parameters (a & d Vs asc & desc)")
	public void Test_SearchSortParameters_A_D() throws Exception {
		LOGGER.info("Started Test_SearchSortParameters_A_D");
		Response aResponse = given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(searchURI + "?sort=a");
		
		Response dResponse = given()
				.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
			.when()
				.get(searchURI + "?sort=d");
		
		aResponse.prettyPeek()
		.then()
			.statusCode( 422 );;
		
		dResponse.prettyPeek()
		.then()
			.statusCode( 422 );
		
		Assert.assertNotEquals( aResponse, dResponse );
		
		LOGGER.info("Completed Test_SearchSortParameters_A_D");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Testing search functionality by alternative sort parameters (ascending and descending, vs asc & desc")
	public void Test_SearchSortParameters_Ascending_Descending() throws Exception {
		LOGGER.info("Started Test_SearchSortParameters_Ascending_Descending");
		Response ascendingResponse = given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(searchURI + "?sort=ascending");
		
		Response descendingResponse = given()
				.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
			.when()
				.get(searchURI + "?sort=descending");
		
		ascendingResponse.prettyPeek()
		.then()
			.statusCode( 422 );
		
		descendingResponse.prettyPeek()
		.then()
			.statusCode( 422 );
		
		Assert.assertEquals( ascendingResponse.getBody().asString(), descendingResponse.getBody().asString());
		
		LOGGER.info("Completed Test_SearchSortParameters_Ascending_Descending");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Exercise the asset API with an alphabetical ID.")
	public void Test_GetAssetByAlphabeticalID() throws Exception {
		LOGGER.info("Started Test_GetAssetByAlphabeticalID");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(assetURI + "/abcde")
			.prettyPeek()
		.then()
			.statusCode(422)
			.body("$", empty());

		LOGGER.info("Completed Test_GetAssetByAlphabeticalID");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Testing search functionality by alternative sort parameters (a & d, vs asc & desc")
	public void Test_SearchAlphabeticalLimit() throws Exception {
		LOGGER.info("Started Test_SearchAlphabeticalLimit");
		given()
				.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
			.when()
				.get(searchURI + "?limit=abcde")
				.prettyPeek()
			.then()
				.statusCode( 422 );

		LOGGER.info("Completed Test_SearchAlphabeticalLimit");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Exercise the asset API with multiple IDs seperated with &.")
	public void Test_GetMultipleAssetsWithAmpersand() throws Exception {
		LOGGER.info("Started Test_GetMultipleAssetsWithAmpersand");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(assetURI + "/12345&12344")
			.prettyPeek()
		.then()
			.statusCode(422);

		LOGGER.info("Completed Test_GetMultipleAssetsWithAmpersand");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Exercise the asset API with multiple IDs seoearted with ,.")
	public void Test_GetMultipleAssetsWithComma() throws Exception {
		LOGGER.info("Started Test_GetMultipleAssetsWithComma");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(assetURI + "/12345,12344")
			.prettyPeek()
		.then()
			.statusCode(422);
		
		LOGGER.info("Completed Test_GetMultipleAssetsWithComma");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Testing search functionality with text from two assets")
	public void Test_SearchWithLongQueryFromTwoAssets() throws Exception {
		LOGGER.info("Started Test_SearchWithLongQueryFromTwoAssets");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(searchURI + "?query=1986 is one of a kind Esa es una bonita Manzana&sort=asc")
			.prettyPeek()
		.then()
			.statusCode( 200 )
			.body("$", empty());
		
		LOGGER.info("Completed Test_SearchWithLongQueryFromTwoAssets");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Testing search functionality with negative sort")
	public void Test_SearchWithNegativeSort() throws Exception {
		LOGGER.info("Started Test_SearchWithNegativeSort");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(searchURI + "?query=1986&sort=-1")
			.prettyPeek()
		.then()
			.statusCode( 422 );
		
		LOGGER.info("Completed Test_SearchWithNegativeSort");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Testing search functionality with a number for the sort")
	public void Test_SearchWithNumberSort() throws Exception {
		LOGGER.info("Started Test_SearchWithNumberSort");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(searchURI + "?query=1986&sort=3")
			.prettyPeek()
		.then()
			.statusCode( 422 );
		
		LOGGER.info("Completed Test_SearchWithNumberSort");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Exercise the asset API with a partial ID.")
	public void Test_GetAssetByPartialID() throws Exception {
		LOGGER.info("Started Test_GetAssetByPartialID");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(assetURI + "/123")
			.prettyPeek()
		.then()
			.statusCode(204)
			.body( "$", empty() );

		LOGGER.info("Completed Test_GetAssetByPartialID");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Exercise the asset API with a partial ID and asterisk.")
	public void Test_GetAssetByPartialIDAsterisk() throws Exception {
		LOGGER.info("Started Test_GetAssetByPartialIDAsterisk");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(assetURI + "/123*")
			.prettyPeek()
		.then()
			.statusCode(200)
			.body( "search", hasSize(5) );

		LOGGER.info("Completed Test_GetAssetByPartialIDAsterisk");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Exercise the asset API with a negative ID.")
	public void Test_GetAssetByNegativeID() throws Exception {
		LOGGER.info("Started Test_GetAssetByNegativeID");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(assetURI + "/-12345")
			.prettyPeek()
		.then()
			.statusCode(422);

		LOGGER.info("Completed Test_GetAssetByNegativeID");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Testing search functionality with no value for each parameter")
	public void Test_SearchWithEmptyParameters() throws Exception {
		LOGGER.info("Started Test_SearchWithEmptyParameters");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(searchURI + "?query=&sort=&limit=")
			.prettyPeek()
		.then()
			.statusCode( 422 );
			//.body("search", hasSize(5));
		
		LOGGER.info("Completed Test_SearchWithEmptyParameters");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Testing search functionality with no value for query parameter")
	public void Test_SearchWithEmptyQuery() throws Exception {
		LOGGER.info("Started Test_SearchWithEmptyQuery");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(searchURI + "?query=")
			.prettyPeek()
		.then()
			.statusCode( 422 );
		
		LOGGER.info("Completed Test_SearchWithEmptyQuery");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Testing search functionality with no value for sort parameter")
	public void Test_SearchWithEmptySort() throws Exception {
		LOGGER.info("Started Test_SearchWithEmptySort");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(searchURI + "?sort=")
			.prettyPeek()
		.then()
			.statusCode( 422 );
		
		LOGGER.info("Completed Test_SearchWithEmptySort");
	}
	
	@Test(dependsOnMethods="Test_GetAccessToken",description="Testing search functionality with no value for limit parameter.")
	public void Test_SearchWithEmptyLimit() throws Exception {
		LOGGER.info("Started Test_SearchWithEmptyLimit");
		given()
			.headers( "Accept", JSON, "Authorization", "Bearer "+accessTokenValue)
		.when()
			.get(searchURI + "?limit=")
			.prettyPeek()
		.then()
			.statusCode( 422 );
		
		LOGGER.info("Completed Test_SearchWithEmptyLimit");
	}
	*/
}
