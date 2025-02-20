package com.oxygenxml.rest.plugin.authn;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.After;
import org.mockito.ArgumentMatcher;

import com.oxygenxml.rest.plugin.AuthHeadersMap;
import com.oxygenxml.rest.plugin.HttpServerManager;

import ro.sync.ecss.webapp.auth.ApplicationAuthenticationManager;
import ro.sync.ecss.webapp.auth.ApplicationUser;
import ro.sync.ecss.webapp.auth.ApplicationUserStore;
import ro.sync.exml.options.OptionTags;
import ro.sync.exml.options.Options;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.options.WSOptionsStorage;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
// import ro.sync.exml.workspace.api.options.Options;
// import ro.sync.exml.workspace.api.options.OptionTags;

public class UserAuthenticatorTest {
  private static final String SESSION_ID = "sessionId";

  private static final String SESSION_COOKIE = "123";

  @Rule
  public HttpServerManager serverManager = new HttpServerManager();

  private UserAuthenticator authenticator;
  private ApplicationAuthenticationManager authManager;
  private ApplicationUserStore userStore;
  private AuthHeadersMap authHeadersMap;
  private HttpServer server;
  private StandalonePluginWorkspace pluginWorkspace;
  private WSOptionsStorage optionsStorage;

  /**
   * This class is used to handle the requests to the server.
   * 
   * It has several endpoints:
   * - /login - receives user & pass and sets a session cookie. The user & pass
   * are not validated.
   * - /me - receives a session cookie and returns a mock user details: id, name
   * and email as JSON.
   */
  private static final class TestAuthEnabledHttpRequestHandler implements HttpRequestHandler {
    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext context)
        throws HttpException, IOException {
      System.out.println("Test - Received request: " + request.getRequestLine().getUri());
      if (request.getRequestLine().getUri().endsWith("/login")) {
        handleLogin(response);
      } else if (request.getRequestLine().getUri().endsWith("/me")) {
        handleMe(request, response);
      } else {
        response.setStatusCode(404);
      }
    }

    private void handleMe(HttpRequest request, HttpResponse response)
        throws IOException {
      // Check if session cookie is the one we set
      Header cookieHeader = request.getFirstHeader("Cookie"); 
      if (cookieHeader == null) {
        System.out.println("Test - No cookie header");
        response.setStatusCode(401);
        return;
      }

      String sessionId = cookieHeader.getValue(); // fix npe
      if (sessionId == null || !sessionId.equals("sessionCookie=" + SESSION_COOKIE)) {
        System.out.println("Test - Invalid sessionId: " + sessionId);
        response.setStatusCode(401);
        return;
      }

      // Handle endpoint that ends in /me
      System.out.println("Test - Handling /me");
      response.setStatusCode(200);
      response.setEntity(new StringEntity(
          "{\"id\": \"123\", \"name\": \"John Doe\", \"email\": \"john@example.com\"}"));
    }

    private void handleLogin(HttpResponse response) {
      response.setHeader("Set-Cookie", "sessionCookie=" + SESSION_COOKIE);
      response.setStatusCode(200);
    }
  }

  @Before
  public void setUp() throws IOException {
    // Start the test server
    server = serverManager.createServerWithDefaulPort(new TestAuthEnabledHttpRequestHandler());

    // Setup mocks
    authManager = mock(ApplicationAuthenticationManager.class);
    userStore = mock(ApplicationUserStore.class);
    when(authManager.getApplicationUserStore()).thenReturn(userStore);

    // Mock PluginWorkspace and OptionsStorage
    pluginWorkspace = mock(StandalonePluginWorkspace.class);
    optionsStorage = mock(WSOptionsStorage.class);
    when(pluginWorkspace.getOptionsStorage()).thenReturn(optionsStorage);
    when(optionsStorage.getOption("rest.server_url", ""))
        .thenReturn("http://localhost:7171/api");

    // Set the mock workspace in PluginWorkspaceProvider
    PluginWorkspaceProvider.setPluginWorkspace(pluginWorkspace);

    // Add test server to trusted hosts
    Options.getInstance().setStringArrayProperty(OptionTags.TRUSTED_HOSTS, 
        new String[] {"localhost:7171"});

    // Create a real AuthHeadersMap
    authHeadersMap = new AuthHeadersMap();

    // Create the authenticator
    authenticator = new UserAuthenticator(authHeadersMap, authManager);
  }

  @After
  public void tearDown() {
    // Clear the trusted hosts after each test
    Options.getInstance().setStringArrayProperty(OptionTags.TRUSTED_HOSTS, new String[] {});
  }

  @Test
  public void testSuccessfulAuthentication() {
    authHeadersMap.setCookiesHeader(SESSION_ID, "sessionCookie=" + SESSION_COOKIE);

    // Test authentication
    authenticator.authenticateUser(SESSION_ID);

    // Verify the user was authenticated with correct details
    verify(userStore).authenticateApplicationUser(
        eq(SESSION_ID),
        argThat(new ArgumentMatcher<ApplicationUser>() {
          @Override
          public boolean matches(Object userObj) {
            ApplicationUser user = (ApplicationUser) userObj;
            return user != null &&
                "123".equals(user.getId()) &&
                "John Doe".equals(user.getName()) &&
                "john@example.com".equals(user.getEmail());
          }
        }));
  }

  @Test
  public void testFailedAuthentication() {

    // Test authentication with invalid session
    authenticator.authenticateUser(SESSION_ID);

    // Verify no user was authenticated
    verify(userStore, never()).authenticateApplicationUser(any(), any());
  }
}
