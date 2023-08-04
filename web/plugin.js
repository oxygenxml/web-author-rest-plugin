(function() {

  var regExpOption = sync.options.PluginsOptions.getClientOption('restRootRegExp');
  var ROOT_REGEXP = regExpOption ? new RegExp(regExpOption) : null;

  goog.events.listen(workspace, sync.api.Workspace.EventType.BEFORE_EDITOR_LOADED, function(e) {
    var url = e.options.url;
    // If the URL has 'rest' protocol we use the rest protocol handler.
    if (url.match('rest:\/\/')) {
      // set the workspace UrlChooser
      workspace.setUrlChooser(fileBrowser);

      // Listen for messages sent from the server-side code.
      goog.events.listen(e.editor, sync.api.Editor.EventTypes.CUSTOM_MESSAGE_RECEIVED, function(e) {
        var context = e.context;
        // pop-up an authentication window,
        fileBrowser.loginUser(function() {
          var webappMessageReceivedContext = sync.api.Editor.WebappMessageReceived.Context;
          // After the user was logged in, retry the operation that failed.
          if (context === webappMessageReceivedContext.LOAD) {
            // If the document was loading, we try to reload the whole webapp.
            window.location.reload();
          } else if (context === webappMessageReceivedContext.EDITING) {
            // During editing, only references can trigger re-authentication. Refresh them.
            editor.getActionsManager().invokeAction('Author/Refresh_references');
          } else if (context === webappMessageReceivedContext.SAVE) {
            // Currently there is no API to re-try saving, but it will be.
            editor.getActionsManager().getActionById('Author/Save').actionPerformed(function() {
            });
          } else if (context === webappMessageReceivedContext.IMAGE) {
            // The browser failed to retrieve an image - reload it.
            var images = document.querySelectorAll('img[data-src]');
            for (var i = 0; i < images.length; i ++) {
              images[i].src = goog.dom.dataset.get(images[i], 'src');
            }
          }
        });
      });
    }
  });

  /**
   * Webdav url chooser.
   *
   * @constructor
   */
  var RestFileBrowser = function() {
    var latestUrl = this.getLatestUrl();
    var latestRootUrl = this.getLatestRootUrl();

    sync.api.FileBrowsingDialog.call(this, {
      initialUrl: latestUrl,
      root: latestRootUrl
    });

    // Listen for messages from the login-finished iframe
    window.addEventListener('message', function(msg) {
        if(msg.data.action === 'login-finished') {
          this.loginDialog && this.loginDialog.hide();

          var iframe = document.getElementById('rest-login-iframe');
          iframe.parentNode.removeChild(iframe);

          this.latestCallback && this.latestCallback();
        }
      }.bind(this));
  };
  goog.inherits(RestFileBrowser, sync.api.FileBrowsingDialog);

  /** @override */
  RestFileBrowser.prototype.renderRepoPreview = function(element) {
    var url = this.getCurrentFolderUrl();
    if (url) {
      var cD = goog.dom.createDom;

      element.style.paddingLeft = '5px';
      element.title = tr(msgs.Server_URL);

      // add an edit button only of there are no enforced servers
      // or there are more than one enforced server.
      var bgImageUrl = sync.util.getImageUrl('/images/SharePointWeb16.png', sync.util.getHdpiFactor());
      element.innerHTML = '';
      goog.dom.appendChild(element,
        cD('div', 'rest-repo-preview',
          cD('div', {className: 'domain-icon', style: 'background-image: url(' + bgImageUrl + '); vertical-align: middle;'}),
          new sync.util.Url(url).getDomain()
        )
      );
    }
    this.dialog.setPreferredSize(null, 700);
  };

  /** @override */
  RestFileBrowser.prototype.renderRepoEditing = function(element) {
    // We force the 'rest://cms/' URL if no other is provided.
    // only happens on Dashboard which is used in development
    this.requestUrlInfo_('rest://cms/');
  };


  /** @override */
  RestFileBrowser.prototype.handleOpenRepo = function(element, e) {
    var input = document.getElementById('rest-browse-url');
    var repoId = input.value.trim();

    if(repoId && new RegExp('^[a-zA-Z0-9_-]*$').test(repoId)) {
      var url = repoId;
      // add the protocol.
      if(url.indexOf('rest://') !== 0) {
        url = 'rest://' + url;
      }
      // add trailing slash
      if(url.substring(url.length -1) !== '/') {
        url += '/';
      }

      // if an url was provided we instantiate the file browsing dialog.
        this.requestUrlInfo_(url);
    } else {
      this.showErrorMessage(tr(msgs.INVALID_URL_));
      // hide the error element on input refocus.
      goog.events.listenOnce(input, goog.events.EventType.FOCUS,
        goog.bind(function(e) {this.hideErrorElement();}, this));
    }
    e.preventDefault();
  };

  /**
   * Request the URL info from the server.
   *
   * @param {string} url The URL about which we ask for information.
   * @param {function} opt_callback callback method to replace the openUrlInfo method.
   *
   * @private
   */
  RestFileBrowser.prototype.requestUrlInfo_ = function (url, opt_callback) {
    var callback = opt_callback || this.openUrlInfo;
    var type = url.endsWith('/') ? 'FOLDER' : 'FILE';
    var matches = ROOT_REGEXP && ROOT_REGEXP.exec(url);
    var rootUrl = matches ? matches[0] : null;

    callback.bind(this)(
      url,
      {
      rootUrl: rootUrl,
      type: type
    });
  };

  /**
   * Opens the url and sets it's url info.
   *
   * @param url the url to open.
   * @param info the available url information.
   *
   */
  RestFileBrowser.prototype.openUrlInfo = function(url, info) {
    var isFile = info.type === 'FILE';
    // Make sure folder urls end with '/'.
    if (!isFile && url.lastIndexOf('/') !== url.length - 1) {
      url = url + '/';
    }
    this.setUrlInfo(url, info);
    this.openUrl(url, isFile, null);
  };

  /**
   * Sets the information received about the url.
   *
   * @param url the url whose info to set.
   * @param info the available url information.
   *
   */
  RestFileBrowser.prototype.setUrlInfo = function(url, info) {
    if(info.rootUrl) {
      var rootUrl = info.rootUrl;
      this.setRootUrl(rootUrl);
    }
    this.setInitialUrl_(url);
  };

  /**
   *
   * @return {string} the latest root url.
   */
  RestFileBrowser.prototype.getLatestRootUrl = function() {
    var newRoot = null;
    var urlParam = decodeURIComponent(sync.util.getURLParameter('url'));
    if(urlParam && urlParam.match('rest:\/\/')) {
      var matches = ROOT_REGEXP && ROOT_REGEXP.exec(decodeURIComponent(urlParam));
      newRoot = matches ? matches[0] : null;
    }
    return newRoot;
  };

  /**
   * Getter of the last usedUrl.
   *
   * @return {String|null} the last set url.
   */
  RestFileBrowser.prototype.getLatestUrl = function() {
    var urlParam = sync.util.getURLParameter('url');
    if(urlParam) {
      return decodeURIComponent(urlParam);
    } else {
      return null;
    }
  };

  /**
   * The user needs to authenticate.
   *
   * @param {function} callback the callback method that should be called after login.
   */
  RestFileBrowser.prototype.loginUser = function(callback) {
    this.latestCallback = callback;
    var useDialogLogin = "false" === sync.options.PluginsOptions.getClientOption('restUseInvisibleLoginForm');
    if(useDialogLogin) {
      this.dialogUserLogin(callback);
    } else {
      this.invisibleUserLogin(callback);
    }
  };

  /**
   * Login the user using a dialog with and iframe inside.
   *
   * @param callback a method to call when the login process has finished.
   */
  RestFileBrowser.prototype.dialogUserLogin = function(callback) {
    if(!this.loginDialog) {
      this.loginDialog = this.createLoginDialog();
    }

    this.loginDialog.getElement().innerHTML =
      '<iframe id="rest-login-iframe" style="width:100%; height:100%;border:none;" src="' +
      sync.options.PluginsOptions.getClientOption('restServerUrl') + 'rest-login"></iframe>'

    this.loginDialog.show();
    this.loginDialog.onSelect(function(e) {
      this.loginDialog.hide();
      callback();
    }.bind(this));
  };

  /**
   * Creates and ivisible iframe that loads the login code.
   */
  RestFileBrowser.prototype.invisibleUserLogin = function() {
    var iframe = goog.dom.createDom('iframe', {
      src: sync.options.PluginsOptions.getClientOption('restServerUrl') + 'rest-login',
      id: 'rest-login-iframe',
      style: 'display:none;'
    });
    document.body.appendChild(iframe);
  };

  /**
   * Creates the rest connector login dialog.
   *
   * @return {*}
   */
  RestFileBrowser.prototype.createLoginDialog = function() {
    var loginDialog = workspace.createDialog();
    loginDialog.setButtonConfiguration(sync.api.Dialog.ButtonConfiguration.CANCEL);
    var dialogElem = loginDialog.getElement();
    dialogElem.style.overflow = 'hidden';

    loginDialog.setPreferredSize(800, 600);
    loginDialog.setResizable(true);
    return loginDialog;
  };

  /**
   * Register all the needed listeners on the file browser.
   *
   * @param {sync.api.FileBrowsingDialog} fileBrowser
   *  the file browser on which to listen.
   */
  var registerFileBrowserListeners = function(fileBrowser) {
    // handle the user action required event.
    var eventTarget = fileBrowser.getEventTarget();
    goog.events.listen(eventTarget,
      sync.api.FileBrowsingDialog.EventTypes.USER_ACTION_REQUIRED,
      function() {
        this.loginUser(function() {
          this.refresh();
        });
      }.bind(fileBrowser));
  };
  /**
   * We do not registed the file browser if the base REST Server URL is not set.
   */
  var restServerURL = sync.options.PluginsOptions.getClientOption('restServerUrl');
  if(!restServerURL) {
    return;
  }

  var fileBrowser = new RestFileBrowser();
  // register all the listeners on the file browser.
  registerFileBrowserListeners(fileBrowser);

  var restOpenAction = new sync.actions.OpenAction(fileBrowser);
  restOpenAction.setDescription('Open document from REST server');
  restOpenAction.setActionId('rest-open-action');
  restOpenAction.setActionName('Rest');

  var webdavCreateAction = new sync.api.CreateDocumentAction(fileBrowser);
  webdavCreateAction.setDescription('Create a new document on a REST server');
  webdavCreateAction.setActionId('rest-create-action');
  webdavCreateAction.setActionName('Rest');

  var actionsManager = workspace.getActionsManager();
  actionsManager.registerOpenAction(restOpenAction);
  actionsManager.registerCreateAction(webdavCreateAction);

})();
