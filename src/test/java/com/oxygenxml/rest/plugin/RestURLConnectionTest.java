package com.oxygenxml.rest.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.net.URL;

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
}
