package com.oxygenxml.rest.plugin;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import ro.sync.ecss.extensions.api.webapp.plugin.WebappServletPluginExtension;

public class LoginCallbackServlet extends WebappServletPluginExtension{
  
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    StringBuilder callbackContent = new StringBuilder();
    callbackContent
      .append("<html><head><script>")

      .append("parent.postMessage("
          // the object passed to the parent window.
          + "{\"action\" : \"login\", \"message\": \"Rest server succeeded in logging in the user\"},"
          // the parent window URL regexp 
          + "'*');")
      .append("</script></head></html>");
    // respond to a page that posts a message to the web author page to close. 
    IOUtils.write(callbackContent.toString(), resp.getOutputStream(), "UTF-8");
  }

  @Override
  public String getPath() {
    return "rest-login-callback";
  }
}
