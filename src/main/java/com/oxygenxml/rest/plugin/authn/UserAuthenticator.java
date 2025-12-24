package com.oxygenxml.rest.plugin.authn;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oxygenxml.rest.plugin.AuthHeadersMap;
import com.oxygenxml.rest.plugin.RestURLStreamHandler;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import ro.sync.ecss.webapp.auth.ApplicationAuthenticationManager;
import ro.sync.ecss.webapp.auth.ApplicationUser;
import ro.sync.ecss.webapp.auth.ApplicationUserStore;
import ro.sync.ecss.webapp.auth.ApplicationUserWithToken;
import ro.sync.net.protocol.http.HttpExceptionWithDetails;

@Slf4j
@AllArgsConstructor
public class UserAuthenticator {
  /**
   * The link to the specification.
   */
  private static final String SPECIFICATION_LINK = "https://github.com/oxygenxml/web-author-rest-plugin/blob/master/docs/API-spec.md#authentication";
  
  private AuthHeadersMap authHeadersMap;
  
  private ApplicationAuthenticationManager applicationAuthenticationManager;
  
  /**
   * Reads the user details from the server.
   * @param sessionId The session id
   * @return The user details.
   * @throws IOException If the user details cannot be read.
   */
  private UserDetails readUserDetailsFromServer(String sessionId) throws IOException{
    URL meEndpoint = getMeEndpoint();
    URLConnection meEndpointConnection = meEndpoint.openConnection();
    authHeadersMap.addHeaders(sessionId, meEndpointConnection);
    
    try (InputStream inputStream = meEndpointConnection.getInputStream()) {
      return new ObjectMapper().readValue(inputStream, UserDetails.class);
    }
  }

  

  /**
   * @return The URL of the /me endpoint.
   */
  private URL getMeEndpoint() {
    try {
      String baseUrl = RestURLStreamHandler.getServerUrl();
      if (!baseUrl.endsWith("/")) {
        baseUrl += "/";
      }
      return new URL(baseUrl + "me");
    } catch (MalformedURLException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Authenticates the user.
   * @param sessionId The session id.
   * @param userDetails The user details.
   */
  private void markUserAsAuthenticated(String sessionId, UserDetails userDetails) {
    ApplicationUser applicationUser = new ApplicationUser(
        userDetails.getId(), userDetails.getName(), userDetails.getEmail());
    
    ApplicationUserStore applicationUserStore = 
        applicationAuthenticationManager.getApplicationUserStore();
    applicationUserStore.authenticateApplicationUser(sessionId, applicationUser);
  }

  
  public void authenticateUser(String sessionId) {
    try {
      UserDetails userDetails = readUserDetailsFromServer(sessionId);
      markUserAsAuthenticated(sessionId, userDetails);
    } catch (IOException e) {
      if (!this.isUnauthorizedException(e)) {
        log.error("Failed to authenticate user. "
            + "Make sure the \"/me\" endpoint is implemented according to the specification: " + SPECIFICATION_LINK,
            e);
      } else {
        log.error("Failed to authenticate user.", e);
      }
    }
  }

  /**
   * Authenticate the user for the current request using the provided bearer token.
   *
   * @param bearerToken The bearer token.
   *
   * @return The authenticated user enriched with the token, or empty if authentication failed.
   */
  public Optional<ApplicationUserWithToken> authenticateUserForCurrentRequest(String bearerToken) {
    if (bearerToken == null || bearerToken.isEmpty()) {
      return Optional.empty();
    }
    
    try {
      UserDetails userDetails = readUserDetailsUsingBearerToken(bearerToken);
      ApplicationUserWithToken user = new ApplicationUserWithToken(
          userDetails.getId(), userDetails.getName(), userDetails.getEmail(), bearerToken);
      return Optional.of(user);
    } catch (IOException e) {
      if (!this.isUnauthorizedException(e)) {
        log.error("Failed to authenticate user for current request. "
            + "Make sure the \"/me\" endpoint is implemented according to the specification: " + SPECIFICATION_LINK,
            e);
      } else {
        log.error("Failed to authenticate user for current request.", e);
      }
    }
    return Optional.empty();
  }

  /**
   * Checks if the exception is an unauthorized exception
   * @param e The exception.
   * @param sessionId The session id. 
   */
  private boolean isUnauthorizedException(IOException e) {
    if (e instanceof HttpExceptionWithDetails) {
      HttpExceptionWithDetails httpException = (HttpExceptionWithDetails) e;
      return httpException.getReasonCode() == 401;
    }
    return false;
  }
  
  @Data
  private static class UserDetails {
    String id;
    String email;
    String name;
  }

  /**
   * Reads the user details using the provided bearer token.
   *
   * @param bearerToken The bearer token.
   *
   * @return The user details read from the server.
   *
   * @throws IOException If the user cannot be read.
   */
  private UserDetails readUserDetailsUsingBearerToken(String bearerToken) throws IOException {
    URL meEndpoint = getMeEndpoint();
    HttpURLConnection meEndpointConnection = (HttpURLConnection) meEndpoint.openConnection();
    meEndpointConnection.setRequestProperty("Authorization", "Bearer " + bearerToken);
    try (InputStream inputStream = meEndpointConnection.getInputStream()) {
      return new ObjectMapper().readValue(inputStream, UserDetails.class);
    }
  }
}
  
