package com.oxygenxml.rest.plugin;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

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

  private static final String USE_INVISIBLE_LOGIN_FORM = "rest.use_invisible_login_form";
  
  @Override
  public void init() throws ServletException {
    super.init();
    Map<String, String> defaultOptions = new HashMap<String, String>();

    defaultOptions.put(USE_INVISIBLE_LOGIN_FORM, "off");
    defaultOptions.put(REST_ROOT_REGEXP, "");
    defaultOptions.put(REST_SERVER_URL, "");

    this.setDefaultOptions(defaultOptions);
  }

  /**
   * @see ro.sync.ecss.extensions.api.webapp.plugin.PluginConfigExtension#getOptionsForm()
   */
  @Override
  public String getOptionsForm() {
    String serverURL = getOption(REST_SERVER_URL, "");
    boolean useInvisibleLoginForm = "on".equals(getOption(USE_INVISIBLE_LOGIN_FORM, "off"));
    PluginResourceBundle rb = ((WebappPluginWorkspace)PluginWorkspaceProvider.getPluginWorkspace()).getResourceBundle();
    
    StringBuilder restServerOptions = new StringBuilder()
      .append("<div style='font-family:robotolight, Arial, Helvetica, sans-serif;font-size:0.85em;font-weight: lighter'>")
      // REST Server URL input
      .append("<label style='display: block; margin-top: 20px;' >")
      .append("REST " + rb.getMessage(TranslationTags.SERVER_URL) + ": <input  name = '").append(REST_SERVER_URL).append("' value='").append(serverURL).append("' ")
      .append("style='width: 80%; line-height: 20px; border: 1px solid #777C7F; background-color: #f7f7f7; border-radius: 5px; padding-left: 7px; margin: 10px 0 0 10px; display: block;' ")
      .append("></input></label>")
      // The RegExp used to determine root url
      .append("<label style='display: block; margin-top: 20px;' title='" + rb.getMessage(TranslationTags.ROOT_REGEXP_DESCRIPTION) + "' >")
      .append(rb.getMessage(TranslationTags.ROOT_REGEXP) + ": <input  name = '")
      .append(REST_ROOT_REGEXP).append("' value='").append(getOption(REST_ROOT_REGEXP, "")).append("' ")
      .append("style='width: 80%; line-height: 20px; border: 1px solid #777C7F; background-color: #f7f7f7; border-radius: 5px; padding-left: 7px; margin: 10px 0 0 10px; display: block;' ")
      .append("></input></label>")
      // Use invisible login form
      .append("<label style='display:block;margin-top:20px;overflow:hidden'>")
      .append("<input name='" + USE_INVISIBLE_LOGIN_FORM + "' type='checkbox' value='on'")
      .append((useInvisibleLoginForm ? " checked" : "") + "> " + rb.getMessage(TranslationTags.USE_INVISIBLE_LOGIN))
      .append("</label>")

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
    boolean useInvisibleLoginForm = "on".equals(getOption(USE_INVISIBLE_LOGIN_FORM, "off"));
    
    return "{ restServerUrl: '" + serverURL + "'," +
        "restUseInvisibleLoginForm: '" + useInvisibleLoginForm + "'," +
        "restRootRegExp: '" + rootRegExp + "'}";
  }
}