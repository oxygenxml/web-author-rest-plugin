package com.oxygenxml.rest.plugin;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;

/**
 * A simple HTTP server with:
 * - A POST /login endpoint that accepts user and pass parameters.
 * - Session management using a sessionId cookie.
 * - A GET /api/me endpoint that returns hardcoded JSON if authenticated.
 * - A GET /api/files?url=... endpoint that always returns "<root/>" as XML.
 */
public class SimpleHttpServer {

  // In-memory session store: sessionId -> username
  private static final Map<String, String> sessions = new ConcurrentHashMap<>();

  public static void main(String[] args) throws Exception {
    HttpServerManager serverManager = new HttpServerManager();
    HttpServer server = serverManager.createServerWithDefaulPort(new AuthEnabledHttpRequestHandler());
    server.start();
    System.out.println("Server started.");
    System.out.println("- Use http://localhost:" + server.getLocalPort() + "/api in the Admin Page configuration");
    System.out.println("- Use http://localhost:" + server.getLocalPort() + "/api/rest-login to log in");
    System.out.println("- Use http://localhost:" + server.getLocalPort() + "/logout to log out");
  }

  static class AuthEnabledHttpRequestHandler implements HttpRequestHandler {
    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext context)
        throws HttpException, IOException {

      String method = request.getRequestLine().getMethod();
      // Parse the URI so we can decide which endpoint to serve.
      URI uri = URI.create(request.getRequestLine().getUri());
      String path = uri.getPath();

      if ("/login".equals(path) && "POST".equalsIgnoreCase(method)) {
        handleLogin(request, response);
      } else if ("/api/me".equals(path) && "GET".equalsIgnoreCase(method)) {
        handleApiMe(request, response);
      } else if ("/api/files".equals(path) && "GET".equalsIgnoreCase(method)) {
        handleApiFiles(request, response);
      } else if ("/api/rest-login".equals(path)) {
        handleRestLoginEndpoint(request, response);
      } else if ("/logout".equals(path)) {
        handleLogout(request, response);
      } else {
        response.setStatusCode(404);
        response.setEntity(new StringEntity("Not Found", ContentType.TEXT_PLAIN));
      }
    }

    private void handleLogout(HttpRequest request, HttpResponse response) {
      String sessionId = getSessionIdFromCookie(request);
      if (sessionId != null) {
        sessions.remove(sessionId);
      }
      response.setStatusCode(302);
      response.addHeader("Location", "./api/rest-login");
    }

    private void handleRestLoginEndpoint(HttpRequest request, HttpResponse response) {
      // Return an HTML page with a form that POSTs to /login.
      // The form should contain user and pass fields. Already populated with the hardcoded values.
      String html = "<html><body>"
          + "<form method='POST' action=\"/login\">"
          + "  <div>User:<input type='text' name='user' value='admin'/></div>" 
          + "  <div>Pass:<input type='text' name='pass' value='password'/></div>"
          + "  <input type='submit' value='Login'/>" 
          + "</form>"
          + "</body></html>";
      
      response.setStatusCode(200);
      response.setEntity(new StringEntity(html, ContentType.TEXT_HTML));
    }

    /**
     * Handles the login endpoint.
     * Expects form data parameters: user and pass.
     * On success, sets a sessionId cookie.
     */
    private void handleLogin(HttpRequest request, HttpResponse response) throws IOException {
      String body = "";
      if (request instanceof HttpEntityEnclosingRequest) {
        body = EntityUtils.toString(((HttpEntityEnclosingRequest) request).getEntity());
      }
      
      // Parse the URL-encoded form data.
      Map<String, String> params = parseForm(body);
      String user = params.get("user");
      String pass = params.get("pass");

      // Validate credentials (hardcoded for this example).
      if ("admin".equals(user) && "password".equals(pass)) {
        // Successful login: generate a sessionId and store the session.
        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, user);
        // Return the sessionId in a cookie.
        response.addHeader("Set-Cookie", "sessionId=" + sessionId + "; Path=/; HttpOnly");
        response.setStatusCode(302);
        response.addHeader("Location", "http://localhost:8081/oxygen-xml-web-author/plugins-dispatcher/rest-login-callback");
      } else {
        response.setStatusCode(401);
        response.setEntity(new StringEntity("{\"error\":\"Invalid credentials\"}", ContentType.APPLICATION_JSON));
      }
    }

    /**
     * Handles the /api/me endpoint.
     * Returns hardcoded JSON with id, name, and email if the session is valid.
     */
    private void handleApiMe(HttpRequest request, HttpResponse response) throws IOException {
      String sessionId = getSessionIdFromCookie(request);
      if (sessionId != null && sessions.containsKey(sessionId)) {
        String json = "{\"id\":\"123\", \"name\":\"John Doe\", \"email\":\"john@example.com\"}";
        response.setStatusCode(200);
        response.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
      } else {
        response.setStatusCode(401);
        response.setEntity(new StringEntity("{\"error\":\"Unauthorized\"}", ContentType.APPLICATION_JSON));
      }
    }

    /**
     * Handles the /api/files endpoint.
     * Always returns "<root/>" as XML regardless of the query parameter.
     */
    private void handleApiFiles(HttpRequest request, HttpResponse response) throws IOException {
      response.setStatusCode(200);
      response.setEntity(new StringEntity("<root/>", ContentType.APPLICATION_XML));
    }

    /**
     * Parses URL-encoded form data.
     */
    private Map<String, String> parseForm(String body) {
      Map<String, String> params = new ConcurrentHashMap<>();
      if (body == null || body.isEmpty()) {
        return params;
      }
      String[] pairs = body.split("&");
      for (String pair : pairs) {
        String[] parts = pair.split("=", 2);
        if (parts.length == 2) {
          String key = decode(parts[0]);
          String value = decode(parts[1]);
          params.put(key, value);
        }
      }
      return params;
    }

    /**
     * URL-decodes a string using UTF-8.
     */
    private String decode(String s) {
      try {
        return URLDecoder.decode(s, StandardCharsets.UTF_8.name());
      } catch (Exception e) {
        return s;
      }
    }

    /**
     * Extracts the sessionId from the Cookie header.
     */
    private String getSessionIdFromCookie(HttpRequest request) {
      if (request.containsHeader("Cookie")) {
        String cookieHeader = request.getFirstHeader("Cookie").getValue();
        String[] cookies = cookieHeader.split(";");
        for (String cookie : cookies) {
          String[] parts = cookie.trim().split("=", 2);
          if (parts.length == 2 && "sessionId".equals(parts[0])) {
            return parts[1];
          }
        }
      }
      return null;
    }
  }
}
