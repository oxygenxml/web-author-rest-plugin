package com.oxygenxml.rest.plugin;

import ro.sync.ecss.extensions.api.webapp.plugin.PluginConfigExtension;

public class RestConfigExtension  extends PluginConfigExtension {

  /**
   * The auto-save interval (in seconds).
   */
  final static String REST_SERVER_URL = "rest.server_url";
  
  /**
   * @see ro.sync.ecss.extensions.api.webapp.plugin.PluginConfigExtension#getOptionsForm()
   */
  @Override
  public String getOptionsForm() {
    String serverURL = getServerURL();
    
    StringBuilder restServerOptions = new StringBuilder()
      .append("<div style='font-family:robotolight, Arial, Helvetica, sans-serif;font-size:0.85em;font-weight: lighter'>")
      .append("<label style='display: block; margin-top: 50px;' >")
      .append("REST Server URL: <input  name = '" + REST_SERVER_URL + "' value='" + serverURL + "' ")
      .append("style='width: 350px; line-height: 20px; border: 1px solid #777C7F; background-color: #f7f7f7; border-radius: 5px;' ")
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
    String serverURL = getServerURL();
    return "{ restServerUrl: '" + serverURL + "'}";
  }

  /**
   * Fetches the rest server URL from the options.
   * 
   * @return the rest server base url.
   */
  private String getServerURL() {
    return getOption(REST_SERVER_URL, "");
  }
}