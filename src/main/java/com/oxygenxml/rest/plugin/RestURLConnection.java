package com.oxygenxml.rest.plugin;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import com.google.common.net.MediaType;

import lombok.extern.slf4j.Slf4j;
import ro.sync.basic.io.QuietClosable;
import ro.sync.basic.util.URLUtil;
import ro.sync.ecss.extensions.api.webapp.WebappMessage;
import ro.sync.ecss.extensions.api.webapp.access.WebappPluginWorkspace;
import ro.sync.ecss.extensions.api.webapp.plugin.FilterURLConnection;
import ro.sync.ecss.extensions.api.webapp.plugin.UserActionRequiredException;
import ro.sync.exml.plugin.urlstreamhandler.CacheableUrlConnection;
import ro.sync.exml.workspace.api.PluginResourceBundle;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.net.protocol.FolderEntryDescriptor;
import ro.sync.net.protocol.http.HttpExceptionWithDetails;

/**
 * Wrapper over an URLConnection that reports 401 exceptions as 
 * {@link UserActionRequiredException}.
 * 
 * @author mihai_coanda
 */
@Slf4j
public class RestURLConnection extends FilterURLConnection implements CacheableUrlConnection {
   
  /**
   * Header set for all requests in order to allow CMS's to prevent CSRF requests.
   */
  private static final String CSRF_HEADER = "X-Requested-With";
  
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
   * The URL of the connection if different from that of the underlying connection.
   */
  private URL urlOverride;

  /**
   * The headers used for authentication
   */
  private AuthHeadersMap authHeadersMap;

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
  protected RestURLConnection(AuthHeadersMap authHeadersMap, String contextId, URLConnection delegate) {
    super(delegate);
    this.authHeadersMap = authHeadersMap;
    this.contextId = contextId;
    addHeaders(this, this.contextId);
  }
  
  @Override
  public void connect() throws IOException {
	log.debug("Attempting to connect to URL: {}", getURL());
    try {
      super.connect();
      log.debug("Successfully connected to URL: {}", getURL());
    } catch (IOException e) {
      log.debug("Failed to connect to URL: {}", getURL());
      handleException(e);
    }
  }
  
