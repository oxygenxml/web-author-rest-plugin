package com.oxygenxml.rest.plugin;

import java.util.Map;

import com.google.common.base.Supplier;

import ro.sync.ecss.extensions.api.access.EditingSessionContext;
import ro.sync.ecss.extensions.api.webapp.AuthorDocumentModel;
import ro.sync.ecss.extensions.api.webapp.access.WebappEditingSessionLifecycleListener;
import ro.sync.ecss.extensions.api.webapp.access.WebappPluginWorkspace;
import ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.util.URLUtil;

/**
 * Class that provides access to the REST headers to cooperating plugins.
 * 
 * @author cristi_talau
 */
public class AuthHeadersApi implements WorkspaceAccessPluginExtension {

  /**
   * The key used to retrieve REST headers to be sent to the server.
   */
  public static final String REST_HEADERS_KEY = "rest_plugin_headers";
  
  @Override
  public void applicationStarted(StandalonePluginWorkspace pluginWorkspaceAccess) {
    WebappPluginWorkspace webappPluginWorkspaceAccess = ((WebappPluginWorkspace)pluginWorkspaceAccess);
    webappPluginWorkspaceAccess.addEditingSessionLifecycleListener(new WebappEditingSessionLifecycleListener() {
      @Override
      public void editingSessionStarted(String sessionId, AuthorDocumentModel documentModel) {
        registerHeaders(documentModel);
      }
      
      public void editingSessionDeserialized(String sessionId, AuthorDocumentModel documentModel) {
        registerHeaders(documentModel);
      }
      @Override
      public void editingSessionClosed(String sessionId, AuthorDocumentModel documentModel) {
        super.editingSessionClosed(sessionId, documentModel);
      }
    });
  }

  /**
   * Register the REST headers in the editing context.
   * 
   * @param documentModel The document model.
   */
  private void registerHeaders(AuthorDocumentModel documentModel) {
    String systemID = documentModel.getAuthorDocumentController().getAuthorDocumentNode().getSystemID();
    if (!systemID.startsWith(RestURLConnection.REST_PROTOCOL + "://")) {
      // Not a document opened over REST, do not register the header.
      return;
    }
    String contextId = URLUtil.getUserInfo(systemID);
    
    EditingSessionContext editingContext = documentModel.getAuthorAccess().getEditorAccess().getEditingContext();
    editingContext.setAttribute(REST_HEADERS_KEY, new Supplier<Map<String, String>>() {
      @Override
      public Map<String, String> get() {
        return RestURLConnection.credentialsMap.getIfPresent(contextId);
      }
    });
  };

  @Override
  public boolean applicationClosing() {
    return true;
  }

}
