package com.oxygenxml.rest.plugin;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ro.sync.ecss.extensions.api.webapp.access.WebappPluginWorkspace;
import ro.sync.ecss.extensions.api.webapp.plugin.WebappServletPluginExtension;
import ro.sync.exml.workspace.api.PluginResourceBundle;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

/**
 * Servlet that represents a servlet to which the REST server will redirect to when the login process was completed.
 *  
 * @author mihai_coanda
 */
public class LoginCallbackServlet extends WebappServletPluginExtension {
  
  /**
   * Returns a HTML page that posts a message to the WebAuthor frame to close the login dialog. 
   */
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    StringBuilder callbackContent = new StringBuilder();
    PluginResourceBundle rb = ((WebappPluginWorkspace)PluginWorkspaceProvider.getPluginWorkspace()).getResourceBundle();
    callbackContent
      .append("<html><head><script>")
      .append("parent.postMessage("
          // the object passed to the parent window.
          + "{\"action\" : \"login-finished\", \"message\": \"" + rb.getMessage(TranslationTags.LOGIN_SUCCESS) + "\"},"
          // the parent window URL regexp 
          + "'*');")
      .append("</script></head></html>");
    // respond to a page that posts a message to the web author page to close.
    resp.getWriter().print(callbackContent);
  }

  @Override
  public String getPath() {
    return "rest-login-callback";
  }
}
