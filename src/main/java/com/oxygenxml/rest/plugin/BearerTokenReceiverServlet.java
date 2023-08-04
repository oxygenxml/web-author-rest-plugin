package com.oxygenxml.rest.plugin;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ro.sync.ecss.extensions.api.webapp.plugin.WebappServletPluginExtension;
import ro.sync.exml.plugin.PluginContext;

public class BearerTokenReceiverServlet extends WebappServletPluginExtension {
  
  /**
   * The headers used for authentication
   */
  @PluginContext
  private AuthHeadersMap authHeadersMap;
  
  /**
   * Saves the bearer token in the headers map.
   */
  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String sessionId = req.getSession().getId();
    String token = req.getParameter("token");
    
    authHeadersMap.setBearerToken(sessionId, token);
    resp.setStatus(200);
  }

  @Override
  public String getPath() {
    return "rest-bearer-token";
  }
}
