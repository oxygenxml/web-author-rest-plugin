package com.oxygenxml.rest.plugin;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.bootstrap.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableMap;

import ro.sync.basic.util.URLStreamHandlerFactorySetter;
import ro.sync.ecss.extensions.api.webapp.AuthorDocumentModel;
import ro.sync.ecss.extensions.api.webapp.WebappAuthorDocumentFactory;
import ro.sync.ecss.extensions.api.webapp.access.WebappPluginWorkspace;
import ro.sync.ecss.extensions.api.webapp.plugin.UserActionRequiredException;
import ro.sync.ecss.webapp.plugin.WebappOptionTags;
import ro.sync.ecss.webapp.testing.MockAuthorDocumentFactory;
import ro.sync.exml.options.OptionTags;
import ro.sync.exml.options.Options;
import ro.sync.exml.workspace.api.PluginResourceBundle;
import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.options.WSOptionsStorage;
import ro.sync.net.protocol.OxygenURLStreamHandlerFactory;
import ro.sync.net.protocol.http.HttpExceptionWithDetails;

public class RestURLConnectionTest {
  /**
   * Responsible with starting servers.
   */
  @Rule
  public HttpServerManager serverManager = new HttpServerManager();
  
  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();

  /**
   * The plugin workspace to reset after test.
   */
  private PluginWorkspace oldPluginWorkspace;
  
  private URLStreamHandlerFactorySetter factorySetter;

  private String[] initialTrustedHosts;
  
  @Before
  public void before() throws Exception {
    MockAuthorDocumentFactory.initForTest();
    factorySetter = new URLStreamHandlerFactorySetter();
    oldPluginWorkspace = PluginWorkspaceProvider.getPluginWorkspace();
    initialTrustedHosts = Options.getInstance().getStringArrayProperty(OptionTags.TRUSTED_HOSTS);
  }
  
  @After
  public void after() throws Exception {
    factorySetter.tearDown();
    PluginWorkspaceProvider.setPluginWorkspace(oldPluginWorkspace);
    Options.getInstance().setStringArrayProperty(OptionTags.TRUSTED_HOSTS, initialTrustedHosts);
  }
  
  /**
   * <p><b>Description:</b> Test that we retrieve the rest:// URL correctly from the request URL.</p>
   * <p><b>Bug ID:</b> WA-1358</p>
   *
   * @author cristi_talau
   *
   * @throws Exception
   */
  @Test
  public void testFileUrlDecoding() throws Exception {
    URL requestURL = new URL("http://localhost:8090/files?url=rest%3A%2F%2Fplatform%2Ffolder%2Fa%2520topic.dita");
    String fileUrl = RestURLConnection.getFileUrl(requestURL);
    assertEquals("rest://platform/folder/a%20topic.dita", fileUrl);
  }
  
  
  /**
   * <p><b>Description:</b> Test that we properly handle FileNotFound exceptions.</p>
   * <p><b>Bug ID:</b> WA-3208</p>
   *
   * @author cristi_talau
   *
   * @throws Exception
   */
  @Test
  public void testFileNotFoundHandling() throws Exception {
    PluginResourceBundle bundle = Mockito.mock(PluginResourceBundle.class);
    Mockito.when(bundle.getMessage(Mockito.eq(TranslationTags.FILE_NOT_FOUND))).thenReturn("Pas trouve");
    WebappPluginWorkspace pluginWorkspace = Mockito.mock(WebappPluginWorkspace.class);
    Mockito.when(pluginWorkspace.getResourceBundle()).thenReturn(bundle);
    PluginWorkspaceProvider.setPluginWorkspace(pluginWorkspace);
    
    URL requestURL = new URL("http://localhost:8090/files?url=rest%3A%2F%2Fplatform%2Ffolder%2FTopic.dita");

    RestURLConnection restURLConnection = new RestURLConnection(
        new AuthHeadersMap(), "sessionId", requestURL.openConnection());
    try {
      restURLConnection.handleException(new HttpExceptionWithDetails("xxx", 404, "pas trouve", requestURL));
      fail("Should throw"); 
    } catch (FileNotFoundException e) {
      // Expected.
      assertEquals("Pas trouve /folder/Topic.dita", e.getMessage());
    }
  }

  /**
   * <p><b>Description:</b> The file-server can return an error message and web-author-rest-plugin should pass it to Web Author.
   * See https://github.com/oxygenxml/web-author-rest-plugin/blob/BRANCH_OXYGEN_RELEASE_24_1/docs/API-spec.md#error-responses</p>
   * <p><b>Bug ID:</b> WA-5621</p>
   *
   * @author bogdan_dumitru
   *
   * @throws Exception If it fails.
   */
  @Test
  public void testTransferServerMessage() throws Exception {
    URL requestURL = new URL("http://localhost:8090/files?url=rest%3A%2F%2Fplatform%2Ffolder%2FTopic.dita");
    HttpURLConnection urlConnection = mock(HttpURLConnection.class);
    when(urlConnection.getErrorStream()).thenReturn(new ByteArrayInputStream("{\"message\":\"user-readable-message\"}".getBytes(StandardCharsets.UTF_8)));
    when(urlConnection.getURL()).thenReturn(requestURL);
    RestURLConnection restUrlConnection = new RestURLConnection(new AuthHeadersMap(), "sessionId", urlConnection);
    try {
      restUrlConnection.handleException(new HttpExceptionWithDetails(null, 500, null, requestURL));
      fail("must throw IOException"); 
    } catch (IOException e) {
      assertEquals("user-readable-message", e.getMessage());
    }
  }
  
  
  
