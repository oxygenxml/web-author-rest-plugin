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
    String action = req.getParameter("action");
    String sessionId = req.getSession().getId();

    if("logout".equals(action)) {
      RestURLConnection.credentialsMap.invalidate(sessionId);

    } else {
      Enumeration<String> headers = req.getHeaderNames();
      Map<String, String> headersMap = RestURLConnection.credentialsMap.getIfPresent(sessionId);
      if (headersMap == null) {
        headersMap = new HashMap<String, String>();
        RestURLConnection.credentialsMap.put(sessionId, headersMap);
      }

      while (headers.hasMoreElements()) {
        String headerName = headers.nextElement();
        String headerValue = req.getHeader(headerName);

        headersMap.put(headerName, headerValue);
      }
    }
    resp.setStatus(HttpServletResponse.SC_OK);
  }

  @Override
  public String getPath() {
    return "rest-login";
  }
}
