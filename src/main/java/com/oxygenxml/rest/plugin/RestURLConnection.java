package com.oxygenxml.rest.plugin;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.Closeables;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ro.sync.ecss.extensions.api.webapp.WebappMessage;
import ro.sync.ecss.extensions.api.webapp.plugin.FilterURLConnection;
import ro.sync.ecss.extensions.api.webapp.plugin.UserActionRequiredException;
import ro.sync.exml.plugin.urlstreamhandler.CacheableUrlConnection;
import ro.sync.net.protocol.FolderEntryDescriptor;
import ro.sync.util.URLUtil;

/**
 * Wrapper over an URLConnection that reports 401 exceptions as 
 * {@link UserActionRequiredException}.
 * 
 * @author mihai_coanda
 */
public class RestURLConnection extends FilterURLConnection implements CacheableUrlConnection {

  /**
   * Logger for logging.
   */
  private static final Logger logger = Logger.getLogger(RestURLConnection.class.getName());
  
  /**
   * Credentials store.
   */
  public static final Cache<String, Map<String, Map<String, String>>> serversMap =
      CacheBuilder.newBuilder()
        .concurrencyLevel(10)
        .maximumSize(10000)
        .build();
  
  /**
   * Prefix of the protocol.
   * 
   * We translate http to rest-http and https to rest-https.
   */
  public static final String REST_PROTOCOL_PREFIX = "rest-";

  /**
   * The session ID.
   */
  private String contextId;

  /**
   * Constructor method for the URLConnection wrapper.
   * 
   * @param contextId
   *            The session ID.
   * 
   * @param delegate
   *            the wrapped URLConnection.
   * @throws UserActionRequiredException if something fails.
   */
  protected RestURLConnection(String contextId, URLConnection delegate) {
    super(delegate);
    this.contextId = contextId;
    addHeaders(this, this.contextId);
  }
  
  @Override
  public void connect() throws IOException {
    try {
      super.connect();
    } catch (IOException e) {
      handleException(e);
    }
  }

  @Override
  public InputStream getInputStream() throws IOException {
    try {
      return super.getInputStream();
    } catch (IOException e) {
      handleException(e);

      // Unreachable.
      return null;
    }
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    try {
      return new FilterOutputStream(super.getOutputStream()) {
        @Override
        public void close() throws IOException {
          try {
            super.close();
          } catch (IOException e) {
            handleException(e);
          }
        }
      };
    } catch (IOException e) {
      handleException(e);

      // Unreachable.
      return null;
    }
  }

  /**
   * Filters the exceptions.
   * 
   * @param e the exception to filter.
   * 
   * @throws UserActionRequiredException
   *             if the exception message contains a 401 status.
   * 
   * @throws IOException
   *             the param exception if it does not contain a 401 status.
   */
  private void handleException(IOException e) throws UserActionRequiredException, IOException {
    if (e.getMessage().indexOf("401") != -1) {
      // log failed login attempts.
      URL url = this.delegateConnection.getURL();
      String userInfo = url.getUserInfo();
      if (userInfo != null && !userInfo.isEmpty()) {
        String user = URLUtil.extractUser(url.toExternalForm());
        if (user != null && !user.trim().isEmpty()) {
          logger.warn("Failed login attempt of user " + user + " for " + URLUtil.getDescription(url));
        } else {
          logger.warn("Failed login attempt for " + URLUtil.getDescription(url));
        }
      }
      throw new UserActionRequiredException(
          new WebappMessage(WebappMessage.MESSAGE_TYPE_CUSTOM, "Authentication required",
              // send back the URL for which to authenticate.
              this.delegateConnection.getURL().toExternalForm(), true));
    } else {
      if (delegateConnection instanceof HttpURLConnection) {
        String serverMessage = null;
        InputStream errorStream = null;
        try {
          errorStream = ((HttpURLConnection) this.delegateConnection).getErrorStream();
          serverMessage = IOUtils.toString(errorStream);
        } catch (Exception ex) {
          Closeables.closeQuietly(errorStream);
        }
        if (shouldDisplayServerMessage(serverMessage)) {
          throw new IOException(serverMessage, e);
        } else {
          logger.debug("Server message too complex to display to the user");
        }
      }
      throw e;
    }
  }