  @Override
  public InputStream getInputStream() throws IOException {
	log.debug("Attempting to get input stream for URL: {}", getURL(), new Exception("Get input stream"));
	
    if (this.getDoOutput()) {
      // Nothing to read for "save" operations.
      log.debug("Nothing to read for \"save\" operations. Returning empty string.");
      return new ByteArrayInputStream(new byte[0]);
    }
    try {
      InputStream stream = super.getInputStream();
      log.debug("Successfully retrieved input stream for URL: {}", getURL());
      return stream;
    } catch (IOException e) {
      log.debug("Failed to get input stream for URL: {}", getURL());
      e.printStackTrace();
      handleException(e);
      
      // Unreachable.
      return null;
    }
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
	log.debug("Attempting to get output stream for URL: {}", getURL(), new Exception("Get output stream"));

    OutputStream outputStream;
    try {
      outputStream = super.getOutputStream();
      log.debug("Successfully retrieved output stream for URL: {}", getURL());
    } catch (IOException e) {
      log.debug("Failed to get output stream for URL: {}", getURL());
      handleException(e);
      // Unreachable.
      return null;
    }
    
    return new FilterOutputStream(outputStream) {
    	
      @Override
      public void close() throws IOException {
    	  
    	log.debug("Attempting to close output stream for URL: {}", getURL());
    	      	  
        RestURLConnection connection = RestURLConnection.this;
        try {
          try {
            super.close();
          } catch (IOException e) {
            handleException(e);
          }
          
          // WA-1358: The server overridden the location.
          String actualLocation = connection.getHeaderField("Location");
          if (actualLocation != null) {
            connection.urlOverride = new URL(actualLocation);
          }
        } finally {
          // CF-902: Release the underlying connection.
          HttpURLConnection delegateHttConn = (HttpURLConnection)connection.delegateConnection;
          delegateHttConn.disconnect();
        }
      }
    };

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
  void handleException(IOException e) throws IOException {
    URL url = this.delegateConnection.getURL();
    String fileUrl = getFileUrl(url);
    if(log.isDebugEnabled()) {
      log.debug("Exception thrown when accessing " + fileUrl, new Exception(e));
    }
    if(e instanceof HttpExceptionWithDetails) {
      HttpExceptionWithDetails detailed = (HttpExceptionWithDetails)e;
      if(detailed.getReasonCode() == HttpStatus.SC_NOT_FOUND) {
        URL baseURL = detailed.getBaseURL();
        String fileURL = getFilePath(baseURL);
        PluginResourceBundle rb = ((WebappPluginWorkspace)PluginWorkspaceProvider.getPluginWorkspace()).getResourceBundle();
        throw new FileNotFoundException(rb.getMessage(TranslationTags.FILE_NOT_FOUND) + " " + fileURL);
      }
      
      if (detailed.getReasonCode() == HttpStatus.SC_UNAUTHORIZED) {
        logFailedLoginAttempt(url, fileUrl);
        throw createUserActionReqiredException(fileUrl);   
      }
    }
    if (delegateConnection instanceof HttpURLConnection) {
      String serverMessage = getServerErrorMessage((HttpURLConnection) this.delegateConnection);
      if (serverMessage != null) {
        if (shouldDisplayServerMessage(serverMessage)) {
          throw new IOException(extractUserReadableMessage(serverMessage), e);
        } else {
          log.debug("Server message too complex to display to the user: " + serverMessage);
        }
      }
    }
    throw e;
  }

  private UserActionRequiredException createUserActionReqiredException(String fileUrl) {
    PluginResourceBundle rb = ((WebappPluginWorkspace)PluginWorkspaceProvider.getPluginWorkspace()).getResourceBundle();
    UserActionRequiredException exception = new UserActionRequiredException(
        new WebappMessage(WebappMessage.MESSAGE_TYPE_CUSTOM, rb.getMessage(TranslationTags.AUTHENTICATION_REQUIRED),
            // send back the URL for which to authenticate.
            fileUrl, true));
    return exception;
  }

  /**
   * Extract user readable message from file-server's response.
   * @param serverResponse The response.
   * @return User readable message.
   */
  @VisibleForTesting
  static String extractUserReadableMessage(String serverResponse) {
    String userMessage = serverResponse;
    try {
      JsonNode tree = new ObjectMapper().reader().readTree(serverResponse);
      JsonNode message = tree.get("message");
      if (message != null && message.getNodeType().equals(JsonNodeType.STRING)) {
        userMessage = message.asText();
      }
    } catch (JsonProcessingException e) {
      log.debug(e);
    }
    return userMessage;
  }

  /**
   * See https://github.com/oxygenxml/web-author-rest-plugin/blob/BRANCH_OXYGEN_RELEASE_24_1/docs/API-spec.md#error-responses
   *
   * @param httpURLConnection The connection
   * @return The error message sent by the server.
   * @throws IOException If the error message could not be read.
   */
  private String getServerErrorMessage(HttpURLConnection httpURLConnection) throws IOException {
    String serverMessage = null;
    try (InputStream errorStream = QuietClosable.from(httpURLConnection.getErrorStream())){
      if (errorStream != null) {
        serverMessage = IOUtils.toString(errorStream);
      }
    }
    return serverMessage;
  }

  /**
   * Log failed login attempt.
   * @param url The REST URL.
   * @param fileUrl The file URL.
   */
  private void logFailedLoginAttempt(URL url, String fileUrl) {
    String userInfo = url.getUserInfo();
    if (userInfo != null && !userInfo.isEmpty()) {
      String user = URLUtil.extractUser(url.toExternalForm());
      if (user != null && !user.trim().isEmpty()) {
        log.warn("Failed login attempt of user " + user + " for " + URLUtil.getDescription(fileUrl));
      } else {
        log.warn("Failed login attempt for " + URLUtil.getDescription(fileUrl));
      }
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
    } else {
      if (serverMessage.contains("<body") || serverMessage.contains("</body")) {
        shouldDisplay = false;
      }
      if (serverMessage.contains("<?xml")) {
        shouldDisplay = false;
      }
    }
    return shouldDisplay;
  }

  /**
   * Adds credentials associated with the given user context to this rest url connection. 
   */
  public void addHeaders(URLConnection urlConnection, String contextId) {
    // This header is set in order to allow CMS's to prevent CSRF attacks.
    // An attacker can create a form to send a post requests to a rest end-point but 
    // won't be able to set this header.
    urlConnection.addRequestProperty(CSRF_HEADER, "RC");
    
    if(contextId == null) {
      // The current request did not match any session - no headers to add.
      log.debug("No session matched. No headers added.");
      return;
    }
    
    authHeadersMap.addHeaders(contextId, urlConnection);
  }
  
  @Override
  public List<FolderEntryDescriptor> listFolder() throws IOException {
    URL listFolderURL = new URL(url.toExternalForm().replaceFirst("/files", "/folders"));
    log.debug("Listing folder contents for URL: {}", listFolderURL);
    URLConnection connection;
    connection = listFolderURL.openConnection();
    connection.addRequestProperty("Accept", MediaType.JSON_UTF_8.toString());
    // Adding headers to the folder listing connection.    
    addHeaders(connection, this.contextId);
    connection.connect();
    
    // Read the server response in a buffer in order to be able to print it for debugging purposes.
    byte[] jsonBytes;
    try {
      jsonBytes = readConnectionBytes(connection);
    } catch (HttpExceptionWithDetails e) {
      log.debug("Failed to read folder listing from REST server :" + e.getMessage());
      if(HttpStatus.SC_UNAUTHORIZED == e.getReasonCode()) {
        PluginResourceBundle rb = ((WebappPluginWorkspace)PluginWorkspaceProvider.getPluginWorkspace()).getResourceBundle();
        throw new UserActionRequiredException(new WebappMessage(
            WebappMessage.MESSAGE_TYPE_CUSTOM, rb.getMessage(TranslationTags.AUTHENTICATION_REQUIRED),
            rb.getMessage(TranslationTags.AUTHENTICATION_REQUIRED), true));
      } else if (HttpStatus.SC_NOT_FOUND == e.getReasonCode()) {
        String folderPath = getFilePath(e.getBaseURL());
        throw new FileNotFoundException(folderPath);
      }
      throw e;
    }    
    if (log.isDebugEnabled()) {
      String jsonFilesString = new String(jsonBytes, Charsets.UTF_8);
      log.debug("Received folder listing from REST server :" + jsonFilesString);
    }
    
    ObjectMapper mapper = new ObjectMapper();
    JsonNode[] array;
    try {
      array = mapper.readValue(jsonBytes, mapper.getTypeFactory().constructArrayType(JsonNode.class));
    } catch (IOException e) {
      // The original error message is not user-friendly at all - replace it.
      throw new IOException("Invalid server response", e);
    }
    
    List<FolderEntryDescriptor> files = new ArrayList<FolderEntryDescriptor>();
    for(int i = 0; i < array.length; i++) {
      JsonNode file = array[i];
      JsonNode folderProp = file.get("folder");
      boolean isFolder = folderProp != null && folderProp.asBoolean();
      String encodedFileName = URLUtil.encodeURIComponent(file.get("name").asText());
      String filePath = getDocumenURL() + encodedFileName + (isFolder ? "/" : "");
      log.debug("Add parsed file path :" + filePath);
      files.add(new FolderEntryDescriptor(filePath));
    }
    return files;
  }

  /**
   * Read the bytes from the given connection.
   * 
   * @param connection The URL connection.
   * @return The bytes read.
   * 
   * @throws IOException Any exception caught when reading from the URL.
   */
  private byte[] readConnectionBytes(URLConnection connection) throws IOException {
    byte[] jsonBytes;
    InputStream inputStream = connection.getInputStream();
    try {
      jsonBytes = ByteStreams.toByteArray(inputStream);
      log.debug("Read input stream bytes for connection: {}; : {} bytes", connection.getURL(), jsonBytes.length);
    } finally {
      try {
        inputStream.close();
      } catch (IOException e) {
        // Ignore the exception - we already read the server response.
      }
    }
    return jsonBytes;
  }
  
  /**
   * @return the document url string from the delegate connection.
   */
  private String getDocumenURL() {
    String restEndpoint = "/files";
    StringBuilder fullURL = new StringBuilder(url.toExternalForm());
    log.debug("Getting document URL from: {}", fullURL);

    int endpointIndex = fullURL.indexOf(restEndpoint);
    fullURL.delete(0, endpointIndex + restEndpoint.length());
    int slashIndex = fullURL.indexOf("/");
    // remove additional path sections
    if(slashIndex != -1) {
      fullURL.delete(slashIndex, fullURL.length());
    }
    
    String documentURL = URLUtil.decodeURIComponent(URLUtil.decodeURIComponent(fullURL.toString()));
    log.debug("Computed document URL: {}", documentURL);
    return documentURL;
  }
  
  @Override
  public URL getURL() {
    if (this.urlOverride != null) {
      return this.urlOverride;
    } else {
      URL requestURL = super.getURL();
      String fileUrl = getFileUrl(requestURL);
      try {
        return new URL(fileUrl);
      } catch (MalformedURLException e) {
        return requestURL;
      }
    }
  }
  
  /**
   * Return the path of the file referred to by the given request URL. 
   * 
   * @param requestURL The request URL.
   * 
   * @return The file URL.
   */
  private static String getFilePath(URL requestURL) {
    String fileUrl = getFileUrl(requestURL);
    String filePath = fileUrl;
    try {
      filePath = new URI(fileUrl).getPath();
    } catch (URISyntaxException se) {
      // use the full URL if it cannot be parsed
    }
    return filePath;
  }

  /**
   * Return the file URL referred to by the given request URL. It is a query parameter with name "url". 
   * 
   * @param requestURL The request URL.
   * 
   * @return The file URL.
   */
  @VisibleForTesting
  static String getFileUrl(URL requestURL) {
    List<NameValuePair> params = URLEncodedUtils.parse(requestURL.getQuery(), Charsets.UTF_8);
    String fileUrlParam = null;
    for (NameValuePair pair : params) {
      if (pair.getName().equals("url")) {
        fileUrlParam = pair.getValue();
      }
    }
    String fileUrl = requestURL.toExternalForm();
    if (fileUrlParam != null) {
      fileUrl = fileUrlParam;
    }
    return fileUrl;
  }
}
