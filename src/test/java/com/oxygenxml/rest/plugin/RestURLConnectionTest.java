package com.oxygenxml.rest.plugin;

import static org.junit.Assert.*;

import java.net.URL;

import org.junit.Test;

public class RestURLConnectionTest {
  
  /**
   * <p><b>Description:</b> DESCRIBE THE TEST HERE!</p>
   * <p><b>Bug ID:</b> EXM-1358</p>
   *
   * @author cristi_talau
   *
   * @throws Exception
   */
  @Test
  public void testFileUrlDecoding() throws Exception {
    URL requestURL = new URL("http://localhost:8090/files/?url=rest%3A%2F%2Fplatform%2Ffolder%2FTopic.dita");
    String fileUrl = RestURLConnection.getFileUrl(requestURL);
    assertEquals("rest://platform/folder/Topic.dita", fileUrl);
  }

}
