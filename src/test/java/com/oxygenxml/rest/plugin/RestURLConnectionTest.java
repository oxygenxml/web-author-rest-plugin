package com.oxygenxml.rest.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.junit.Test;
import org.mockito.Mockito;

import ro.sync.ecss.extensions.api.webapp.access.WebappPluginWorkspace;
import ro.sync.exml.workspace.api.PluginResourceBundle;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.net.protocol.http.HttpExceptionWithDetails;

public class RestURLConnectionTest {
  
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
    URL requestURL = new URL("http://localhost:8090/files?url=rest%3A%2F%2Fplatform%2Ffolder%2FTopic.dita");
    String fileUrl = RestURLConnection.getFileUrl(requestURL);
    assertEquals("rest://platform/folder/Topic.dita", fileUrl);
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

    RestURLConnection restURLConnection = new RestURLConnection("sessionId", requestURL.openConnection());
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
    RestURLConnection restUrlConnection = new RestURLConnection("sessionId", urlConnection);
    try {
      restUrlConnection.handleException(new HttpExceptionWithDetails(null, 500, null, requestURL));
      fail("must throw IOException"); 
    } catch (IOException e) {
      assertEquals("user-readable-message", e.getMessage());
    }
  }
}
