package com.oxygenxml.rest.plugin.authn;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

import com.oxygenxml.rest.plugin.AuthHeadersMap;
import com.oxygenxml.rest.plugin.RestURLStreamHandler;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ro.sync.ecss.extensions.api.webapp.SessionStore;
import ro.sync.ecss.extensions.api.webapp.plugin.UserContext;
import ro.sync.ecss.webapp.auth.ApplicationAuthenticationManager;
import ro.sync.ecss.webapp.auth.ApplicationAuthenticationProvider;
import ro.sync.ecss.webapp.auth.ApplicationUser;
import ro.sync.ecss.webapp.auth.ApplicationUserStore;
import ro.sync.ecss.webapp.auth.ApplicationUserWithToken;

@Slf4j
@AllArgsConstructor
public class RestApplicationAuthenticationProvider implements ApplicationAuthenticationProvider {
  /**
   * Session attribute name that is set before redirecting the user to the CMS login page.
   */
  public static final String REST_IS_AUTH_REDIRECT = "rest.is.auth.running";

  /**
   * The headers used for authentication
   */
  private AuthHeadersMap authHeadersMap;
  
  /**
   * The application authentication manager.
   */
  private ApplicationAuthenticationManager applicationAuthenticationManager;
  
  /**
   * The session store.
   */
  private SessionStore sessionStore;
  
  @Override
  public String getIcon() {
    return "./images/no-sprite/DashboardFilesContainer.png";
  }

  @Override
  public String getName() {
    return "CMS";
  }

  @Override
  public boolean isAskingForAuthorization() {
    return true;
  }

  @Override
  public URL prepareAuthenticationRedirect(String sessionId) {
    sessionStore.putIfAbsentWithoutSessionCookieRefresh(sessionId, REST_IS_AUTH_REDIRECT, true);
    try {
      // Redirect to the CMS login page
      return new URL(RestURLStreamHandler.getServerUrl() + "rest-login");
    } catch (MalformedURLException e) {
      throw new IllegalStateException(e);
    }
  }


  @Override
  public String getCallbackPath() {
    // After users log in with the CMS, they will be redirected to this URL.
    // 
    // After that they will be send to the oxygen.html page, where the 
    // {@link #authenticateWithoutRedirect(UserContext, String)} method will be called.
    return "../plugins-dispatcher/rest-login-callback";
  }
  
  @Override
  public void authenticateWithoutRedirect(UserContext userContext, String pathWithQuery) {
    UserAuthenticator userAuthenticator = new UserAuthenticator(
        authHeadersMap, applicationAuthenticationManager);
    if (registerUserCredentials(userContext)) {
      userAuthenticator.authenticateUser(userContext.getSessionId());
    }
  }

  @Override
  public boolean automaticallyFollowAuthRedirect() {
    return true;
  }

  /**
   * Registers the user credentials in the authHeadersMap.
   * @param userContext The user context.
   * @return <code>true</code> if the user has credentials.
   */
  boolean registerUserCredentials(UserContext userContext) {
    boolean hasCredentials = false;
    
    Map<String, String> cookies = userContext.getCookies();
    if (!cookies.isEmpty()) {
      hasCredentials = true;
      authHeadersMap.setCookiesHeader(userContext.getSessionId(), 
          serializeCookieHeader(cookies));
    }
    
    String authorizationHeader = userContext.getHeaders().get("Authorization");
    if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
      hasCredentials = true;
      String bearerToken = authorizationHeader.substring("Bearer ".length());
      authHeadersMap.setBearerToken(userContext.getSessionId(), bearerToken);
    }
    return hasCredentials;
  }
  
  /**
   * Serializes the cookies to a string.
   * @param cookies The cookies
   * @return The serialized cookies.
   */
  String serializeCookieHeader(Map<String, String> cookies) {
    StringBuilder cookiesHeader = new StringBuilder();
    for (Map.Entry<String, String> cookie : cookies.entrySet()) {
      cookiesHeader.append(cookie.getKey()).append('=').append(cookie.getValue()).append("; ");
    }
    return cookiesHeader.toString();
  }

  @Override
  public Optional<ApplicationUser> authenticateForCurrentRequest(String bearerToken) {
    UserAuthenticator userAuthenticator = new UserAuthenticator(
        authHeadersMap, applicationAuthenticationManager);
    Optional<ApplicationUserWithToken> userOpt = userAuthenticator.authenticateUserForCurrentRequest(bearerToken);
    if (userOpt.isPresent()) {
      ApplicationUserStore userStore = applicationAuthenticationManager.getApplicationUserStore();
      userStore.authenticateApplicationUserForCurrentRequest(userOpt.get());
    }
    return userOpt.map(user -> (ApplicationUser) user);
  }
}
