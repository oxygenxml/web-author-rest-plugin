package com.oxygenxml.rest.plugin;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.TimeUnit;

import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.HttpRequestHandler;
import org.junit.rules.ExternalResource;

/**
 * Generic HTTP server manager for tests.
 * 
 * @author cristi_talau
 */
public class HttpServerManager extends ExternalResource {

  /**
   * The server.
   */
  private HttpServer server;

  
  /**
   * Creates a server listening on a random port.
   * 
   * @param handler The request handler.
   * @return the server
   * @throws IOException When the levee breaks.
   */
  public HttpServer createServer(HttpRequestHandler handler) throws IOException {
    return this.createServerInternal(handler, getAvailablePort());
  }
  
  /**
   * Creates a server listening on port 7171.
   * 
   * @param handler The request handler.
   * @return the server
   * @throws IOException When the levee breaks.
   */
  public HttpServer createServerWithDefaulPort(HttpRequestHandler handler) throws IOException {
    return this.createServerInternal(handler, 7171);
  }
  
  /**
   * Creates a server listening on a specific port.
   * 
   * @param handler The request handler.
   * @param port The port.
   * @return the server
   * @throws IOException When the levee breaks.
   */
  private HttpServer createServerInternal(HttpRequestHandler handler, int port) throws IOException {
    ServerBootstrap serverBootstrap = ServerBootstrap.bootstrap()
        .setListenerPort(port)
        .setServerInfo("Test/1.1")
        .registerHandler("*", handler);
    startServer(serverBootstrap);
    return this.server;
  }
  

  /**
   * @return An available port.
   * @throws IOException
   */
  private int getAvailablePort() throws IOException {
    int availablePort;
    try (ServerSocket ss = new ServerSocket(0)) {
      availablePort = ss.getLocalPort();
    }
    return availablePort;
  }

  /**
   * Start the server specified by the bootstrap object.
   * @param serverBootstrap The bootstrap object.
   * @throws IOException If the server cannot start.
   */
  private void startServer(ServerBootstrap serverBootstrap) throws IOException {
    if (this.server != null) {
      this.server.shutdown(0, TimeUnit.SECONDS);
    }
    this.server = serverBootstrap.create();
    this.server.start();
  }

  /**
   * Shuts down the server after the tests.
   */
  @Override
  protected void after() {
    if (this.server != null) {
      this.server.shutdown(0, TimeUnit.MILLISECONDS);
    }
  }
}
