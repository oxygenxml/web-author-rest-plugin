package com.oxygenxml.rest.plugin;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.Closeables;

import ro.sync.ecss.extensions.api.webapp.WebappMessage;
import ro.sync.ecss.extensions.api.webapp.plugin.FilterURLConnection;
import ro.sync.ecss.extensions.api.webapp.plugin.UserActionRequiredException;
import ro.sync.exml.plugin.urlstreamhandler.CacheableUrlConnection;
import ro.sync.net.protocol.FolderEntryDescriptor;
import ro.sync.net.protocol.http.HttpExceptionWithDetails;
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
  public static final Cache<String, Map<String, String>> credentialsMap =
      CacheBuilder.newBuilder()
        .concurrencyLevel(10)
        .maximumSize(10000)
        .build();
  
  /**
   * Prefix of the protocol.
   * 
   * We translate http to rest-http and https to rest-https.
   */
  public static final String REST_PROTOCOL = "rest";

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
        
        if(e instanceof HttpExceptionWithDetails) {
          HttpExceptionWithDetails detailed = (HttpExceptionWithDetails)e;
          if(detailed.getReasonCode() == HttpStatus.SC_NOT_FOUND) {
            serverMessage = "The document was not found.";
          }
        }
        if(serverMessage == null) {
          InputStream errorStream = null;
          try {
            errorStream = ((HttpURLConnection) this.delegateConnection)
                .getErrorStream();
            serverMessage = IOUtils.toString(errorStream);
          } catch(Exception ex) {
            Closeables.closeQuietly(errorStream);
          }
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
    Map<String, String> serverHeaders = credentialsMap.getIfPresent(contextId);
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
  }
  
  @Override
  public List<FolderEntryDescriptor> listFolder() throws IOException {
    URL listFolderURL = new URL(url.toExternalForm().replaceFirst("/files/", "/folders/"));
    URLConnection connection;
    connection = listFolderURL.openConnection();
    // Adding headers to the folder listing connection.    
    addHeaders(connection, this.contextId);
    connection.connect();
    
    String jsonFilesString;
    try {
      jsonFilesString = IOUtils.toString(connection.getInputStream());
    } catch(IOException e) {
      if(401 == ((HttpExceptionWithDetails)e).getReasonCode()) {
        throw new UserActionRequiredException(new WebappMessage(
            WebappMessage.MESSAGE_TYPE_CUSTOM, "Authentication required",
            "Authentication required", true));
      } else {
        throw e;
      }
    }
    
    ObjectMapper mapper = new ObjectMapper();
    JsonNode[] array;
    array = mapper.readValue(jsonFilesString, mapper.getTypeFactory().constructArrayType(JsonNode.class));
    
    List<FolderEntryDescriptor> files = new ArrayList<FolderEntryDescriptor>();
    for(int i = 0; i < array.length; i++) {
      JsonNode file = array[i];
      String filePath = getDocumenURL() + file.get("name").asText() + (file.get("folder").asBoolean() ? "/" : "");
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
