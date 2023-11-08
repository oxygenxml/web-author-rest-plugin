package com.oxygenxml.rest.plugin;


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import ro.sync.basic.util.URLUtil;
import ro.sync.ecss.extensions.api.webapp.access.WebappPluginWorkspace;
import ro.sync.ecss.extensions.api.webapp.plugin.URLStreamHandlerWithContext;
import ro.sync.ecss.extensions.api.webapp.plugin.UserContext;
import ro.sync.exml.workspace.api.PluginResourceBundle;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.options.WSOptionsStorage;

/**
 * URL stream handler for a rest server.
 * 
 * @author mihai_coanda
 */
@Slf4j
public class RestURLStreamHandler  extends URLStreamHandlerWithContext {

  /**
   * The environment variable used by the REST plugin to determine the server URL.
   */
  private static final String REST_SERVER_URL_ENV_VAR = "REST_SERVER_URL";
  /**
   * The headers used for authentication
   */
  private AuthHeadersMap authHeadersMap;


  /**
   * Constructor.
   * @param authHeadersMap The headers used for authentication. 
   */
  public RestURLStreamHandler(AuthHeadersMap authHeadersMap) {
    this.authHeadersMap = authHeadersMap;
    String restServerUrlEnvVar = System.getenv(REST_SERVER_URL_ENV_VAR);
    if (restServerUrlEnvVar != null) {
      WSOptionsStorage optionsStorage = PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage();
      String serverUrl = optionsStorage.getOption(RestConfigExtension.REST_SERVER_URL, null);
      if (serverUrl != null && !restServerUrlEnvVar.equals(serverUrl)) {
        log.warn("The \"REST Server URL\" option is overriden by the "
            + "\"" + RestConfigExtension.REST_SERVER_URL + "\" environment variable.");
      }
    }
  }

  @Override
  protected String getContextId(UserContext context) {
    String contextId = super.getContextId(context);
    
    StringBuilder cookies = new StringBuilder();
    for (Map.Entry<String, String> cookie: context.getCookies().entrySet()) {
      cookies.append(cookie.getKey()).append('=').append(cookie.getValue()).append("; ");
    }
    
    authHeadersMap.setCookiesHeader(contextId, cookies.toString());
    return contextId;
  }

  @Override
  protected URLConnection openConnectionInContext(String contextId, URL url, Proxy proxy) throws IOException {
	log.debug("Opening connection in session {} for URL: {}", contextId, url);
    URLConnection urlConnection = computeRestUrl(url).openConnection();
    return new RestURLConnection(authHeadersMap, contextId, urlConnection);
  }
  
  /**
   * Computes the new URL to the REST server from the current connection.
   *  
   * @param url the current URL.
   * 
   * @return the URL to the REST server that represents the current URL.
   * 
   * @throws MalformedURLException whether something fails.
   */
  private static URL computeRestUrl(URL url) throws MalformedURLException {
    String encodedDocumentURL = URLUtil.encodeURIComponent(url.toExternalForm());
    String serverUrl = getServerUrl();
    if(serverUrl != null && !serverUrl.isEmpty()) {
      String restUrl = serverUrl + "files?url=" + encodedDocumentURL;
      log.debug("Computed REST URL: {}", restUrl);
      return new URL(restUrl);
    }
    PluginResourceBundle rb = ((WebappPluginWorkspace)PluginWorkspaceProvider.getPluginWorkspace()).getResourceBundle();
    throw new MalformedURLException(rb.getMessage(TranslationTags.UNCONFIGURED_REST_SERVER_URL));
  }
  
  /**
   * Getter for the server's base rest url.
   * 
   * @return the server URL.
   */
  public static String getServerUrl() {
    String restServerUrlEnvVar = System.getenv(REST_SERVER_URL_ENV_VAR);
    String serverUrl;
    if (restServerUrlEnvVar != null) {
      serverUrl = restServerUrlEnvVar;
    } else {
      WSOptionsStorage optionsStorage = PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage();
      serverUrl = optionsStorage.getOption(RestConfigExtension.REST_SERVER_URL, "");
    }
    
    // if the server URL does not end in '/' we add the '/'
    if(serverUrl != null && !serverUrl.isEmpty() && serverUrl.lastIndexOf('/') != serverUrl.length() - 1) {
      serverUrl = serverUrl + "/";
    }
    
    log.debug("Server URL: {}", serverUrl);
    return serverUrl;
  }
}
