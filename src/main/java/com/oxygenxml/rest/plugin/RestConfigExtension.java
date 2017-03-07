package com.oxygenxml.rest.plugin;

import ro.sync.ecss.extensions.api.webapp.access.WebappPluginWorkspace;
import ro.sync.ecss.extensions.api.webapp.plugin.PluginConfigExtension;
import ro.sync.exml.workspace.api.PluginResourceBundle;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;

public class RestConfigExtension  extends PluginConfigExtension {

  /**
   * The rest server URL option.
   */
  final static String REST_SERVER_URL = "rest.server_url";
  
  /**
   * The RexExp string that determines the root url.
   */
  final static String REST_ROOT_REGEXP = "rest.root_regexp";
  
  
  /**
   * @see ro.sync.ecss.extensions.api.webapp.plugin.PluginConfigExtension#getOptionsForm()
   */
  @Override
  public String getOptionsForm() {
    String serverURL = getOption(REST_SERVER_URL, "");
    PluginResourceBundle rb = ((WebappPluginWorkspace)PluginWorkspaceProvider.getPluginWorkspace()).getResourceBundle();
    StringBuilder restServerOptions = new StringBuilder()
      .append("<div style='font-family:robotolight, Arial, Helvetica, sans-serif;font-size:0.85em;font-weight: lighter'>")
      
      .append("<label style='display: block; margin-top: 50px;' >")
      .append("REST " + rb.getMessage("Server_URL") + ": <input  name = '").append(REST_SERVER_URL).append("' value='").append(serverURL).append("' ")
      .append("style='width: 350px; line-height: 20px; border: 1px solid #777C7F; background-color: #f7f7f7; border-radius: 5px; padding-left: 7px;' ")
      .append("></input></label>")
      // The RegExp used to determine root url
      .append("<label style='display: block; margin-top: 20px;' title='" + rb.getMessage("Root_regexp_description") + "' >")
      .append(rb.getMessage("Root_regexp") + ": <input  name = '")
      .append(REST_ROOT_REGEXP).append("' value='").append(getOption(REST_ROOT_REGEXP, "")).append("' ")
      .append("style='width: 350px; float:right; line-height: 20px; border: 1px solid #777C7F; background-color: #f7f7f7; border-radius: 5px; padding-left: 7px;' ")
      .append("></input></label>")
      
      .append("</div>");
    
    return restServerOptions.toString();
  }

  @Override
  public String getPath() {
    return "rest-config";
  }

  @Override
  public String getOptionsJson() {
    String serverURL = getOption(REST_SERVER_URL, "");
    String rootRegExp = getOption(REST_ROOT_REGEXP, "");
    
    return "{ restServerUrl: '" + serverURL + "'," +
        "restRootRegExp: '" + rootRegExp + "'}";
  }
}