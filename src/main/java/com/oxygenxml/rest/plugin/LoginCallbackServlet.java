package com.oxygenxml.rest.plugin;

import static com.oxygenxml.rest.plugin.authn.RestApplicationAuthenticationProvider.REST_IS_AUTH_REDIRECT;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.oxygenxml.rest.plugin.authn.UserAuthenticator;

import lombok.extern.slf4j.Slf4j;
import ro.sync.ecss.extensions.api.webapp.SessionStore;
import ro.sync.ecss.extensions.api.webapp.access.InternalWebappPluginWorkspace;
import ro.sync.ecss.extensions.api.webapp.plugin.WebappServletPluginExtension;
import ro.sync.exml.plugin.PluginContext;
import ro.sync.exml.workspace.api.PluginResourceBundle;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

/**
 * Servlet that represents a servlet to which the REST server will redirect to when the login process was completed.
 *  
 * @author mihai_coanda
 */
@Slf4j
public class LoginCallbackServlet extends WebappServletPluginExtension {
  
  /**
   * The headers used for authentication
   */
  @PluginContext
  private AuthHeadersMap authHeadersMap;
  
  /**
   * Returns a HTML page that posts a message to the WebAuthor frame to close the login dialog. 
   */
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String sessionId = req.getSession().getId();
    Cookie[] cookies = req.getCookies();
    if (cookies != null) {
      StringBuilder storedCookies = new StringBuilder();
      StringBuilder obfuscatedCookies = new StringBuilder();
      for (int i = 0; i < cookies.length; i++) {
        Cookie cookie = cookies[i];
        String cookieValue = cookie.getValue();
        String cookieName = cookie.getName();
        if (log.isDebugEnabled()) {
        	obfuscatedCookies.append(cookieName).append("=").append("******").append(";");
        }
        storedCookies.append(cookieName).append("=").append(cookieValue).append(";");
      }
      log.debug("Cookies for session: {}, cookies: {}", sessionId, obfuscatedCookies);
      authHeadersMap.setCookiesHeader(sessionId, storedCookies.toString());
    } else {
      authHeadersMap.clearCookiesHeader(sessionId);
    }
    
    boolean wasAuthenticationRedirect = authenticateUserIfNeeded(sessionId);
    if (wasAuthenticationRedirect) {
      resp.sendRedirect("../app/authenticate.html");
    } else {
      writeIframeContentToFinishLogin(resp);
    }
  }

  /**
   * Writes the content of the iframe that will close the login dialog.
   * @param resp The response
   * @throws IOException If the content cannot be written.
   */
  private void writeIframeContentToFinishLogin(HttpServletResponse resp) throws IOException {
    InternalWebappPluginWorkspace pluginWorkspace = (InternalWebappPluginWorkspace)PluginWorkspaceProvider.getPluginWorkspace();
    PluginResourceBundle rb = pluginWorkspace.getResourceBundle();
    StringBuilder callbackContent = new StringBuilder();
    callbackContent
      .append("<html><head><script>")
      .append("if (window.self !== window.top) {\n")
      .append("  parent.postMessage("
          // the object passed to the parent window.
          + "{\"action\" : \"login-finished\", \"message\": \"" + rb.getMessage(TranslationTags.REST_LOGIN_SUCCESS) + "\"},"
          // the parent window URL regexp 
          + "'*');")
      .append("</script></head></html>");
    // respond to a page that posts a message to the web author page to close.
    resp.setContentType("text/html;charset=UTF-8");
    resp.getWriter().print(callbackContent);
  }

  /**
   * If the user comes back here from the authentication process, mark it as authenticated.
   * 
   * @param sessionId The session id.
   * 
   * @return <code>true</code> if the user was redirected here from the authentication process.
   * @throws IOException If the user cannot be authenticated
   */
  private boolean authenticateUserIfNeeded(String sessionId) throws IOException {
    InternalWebappPluginWorkspace pluginWorkspace = (InternalWebappPluginWorkspace)PluginWorkspaceProvider.getPluginWorkspace();
    SessionStore sessionStore = pluginWorkspace.getSessionStore();
    boolean isAuthRedirect = Boolean.TRUE.equals(sessionStore.getAndDel(sessionId, REST_IS_AUTH_REDIRECT));
    if (isAuthRedirect) {
      UserAuthenticator userAuthenticator = new UserAuthenticator(
          authHeadersMap, 
          pluginWorkspace.getApplicationAuthenticationManager());
      userAuthenticator.authenticateUser(sessionId);
    }
    return isAuthRedirect;
  }

  @Override
  public String getPath() {
    return "rest-login-callback";
  }
}
