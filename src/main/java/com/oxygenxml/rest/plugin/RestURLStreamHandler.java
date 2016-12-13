package com.oxygenxml.rest.plugin;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

import ro.sync.ecss.extensions.api.webapp.plugin.URLStreamHandlerWithContext;
import ro.sync.util.URLUtil;

/**
 * URL stream handler for a webdav server.
 * 
 * @author mihai_coanda
 */
public class RestURLStreamHandler  extends URLStreamHandlerWithContext {

  
  private static final String VERSION = "1";
  /**
   * The base restURL at which to connect to open and save the document.
   */
//  public static final String REST_BASE_URL = "http://localhost:8081/oxygen-webapp/rest-plugin-base/v" + VERSION  + "/";
  // TODO: delete this configuration
  public static final String REST_BASE_URL = "http://localhost:8081/oxygen-webapp/plugins-dispatcher/rest-mock-server/v" + VERSION  + "/";
  

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
    String encodedDocumentURL = URLUtil.encodeURIComponent(URLUtil.encodeURIComponent(httpUrl.toExternalForm()));
    String restUrl = RestURLStreamHandler.REST_BASE_URL + "files/" + encodedDocumentURL;
    return new URL(restUrl);
  }
}