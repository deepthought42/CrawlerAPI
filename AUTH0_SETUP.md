# Auth0 Configuration

This project has been restored with Auth0 authentication support. To configure Auth0 for your application, follow these steps:

## 1. Auth0 Application Setup

1. Create an Auth0 account at https://auth0.com
2. Create a new Application in your Auth0 dashboard
3. Configure the following settings:
   - **Application Type**: Single Page Application (SPA) or Regular Web Application
   - **Allowed Callback URLs**: Add your application's callback URL
   - **Allowed Logout URLs**: Add your application's logout URL
   - **Allowed Web Origins**: Add your application's domain

## 2. API Configuration

1. Create a new API in your Auth0 dashboard
2. Set the identifier to `https://api.your-domain.com` (or your preferred API identifier)
3. Enable RS256 signing algorithm
4. Note down the API identifier for configuration

## 3. Application Configuration

Update the `src/main/resources/auth0.properties` file with your Auth0 credentials:

```properties
auth0.domain:your-domain.auth0.com
auth0.issuer:https://your-domain.auth0.com/
auth0.apiAudience:https://api.your-domain.com

auth0.clientId:your-client-id
auth0.clientSecret:your-client-secret

auth0.audience:https://api.your-domain.com
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://your-domain.auth0.com/
spring.security.oauth2.client.registration.auth0.client-id=your-client-id
spring.security.oauth2.client.registration.auth0.client-secret=your-client-secret

auth0.securedRoute: /*
auth0.base64EncodedSecret: false
auth0.authorityStrategy: ROLES
auth0.defaultAuth0ApiSecurityEnabled: true
auth0.signingAlgorithm: RS256
```

## 4. Management API (Optional)

If you need to use the Auth0 Management API for user management:

1. Create a Machine-to-Machine application in Auth0
2. Authorize it to access the Auth0 Management API
3. Update the `Auth0ManagementApi` class with your Management API credentials

## 5. Security Configuration

The application is configured to:
- Use JWT tokens for authentication
- Validate tokens against your Auth0 domain
- Require authentication for all endpoints except:
  - `/actuator/info`
  - `/actuator/health`
  - `/auditor/start-individual`
  - `/audits`

## 6. Testing

To test the Auth0 integration:

1. Start your application
2. Try to access a protected endpoint without a valid JWT token
3. You should receive a 401 Unauthorized response
4. Include a valid JWT token in the Authorization header: `Bearer <your-jwt-token>`

## 7. User ID Handling

The application automatically handles Auth0 user IDs by removing the `auth0|` prefix from user IDs when looking up accounts in the database.

## 8. Troubleshooting

- Ensure your Auth0 domain is correct
- Verify that your API identifier matches the audience in your JWT tokens
- Check that your application has the correct permissions in Auth0
- Verify that your JWT tokens are signed with RS256 algorithm

## 9. Environment Variables

For production deployments, consider using environment variables instead of hardcoded values:

```properties
auth0.domain:${AUTH0_DOMAIN}
auth0.clientId:${AUTH0_CLIENT_ID}
auth0.clientSecret:${AUTH0_CLIENT_SECRET}
auth0.apiAudience:${AUTH0_API_AUDIENCE}
``` 