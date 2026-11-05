package com.oxygenxml.rest.plugin;

import java.io.IOException;

import ro.sync.ecss.extensions.api.webapp.plugin.ServletPluginExtension;
import ro.sync.ecss.extensions.api.webapp.plugin.servlet.ServletException;
import ro.sync.ecss.extensions.api.webapp.plugin.servlet.http.HttpServletRequest;
import ro.sync.ecss.extensions.api.webapp.plugin.servlet.http.HttpServletResponse;
import ro.sync.exml.plugin.PluginContext;

public class BearerTokenReceiverServlet extends ServletPluginExtension {
  
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
