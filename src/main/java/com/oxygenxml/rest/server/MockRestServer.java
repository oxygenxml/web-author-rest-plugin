package com.oxygenxml.rest.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.oxygenxml.rest.plugin.RestURLConnection;

import ro.sync.ecss.extensions.api.webapp.plugin.WebappServletPluginExtension;
import ro.sync.util.URLUtil;

public class MockRestServer extends WebappServletPluginExtension {
  
  /**
   * REST end-points.
   */
  private static final String LOCK = "lock";
  private static final String FILES = "files";
  private static final String UNLOCK = "unlock";
  private static final String VERSIONS = "versions";
  private static final String FOLDERS = "folders";
  private static final String INFO = "info";

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String restEndpoint = getRestEndpoint(req);
    URL docURL = getDocumentURL(req, restEndpoint);
    
    switch (restEndpoint) {
    case FILES:
      URLConnection connection = docURL.openConnection();
      connection.connect();
      InputStream is = connection.getInputStream();
      ServletOutputStream os = resp.getOutputStream();

      IOUtils.copy(is, os);

      break;
    case LOCK :
      
      
      break;
    case UNLOCK :
      
      
      break;
    case FOLDERS :
      URL listURL = new URL("http://localhost:8081/oxygen-webapp/rest/v19.0.0/browse/list/webdav-"
           + URLUtil.encodeURIComponent(URLUtil.encodeURIComponent(docURL.toExternalForm())));
      URLConnection listConnection = listURL.openConnection();
      listConnection.connect();
      IOUtils.copy(listConnection.getInputStream(), resp.getOutputStream());
      
      break;
    case VERSIONS :
      
      
      break;
    case INFO :
      // TODO: send the url with the same protocol for all the services.
      // this one contains "rest-"
      
      String  docUrlString = docURL.toExternalForm();
      if(docUrlString.startsWith(RestURLConnection.REST_PROTOCOL_PREFIX)) {
        docUrlString = docUrlString.substring(RestURLConnection.REST_PROTOCOL_PREFIX.length());
      }
      
      URL infoURL = new URL("http://localhost:8081/oxygen-webapp/plugins-dispatcher/" +
          "webdav-url-info" +
          "?url=" + URLUtil.encodeURIComponent("webdav-" + docUrlString));
      URLConnection infoConnection = infoURL.openConnection();
      infoConnection.connect();
      
      IOUtils.copy(infoConnection.getInputStream(), resp.getOutputStream());
      
      break;
    default:
      // TODO: unimplemented method.
      break;
    }
  }
  
  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    
    String restEndpoint = getRestEndpoint(req);
    URL docURL = getDocumentURL(req, restEndpoint);
//    System.out.println("POST " + docURL.toExternalForm());
    
    
    switch (restEndpoint) {
    case FILES:
      URLConnection connection = docURL.openConnection();
      connection.setDoOutput(true);
      connection.connect();
      // write to the connection.
      ServletInputStream is = req.getInputStream();
      OutputStream os = connection.getOutputStream();
      IOUtils.copy(is, os);
      
      break;
    case LOCK :
      
      
      break;
    case UNLOCK :
      
      
      break;

      
    default:
      break;
    }
    
    
    
  }

  /**
   * Computes the rest endpoint.
   * 
   * @param req the request.
   * 
   * @return
   */
  private String getRestEndpoint(HttpServletRequest req) {
    String pathInfo = removeServletPathAndVersion(req.getPathInfo());
    int slashIndex = pathInfo.indexOf("/");
    
    String restEndpoint = pathInfo.substring(0, slashIndex);
    
//    System.out.println(" rest endpoint");
    
    return restEndpoint;
  }

  /**
   * Computes the document url from the request.
   * 
   * @param req the servlet request.
   * @param restEndpoint the rest endpoint.
   * 
   * @return the document url.
   * 
   * @throws MalformedURLException if somenthing went wrong.
   */
  public URL getDocumentURL(HttpServletRequest req, String restEndpoint) throws MalformedURLException {
    String cleanPathInfo = removeServletPathAndVersion(req.getPathInfo());
    
    String encodedDocuUrl = cleanPathInfo.substring(("/" + getPath() + "/").length());
    encodedDocuUrl = cleanPathInfo.substring(encodedDocuUrl.indexOf("/" + restEndpoint + "/") + ("/" + restEndpoint + "/").length());

    return new URL(URLUtil.decodeURIComponent(encodedDocuUrl));
  }
  
  /**
   * Removes the servlet path and the version from the path info.
   * 
   * @param pathInfo the request path info.
   * 
   * @return the path info without the servlet path and the version.
   */
  public String removeServletPathAndVersion(String pathInfo) {
    String noServletInfo = pathInfo.substring(("/" + getPath() + "/").length());
    int slashIndex = noServletInfo.indexOf("/");
    
//    System.out.println("clean info " + noServletInfo.substring(slashIndex + 1));
    
    return noServletInfo.substring(slashIndex + 1);
  }
  
  @Override
  public String getPath() {
    return "rest-mock-server";
  }
}
