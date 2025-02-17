package com.oxygenxml.rest.plugin.authn;

import com.oxygenxml.rest.plugin.AuthHeadersMap;

import ro.sync.ecss.extensions.api.webapp.access.InternalWebappPluginWorkspace;
import ro.sync.ecss.webapp.auth.ApplicationAuthenticationManager;
import ro.sync.exml.plugin.PluginContext;
import ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * Installer for the authentication provider.
 */
public class AuthenticationProviderInstaller implements WorkspaceAccessPluginExtension {
  
  @PluginContext
  private AuthHeadersMap authHeadersMap;
  

  @Override
  public void applicationStarted(StandalonePluginWorkspace pluginWorkspaceAccess) {
    InternalWebappPluginWorkspace pluginWorkspace = (InternalWebappPluginWorkspace) pluginWorkspaceAccess;
    ApplicationAuthenticationManager applicationAuthenticationManager =
        pluginWorkspace.getApplicationAuthenticationManager();
    
    RestApplicationAuthenticationProvider authenticationProvider = new RestApplicationAuthenticationProvider(
        authHeadersMap, 
        applicationAuthenticationManager,
        pluginWorkspace.getSessionStore());
    applicationAuthenticationManager.addApplicationAuthenticationProvider(authenticationProvider);
  }

  @Override
  public boolean applicationClosing() {
    return true;
  }
}
