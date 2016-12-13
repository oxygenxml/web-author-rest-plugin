package com.oxygenxml.rest.plugin;

import ro.sync.exml.plugin.lock.LockHandler;
import ro.sync.exml.plugin.urlstreamhandler.LockHandlerFactoryPluginExtension;
import ro.sync.exml.workspace.api.Platform;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

/**
 * Plugin extension responsible for handling lock/unlock requests.
 * 
 * @author mihai_coanda
 */
public class RestLockHandlerFactory implements LockHandlerFactoryPluginExtension {
  
  /**
   * @return The lock handler.
   */
  public LockHandler getLockHandler() {
    return new RestLockHandler();
  }

  /**
   * @return <code>true</code> for the WebDAV protocol.
   */
  public boolean isLockingSupported(String protocol) {
    boolean isWebapp = Platform.WEBAPP.equals(PluginWorkspaceProvider.getPluginWorkspace().getPlatform());
    return isWebapp && protocol.startsWith(RestURLConnection.REST_PROTOCOL_PREFIX);
  }

}
