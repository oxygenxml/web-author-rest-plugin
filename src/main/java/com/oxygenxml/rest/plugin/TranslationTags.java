package com.oxygenxml.rest.plugin;

public interface TranslationTags {

  /**
   * The server URL translation tag.
   *  
   * en: Server URL
   */
  String SERVER_URL = "Server_URL";
  
  /**
   * Label for text input for setting the regular expression that determines the root directory.
   * 
   * en: Root RegExp
   */
  String ROOT_REGEXP = "Root_regexp";
  
  /**
   * Tooltip explaining that the input value required is a regular expression which will determine the root directory.
   * 
   * en: Regular expression applied over the URL to determine the RootURL.
   */
  String ROOT_REGEXP_DESCRIPTION = "Root_regexp_description";

  /**
   * Informs the user that the login was successful.
   * 
   * en: Rest server succeeded in logging in the user
   */
  String LOGIN_SUCCESS = "REST_login_success";

  /**
   * Title of login dialog.
   * 
   * en: Authentication required
   */
  String AUTHENTICATION_REQUIRED = "Authentication_required";

  /**
   * Error message informing the user that the REST Server url option is not set.
   * 
   * en: The REST server URL option is not set.
   */
  String UNCONFIGURED_REST_SERVER_URL = "Unconfigured_REST_server_url";

  /**
   * The requested file was not found.
   * 
   * en: File not found
   */
  String FILE_NOT_FOUND = "File_not_found";
  
  /**
   * The label of the checkbox that configures whether the plugin users the invisible login mechanism.
   * 
   * en: Use invisible login form.
   */
  String USE_INVISIBLE_LOGIN = "use_invisible_login";

}