  /**
   * Decide whether to display the message returned by the REST server.
   * 
   * @param serverMessage
   *            The server message.
   * 
   * @return <code>true</code> if we should display the server message.
   */
  private boolean shouldDisplayServerMessage(String serverMessage) {
    boolean shouldDisplay = true;
    
    if (serverMessage == null) {
      shouldDisplay = false;
    }
    if (serverMessage.contains("<body") || serverMessage.contains("</body")) {
      shouldDisplay = false;
    }
    if (serverMessage.contains("<?xml")) {
      shouldDisplay = false;
    }
    return shouldDisplay;
  }

  /**
   * Adds credentials associated with the given user context to this rest url connection. 
   */
  public static void addHeaders(URLConnection urlConnection, String contextId) {
    Map<String, String> serverHeaders = null;

    Map<String, Map<String, String>> userCredentialsMap = serversMap.getIfPresent(contextId);
    String serverId;
    try {
      serverId = computeServerId(urlConnection.getURL().toExternalForm());
      
      if(userCredentialsMap != null) {
        serverHeaders = userCredentialsMap.get(serverId);
      }
      if(serverHeaders != null) {
        // add all headers to the url connection
        Set<String> keySet = serverHeaders.keySet();
        Iterator<String> keysIterator = keySet.iterator();
        while (keysIterator.hasNext()) {
          String header = keysIterator.next();
          String headerValue = serverHeaders.get(header);
          urlConnection.addRequestProperty(header, headerValue);
        }
      }
    } catch(MalformedURLException e) {
      // the url is from the delegate connection so it is always well formed.
    }
  }
  
  /**
   * Computes a server identifier out of the requested URL.
   * 
   * @param serverUrl the URL string.
   * 
   * @return the server identifier.
   * @throws MalformedURLException if the URL is malformed
   */
  public static String computeServerId(String serverUrl) throws MalformedURLException {
    logger.debug("Server for which to compute the serverID :" + serverUrl);
    URL url = new URL(serverUrl);
    String serverId = url.getProtocol() + url.getHost() + url.getPort();

    // remove the rest prefix from server id.
    if(serverId.startsWith(REST_PROTOCOL_PREFIX)) {
      serverId = serverId.substring(REST_PROTOCOL_PREFIX.length());
    }
    return serverId;
  }
  
  @Override
  public List<FolderEntryDescriptor> listFolder() throws IOException {
    // Take into consideration that we might send too much information.
    // Consider a full rewrite of the URL.
    URL listFolderURL = new URL(url.toExternalForm().replaceFirst("/files/", "/folders/"));
    URLConnection connection = listFolderURL.openConnection();
    connection.connect();
    
    String jsonFilesString = IOUtils.toString(connection.getInputStream());
    JsonArray jsonFiles = new JsonParser().parse(jsonFilesString).getAsJsonArray();
    String docUrl = getDocumenURL();
    List<FolderEntryDescriptor> files = new ArrayList<FolderEntryDescriptor>();
    for(int i = 0 ; i < jsonFiles.size(); i++) {
      JsonObject file = jsonFiles.get(i).getAsJsonObject();
      String filePath = docUrl + file.get("name").getAsString() + (file.get("folder").getAsBoolean() ? "/" : "");
      files.add(new FolderEntryDescriptor(filePath));
    }
    return files;
  }
  
  /**
   * @return the document url string from the delegate connection.
   */
  private String getDocumenURL() {
    String restEndpoint = "/files/";
    StringBuilder fullURL = new StringBuilder(url.toExternalForm());
    int endpointIndex = fullURL.indexOf(restEndpoint);
    fullURL.delete(0, endpointIndex + restEndpoint.length());
    int slashIndex = fullURL.indexOf("/");
    // remove additional path sections
    if(slashIndex != -1) {
      fullURL.delete(slashIndex, fullURL.length());
    }
    return URLUtil.decodeURIComponent(URLUtil.decodeURIComponent(fullURL.toString()));
  }
}
