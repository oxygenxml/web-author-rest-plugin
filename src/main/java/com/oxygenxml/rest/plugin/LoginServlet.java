package com.oxygenxml.rest.plugin;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ro.sync.ecss.extensions.api.webapp.plugin.WebappServletPluginExtension;

public class LoginServlet extends WebappServletPluginExtension{
  
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String serverUrl = req.getParameter("server");
    String action = req.getParameter("action");
    String sessionId = req.getSession().getId();

    if("logout".equals(action)) {
      RestURLConnection.serversMap.invalidate(sessionId);

    } else {
      Enumeration<String> headers = req.getHeaderNames();
      Map<String, Map<String, String>> userCredentials = RestURLConnection.serversMap.getIfPresent(sessionId);
      if (userCredentials == null) {
        userCredentials = new HashMap<String, Map<String, String>>();
        RestURLConnection.serversMap.put(sessionId, userCredentials);
      }
      String serverId = RestURLConnection.computeServerId(serverUrl);

      Map<String, String> serverHeadersMap = new HashMap<String, String>();
      userCredentials.put(serverId, serverHeadersMap);

      while (headers.hasMoreElements()) {
        String headerName = headers.nextElement();
        String headerValue = req.getHeader(headerName);

        serverHeadersMap.put(headerName, headerValue);
      }
    }
    resp.setStatus(HttpServletResponse.SC_OK);
  }

  @Override
  public String getPath() {
    return "rest-login";
  }
}
