package com.oxygenxml.rest.plugin;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import ro.sync.ecss.extensions.api.webapp.plugin.WebappServletPluginExtension;
import ro.sync.util.URLUtil;

/**
 * Servlet that retrieves url information for a REST url.
 * 
 * @author mihai_coanda
 */
public class RestURLInfo  extends WebappServletPluginExtension {
  
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String url = req.getParameter("url");
    String sessionId = req.getSession().getId();
    
    URL restURL = new URL(RestURLStreamHandler.REST_BASE_URL + "info/" +
        URLUtil.encodeURIComponent(URLUtil.encodeURIComponent(url)));
    
    // Add the headers to the connection for the authentication.
    URLConnection connection = restURL.openConnection();
    RestURLConnection.addHeaders(connection, sessionId);
    connection.connect();
    IOUtils.copy(connection.getInputStream(), resp.getOutputStream());
  }

  @Override
  public String getPath() {
    return "rest-url-info";
  }
}
