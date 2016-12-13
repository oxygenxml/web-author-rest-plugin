package com.oxygenxml.rest.plugin;

import java.net.URLStreamHandler;

import ro.sync.exml.plugin.urlstreamhandler.URLStreamHandlerPluginExtension;
import ro.sync.exml.workspace.api.Platform;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

/**
 * URL Stream Handler plugin extension.
 * 
 * @author mihai_coanda
 */
public class RestURLHandlerPluginExtension implements URLStreamHandlerPluginExtension {

  /**
   * @see ro.sync.exml.plugin.urlstreamhandler.URLStreamHandlerPluginExtension#getURLStreamHandler(java.lang.String)
   */
  public URLStreamHandler getURLStreamHandler(String protocol) {
    boolean isWebapp = Platform.WEBAPP.equals(PluginWorkspaceProvider.getPluginWorkspace().getPlatform());
    if (isWebapp && protocol.startsWith(RestURLConnection.REST_PROTOCOL_PREFIX)) {
      return new RestURLStreamHandler();
    }
    return null;
  }

}