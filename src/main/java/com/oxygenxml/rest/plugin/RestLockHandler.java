package com.oxygenxml.rest.plugin;

import java.net.URL;

import ro.sync.ecss.extensions.api.webapp.plugin.LockHandlerWithContext;
import ro.sync.exml.plugin.lock.LockException;

/**
 * Lock handler for the WebDAV protocol.
 * 
 * @author cristi_talau
 */
public class RestLockHandler extends LockHandlerWithContext {

  /**
   * @see LockHandlerWithContext#isSaveAllowed(String, URL, int)
   */
  @Override
  public boolean isSaveAllowed(String sessionId, URL url, int timeoutSeconds) {
    // TODO:
    return true;
  }

  /**
   * @see LockHandlerWithContext#unlock(String, URL)
   */
  @Override
  public void unlock(String sessionId, URL url) throws LockException {
//    try {
//      url = RestURLStreamHandler.addCredentials(sessionId, url);
//    } catch (UserActionRequiredException e) {
//      // The user should be already authenticated. Anyway, we cannot do anything 
//      // from this method call.
//      logger.debug(e, e);
//    }
//    new WebdavLockHelper().unlock(sessionId, url);
  }

  /**
   * @see LockHandlerWithContext#updateLock(String, URL, int)
   */
  @Override
  public void updateLock(String sessionId, URL url, int timeoutSeconds) throws LockException {
//    try {
//      url = RestURLStreamHandler.addCredentials(sessionId, url);
//    } catch (UserActionRequiredException e) {
//      // The user should be already authenticated. Anyway, we cannot do anything 
//      // from this method call.
//      logger.debug(e, e);
//    }
//    WebdavLockHelper lockHelper = new WebdavLockHelper();
//    Map<String, Map<String, Enumeration<String>>> credentialsMap = RestURLStreamHandler.serversMap.getIfPresent(sessionId);
//    String serverId = RestURLStreamHandler.computeServerId("rest-" + url.toExternalForm());
//    
//    Map<String, Enumeration<String>> serverHeaders = null;
//    
//    if(credentialsMap != null) {
//      serverHeaders = credentialsMap.get(serverId);
//    }
    // TODO: lock the document.
//    String userName = passwordAuthentication != null ? passwordAuthentication.getUserName() : "Anonymous";
//    lockHelper.setLockOwner(sessionId, userName);
//
//    lockHelper.updateLock(sessionId, url, timeoutSeconds);
  }

  /**
   * @see LockHandlerWithContext#isLockEnabled()
   */
  @Override
  public boolean isLockEnabled() {
    // TODO: make this optional.
//    WSOptionsStorage optionsStorage = PluginWorkspaceProvider.getPluginWorkspace().getOptionsStorage();
    
//    String optionValue = optionsStorage.getOption(WebdavPluginConfigExtension.LOCKING_ENABLED, "on");
//    return "on".equals(optionValue);
    return false;
  }
}
