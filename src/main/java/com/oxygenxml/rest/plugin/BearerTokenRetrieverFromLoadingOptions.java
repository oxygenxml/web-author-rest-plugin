package com.oxygenxml.rest.plugin;

import java.net.URL;
import java.util.Map;

import ro.sync.ecss.extensions.api.webapp.access.EditingSessionOpenVetoException;
import ro.sync.ecss.extensions.api.webapp.access.WebappEditingSessionLifecycleListener;
import ro.sync.ecss.extensions.api.webapp.access.WebappPluginWorkspace;
import ro.sync.exml.plugin.PluginContext;
import ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

public class BearerTokenRetrieverFromLoadingOptions implements WorkspaceAccessPluginExtension {

  private static final String BEARER_TOKEN_LOADING_OPTION = "bearer.token";
  
  @PluginContext
  private AuthHeadersMap authHeadersMap;

  @Override
  public void applicationStarted(StandalonePluginWorkspace workspaceAccess) {
    WebappPluginWorkspace webappWorkspaceAccess = (WebappPluginWorkspace) workspaceAccess;
    webappWorkspaceAccess.addEditingSessionLifecycleListener(new WebappEditingSessionLifecycleListener() {
      @Override
      public void editingSessionAboutToBeStarted(String sessionId, String licenseeId, 
          URL systemId, Map<String, Object> options)
          throws EditingSessionOpenVetoException {
        String bearerToken = (String) options.get(BEARER_TOKEN_LOADING_OPTION);
        if (bearerToken != null) {
          authHeadersMap.setBearerToken(sessionId, bearerToken);
        }
      }
    });
  }

  @Override
  public boolean applicationClosing() {
    return true;
  }

}