  /**
   * <p><b>Description:</b> Test that an exception with 401 in message is not detected as auth error.</p>
   * <p><b>Bug ID:</b> WA-6056</p>
   *
   * @author cristi_talau
   *
   * @throws Exception
   */
  @Test
  public void test401Detection() throws Exception {
    factorySetter.setFactory(new OxygenURLStreamHandlerFactory());

    String filePath = "file401.xml";
    HttpServer server = createServerThatFailsOnSave(412, filePath + "is in conflict");
    
    RestURLConnection conn = createRestConnection(server, filePath);
    
    exceptionRule.expect(not(instanceOf(UserActionRequiredException.class)));
    writeToConnection(conn);
  }

  private HttpServer createServerThatFailsOnSave(int status, String errorMessage) throws IOException {
    HttpServer server = serverManager.createServer((request, response, context) -> {
      if (request.getRequestLine().getMethod().equals("GET")) {
        response.setEntity(new StringEntity("<root/>"));
      } else {
        response.setStatusCode(status);
        response.setEntity(new StringEntity(errorMessage));
      }
    });
    Options.getInstance().setStringArrayProperty(OptionTags.TRUSTED_HOSTS,
        new String[] {"localhost:" + server.getLocalPort()});
    return server;
  }
  
  /**
   * <p><b>Description:</b> Test that an exception with 401 status code is detected as auth error.</p>
   * <p><b>Bug ID:</b> WA-6056</p>
   *
   * @author cristi_talau
   *
   * @throws Exception
   */
  @Test
  public void test401Detection2() throws Exception {
    factorySetter.setFactory(new OxygenURLStreamHandlerFactory());
    HttpServer server = createServerThatFailsOnSave(401, "Unauthorized");

    String filePath = "file401.xml";
    RestURLConnection conn = createRestConnection(server, filePath);
    
    exceptionRule.expect(UserActionRequiredException.class);
    writeToConnection(conn);
  }

  /**
   * <p><b>Description:</b> Test that a bearer auth token is sent.</p>
   * <p><b>Bug ID:</b> WA-6658</p>
   *
   * @author cristi_talau
   *
   * @throws Exception
   */
  @Test
  public void testBearerAuth() throws Exception {
    String bearerToken = "abc";
    String sessionId = "session-id";
    
    AuthHeadersMap authHeadersMap = new AuthHeadersMap();
    authHeadersMap.setBearerToken(sessionId, bearerToken);
    
    factorySetter.setHandler("rest", new RestURLStreamHandler(authHeadersMap));
    
    WSOptionsStorage optionsStorage = PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage();
    String oldOption = optionsStorage.getOption(RestConfigExtension.REST_SERVER_URL, "");
    try {
      HttpServer server = createServerThatRequiresAuth(bearerToken, "<root/>");
      optionsStorage.setOption(RestConfigExtension.REST_SERVER_URL, "http://localhost:" + server.getLocalPort() + "/");

      AuthorDocumentModel documentInfo = WebappAuthorDocumentFactory.createAuthorDocumentInfo("rest://" + sessionId + "@server/file.xml", 
          ImmutableMap.of("bearer.token", bearerToken));
      
      String content = IOUtils.toString(documentInfo.createReader());
      assertEquals("<root/>", content);  
    } finally {
      optionsStorage.setOption(RestConfigExtension.REST_SERVER_URL, oldOption);        
    }
  }
  
  private HttpServer createServerThatRequiresAuth(String token, String content) throws IOException {
    HttpServer server = serverManager.createServer((request, response, context) -> {
      Header authHeader = request.getFirstHeader("Authorization");
      if (authHeader != null && authHeader.getValue().equals("Bearer " + token)) {
        response.setEntity(new StringEntity(content));
      } else {
        response.setStatusCode(401);
        response.setEntity(new StringEntity("entity"));
      }
    });
    return server;
  }

  
  /**
   * <p><b>Description:</b> Test exception parsing.</p>
   * <p><b>Bug ID:</b> WA-6173</p>
   *
   * @author cristi_talau
   *
   * @throws Exception
   */
  @Test
  public void testMalformedErrorMessage() throws Exception {
    String message = RestURLConnection.extractUserReadableMessage("{}");
    assertEquals("{}", message);
  }


  private RestURLConnection createRestConnection(HttpServer server, String filePath) throws IOException, MalformedURLException {
    URLConnection lowLevelHttpConnection = new URL("http://localhost:" + server.getLocalPort()+ "/" + filePath).openConnection();
    RestURLConnection conn = new RestURLConnection(new AuthHeadersMap(), "abc", lowLevelHttpConnection);
    return conn;
  }

  private void writeToConnection(RestURLConnection conn) throws IOException {
    conn.setDoOutput(true);
    try (OutputStream out = conn.getOutputStream()) {
      out.write("<rut/>".getBytes(StandardCharsets.UTF_8));
    }
  }

}
