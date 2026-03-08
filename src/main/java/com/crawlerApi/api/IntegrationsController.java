package com.crawlerApi.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.crawlerApi.integrations.IntegrationFacade;
import com.crawlerApi.integrations.IntegrationMetadata;
import com.crawlerApi.integrations.IntegrationWithConfigResponse;
import com.crawlerApi.integrations.SlackOAuthService;
import com.looksee.exceptions.UnknownAccountException;
import com.looksee.models.Account;
import com.nimbusds.jwt.JWT;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * API for integration configuration and discovery. All endpoints require an authenticated account.
 */
@Controller
@RequestMapping(path = "v1/integrations", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Integrations V1", description = "Integrations API")
public class IntegrationsController extends BaseApiController {

    private static final Logger log = LoggerFactory.getLogger(IntegrationsController.class);

    @Autowired
    private IntegrationFacade integrationFacade;

    @Autowired
    private SlackOAuthService slackOAuthService;

    @RequestMapping(method = RequestMethod.GET)
    @Operation(summary = "List available integrations", description = "Returns metadata for all available integration types")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List of integration metadata"),
        @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @ResponseBody
    public ResponseEntity<List<IntegrationMetadata>> list(HttpServletRequest request) throws UnknownAccountException {
        Account acct = getAuthenticatedAccount(request.getUserPrincipal());
        List<IntegrationMetadata> list = integrationFacade.listAvailableIntegrations();
        return ResponseEntity.ok(list);
    }

    @RequestMapping(path = "/{type}", method = RequestMethod.GET)
    @Operation(summary = "Get integration by type", description = "Returns metadata and current account config (masked) for the given integration type")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Integration metadata and config"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "404", description = "Unknown integration type")
    })
    @ResponseBody
    public ResponseEntity<IntegrationWithConfigResponse> getByType(
            HttpServletRequest request,
            @PathVariable("type") String type) throws UnknownAccountException {
        Account acct = getAuthenticatedAccount(request.getUserPrincipal());
        Optional<IntegrationMetadata> meta = integrationFacade.getMetadata(type);
        if (meta.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Optional<Map<String, Object>> configOpt = integrationFacade.getConfigMasked(acct.getId(), type);
        IntegrationWithConfigResponse body = new IntegrationWithConfigResponse(meta.get(), configOpt.orElse(null));
        return ResponseEntity.ok(body);
    }

    @RequestMapping(path = "/{type}/config", method = RequestMethod.GET)
    @Operation(summary = "Get integration config", description = "Returns current account config for the integration type (for backend use)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Config map"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "404", description = "Unknown integration type or no config")
    })
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getConfig(
            HttpServletRequest request,
            @PathVariable("type") String type) throws UnknownAccountException {
        Account acct = getAuthenticatedAccount(request.getUserPrincipal());
        if (integrationFacade.getProvider(type).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Optional<Map<String, Object>> config = integrationFacade.getConfig(acct.getId(), type);
        return config.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @RequestMapping(path = "/{type}/config", method = RequestMethod.PUT)
    @Operation(summary = "Create or update integration config", description = "Validates and saves config for the current account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Config saved"),
        @ApiResponse(responseCode = "400", description = "Invalid config"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "404", description = "Unknown integration type")
    })
    @ResponseBody
    public ResponseEntity<Void> putConfig(
            HttpServletRequest request,
            @PathVariable("type") String type,
            @RequestBody Map<String, Object> config) throws UnknownAccountException {
        Account acct = getAuthenticatedAccount(request.getUserPrincipal());
        if (integrationFacade.getProvider(type).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if ("slack".equalsIgnoreCase(type)) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
        }
        if (!integrationFacade.putConfig(acct.getId(), type, config)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }

    @RequestMapping(path = "/{type}/config", method = RequestMethod.DELETE)
    @Operation(summary = "Delete integration config", description = "Removes stored config for the current account and integration type")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Config deleted"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "404", description = "Unknown integration type")
    })
    @ResponseBody
    public ResponseEntity<Void> deleteConfig(
            HttpServletRequest request,
            @PathVariable("type") String type) throws UnknownAccountException {
        Account acct = getAuthenticatedAccount(request.getUserPrincipal());
        if (integrationFacade.getProvider(type).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        integrationFacade.deleteConfig(acct.getId(), type);
        return ResponseEntity.ok().build();
    }

    @RequestMapping(path = "/{type}/test", method = RequestMethod.POST)
    @Operation(summary = "Test integration connection", description = "Tests connection using the current account's stored config")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Connection succeeded"),
        @ApiResponse(responseCode = "502", description = "Connection failed"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "404", description = "Unknown integration type or no config")
    })
    @ResponseBody
    public ResponseEntity<Void> testConnection(
            HttpServletRequest request,
            @PathVariable("type") String type) throws UnknownAccountException {
        Account acct = getAuthenticatedAccount(request.getUserPrincipal());
        if (integrationFacade.getProvider(type).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        boolean ok = integrationFacade.testConnection(acct.getId(), type);
        return ok ? ResponseEntity.ok().build() : ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
    }



    @RequestMapping(path = "/slack/connect", method = RequestMethod.GET)
    @Operation(summary = "Start Slack OAuth connect", description = "Returns Slack OAuth authorization URL for click-to-connect user flow")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authorization URL generated"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "503", description = "Slack OAuth not configured")
    })
    @ResponseBody
    public ResponseEntity<Map<String, String>> startSlackConnect(HttpServletRequest request) throws UnknownAccountException {
        Account acct = getAuthenticatedAccount(request.getUserPrincipal());
        try {
            String url = slackOAuthService.buildAuthorizationUrl(acct.getId());
            Map<String, String> response = new HashMap<>();
            response.put("authorizationUrl", url);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException ex) {
            log.warn("Slack OAuth init failed", ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    @RequestMapping(path = "/slack/callback", method = RequestMethod.GET)
    @Operation(summary = "Handle Slack OAuth callback", description = "Exchanges Slack OAuth code for access token and stores integration config")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Slack connected"),
        @ApiResponse(responseCode = "400", description = "Invalid callback payload/state"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "502", description = "Slack token exchange failed"),
        @ApiResponse(responseCode = "503", description = "Slack OAuth not configured")
    })
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleSlackCallback(
            HttpServletRequest request,
            @RequestParam("code") String code,
            @RequestParam("state") String state) throws UnknownAccountException {
        Account acct = getAuthenticatedAccount(request.getUserPrincipal());
        try {
            Map<String, Object> oauthConfig = slackOAuthService.exchangeCodeForConfig(acct.getId(), code, state);
            if (!integrationFacade.putConfig(acct.getId(), "slack", oauthConfig)) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
            }
            Map<String, Object> response = new HashMap<>();
            response.put("connected", true);
            response.put("workspace", oauthConfig.getOrDefault("teamName", ""));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException ex) {
            String msg = ex.getMessage() == null ? "" : ex.getMessage();
            if (msg.contains("not configured")) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
            }
            log.warn("Slack OAuth callback failed", ex);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }
    @RequestMapping(path = "/product-board", method = RequestMethod.POST)
    @Operation(summary = "Create Product Board integration token", description = "Create a JWT token for Product Board integration (backward compatibility)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Integration token created", content = @Content(schema = @Schema(type = "object", description = "JWT"))),
        @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @ResponseBody
    public JWT createProductBoard(HttpServletRequest request, @RequestBody(required = false) Account account) throws UnknownAccountException {
        log.debug("product board integration request");
        Account acct = getAuthenticatedAccount(request.getUserPrincipal());
        // Backward compatibility: JWT creation can be implemented in ProductBoardIntegrationProvider or here
        return null;
    }
}
