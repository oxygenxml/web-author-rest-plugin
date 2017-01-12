package com.oxygenxml.rest.plugin;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Map;

import ro.sync.ecss.extensions.api.webapp.plugin.URLStreamHandlerWithContext;
import ro.sync.ecss.extensions.api.webapp.plugin.UserContext;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.options.WSOptionsStorage;
import ro.sync.util.URLUtil;

/**
 * URL stream handler for a webdav server.
 * 
 * @author mihai_coanda
 */
public class RestURLStreamHandler  extends URLStreamHandlerWithContext {
  
  @Override
  protected String getContextId(UserContext context) {
    String contextId = super.getContextId(context);
    
    StringBuilder cookies = new StringBuilder();
    for (Map.Entry<String, String> cookie: context.getCookies().entrySet()) {
      cookies.append(cookie.getKey()).append('=').append(cookie.getValue()).append("; ");
    }
    Map<String, String> headersMap = Collections.singletonMap("Cookie", cookies.toString()); 
    RestURLConnection.credentialsMap.put(contextId, headersMap);
    return contextId;
  }

  @Override
  protected URLConnection openConnectionInContext(String contextId, URL url, Proxy proxy) throws IOException {

    URLConnection urlConnection = computeRestUrl(url).openConnection();
    return new RestURLConnection(contextId, urlConnection);
  }
  
  /**
   * Computes the new connection from the 
   * @param urlConnection
   * @return
   * @throws MalformedURLException whether something fails.
   */
  private static URL computeRestUrl(URL url) throws MalformedURLException {
    // remove the "rest-" protocol prefix.
    String encodedDocumentURL = URLUtil.encodeURIComponent(url.toExternalForm());
    String restUrl = getServerUrl() + "files/?url=" + encodedDocumentURL;
    return new URL(restUrl);
  }
  
  /**
   * Getter for the server's base rest url.
   * 
   * @return the server URL.
   */
  public static String getServerUrl() {
    WSOptionsStorage optionsStorage = PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage();
    String serverUrl = optionsStorage.getOption(RestConfigExtension.REST_SERVER_URL, "");
    // TODO: throw a not-configured exception so that calling methods handle this case.
    return serverUrl;
  }
}
