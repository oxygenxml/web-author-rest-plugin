package com.oxygenxml.rest.plugin;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

import ro.sync.ecss.extensions.api.webapp.plugin.URLStreamHandlerWithContext;
import ro.sync.exml.options.Options;
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
    URL httpUrl = new URL(url.toExternalForm().substring(RestURLConnection.REST_PROTOCOL_PREFIX.length()));
    String encodedDocumentURL = URLUtil.encodeURIComponent(httpUrl.toExternalForm());
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
    if(serverUrl.isEmpty()) {
      // TODO: handle the case when the REST Server URL option is not set.
    }
    
    return serverUrl;
  }
}