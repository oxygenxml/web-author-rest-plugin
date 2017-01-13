(function() {
  goog.events.listen(workspace, sync.api.Workspace.EventType.BEFORE_EDITOR_LOADED, function(e) {
    var url = e.options.url;
    // If the URL has 'rest' protocol we use the rest protocol handler.
    if (url.match('rest:\/\/')) {
      // set the workspace UrlChooser
      workspace.setUrlChooser(fileBrowser);

      // Listen for messages sent from the server-side code.
      goog.events.listen(e.editor, sync.api.Editor.EventTypes.CUSTOM_MESSAGE_RECEIVED, function(e) {
        var context = e.context;
        var url = e.message.message;
        // pop-up an authentication window,
        fileBrowser.loginUser(function() {
          // After the user was logged in, retry the operation that failed.
          if (context == sync.api.Editor.WebappMessageReceived.Context.LOAD) {
            // If the document was loading, we try to reload the whole webapp.
            window.location.reload();
          } else if (context == sync.api.Editor.WebappMessageReceived.Context.EDITING) {
            // During editing, only references can trigger re-authentication. Refresh them.
            editor.getActionsManager().invokeAction('Author/Refresh_references');
          } else if (context == sync.api.Editor.WebappMessageReceived.Context.SAVE) {
            // Currently there is no API to re-try saving, but it will be.
            editor.getActionsManager().getActionById('Author/Save').actionPerformed(function() {
            });
          } else if (context == sync.api.Editor.WebappMessageReceived.Context.IMAGE) {
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
  };
  goog.inherits(RestFileBrowser, sync.api.FileBrowsingDialog);

  /** @override */
  RestFileBrowser.prototype.renderRepoPreview = function(element) {
    var url = this.getCurrentFolderUrl();
    if (url) {
      element.style.paddingLeft = '5px';
      element.title = "Server URL";
      var content = '<div class="rest-repo-preview">' +
        '<div class="domain-icon" style="' +
        'background-image: url(' + sync.util.getImageUrl('/images/SharePointWeb16.png', sync.util.getHdpiFactor()) +
        ');vertical-align: middle"></div>' +
        new sync.util.Url(url).getDomain();
      // add an edit button only of there are no enforced servers
      // or there are more than one enforced server.
      content += '<div class="rest-domain-edit"></div>';
      content += '</div>'
      element.innerHTML = content;
      var button = element.querySelector('.rest-domain-edit');
      if(button) {
        button.title = "Edit server URL";
        goog.events.listen(button, goog.events.EventType.CLICK,
          goog.bind(this.switchToRepoConfig, this, element));
      }
    }
    this.dialog.setPreferredSize(null, 700);
  };

  /** @override */
  RestFileBrowser.prototype.renderRepoEditing = function(element) {
    var url = this.getCurrentFolderUrl();
    var latestUrl = this.getLatestUrl();
    // if none was set we let it empty.
    var editUrl = latestUrl || url || '';

    var button = element.querySelector('.rest-domain-edit');
    element.title = "";
    goog.events.removeAll(button);

    element.style.paddingLeft = '5px';
    // the webdavServerPlugin additional content.
    element.innerHTML =
      '<div class="rest-config-dialog">' +
      '<label>Server URL: <input id="rest-browse-url" type="text" autocorrect="off" autocapitalize="none" autofocus/></label>' +
      '</div>';
    element.querySelector('#rest-browse-url').value = editUrl;

    var prefferedHeight = 190;
    this.dialog.setPreferredSize(null, prefferedHeight);
  };

  /** @override */
  RestFileBrowser.prototype.handleOpenRepo = function(element, e) {
    var input = document.getElementById('rest-browse-url');
    var url = input.value.trim();

    // if an url was provided we instantiate the file browsing dialog.
    if(url) {
      if(url.match('rest:\/\/')) {
        this.requestUrlInfo_(url);
      } else {
        this.showErrorMessage('Invalid URL inserted.');
        // hide the error element on input refocus.
        goog.events.listenOnce(input, goog.events.EventType.FOCUS,
          goog.bind(function(e) {this.hideErrorElement();}, this));
      }
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
    // TODO: demo only, we have to find a way to determine the true type of the URL.
    var type = url.endsWith('/') ? 'FOLDER' : 'FILE';
    this.openUrlInfo(
      url,
      {
      rootUrl: url,
      type: type
    });
  };

  /**
   * URL information received from the server, we can open that URL in the dialog.
   *
   * @param {string} url The URL about which we requested info.
   * @param {function} callback the callback method.
   *
   * @param {goog.events.Event} e The XHR event.
   */
  RestFileBrowser.prototype.handleUrlInfoReceived = function (url, callback, e) {
    var request = /** {@type goog.net.XhrIo} */ (e.target);
    var status = request.getStatus();
    if (status == 200) {
      var info = request.getResponseJson();
      callback(url, info);
    } else if (status == 401) {
      this.loginUser(function() {
        goog.bind(this.requestUrlInfo_, this, url, callback);
      }.bind(this));
    } else {
      this.showErrorMessage('Cannot open this URL');
    }
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
  }

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
    var urlObj = new sync.util.Url(url);
    this.setInitialUrl_(url);
  };

  /**
   *
   * @return {string} the latest root url.
   */
  RestFileBrowser.prototype.getLatestRootUrl = function() {
    return 'rest://platform/';
  };

  /**
   * Getter of the last usedUrl.
   *
   * @return {String} the last set url.
   */
  RestFileBrowser.prototype.getLatestUrl = function() {
    return 'rest://platform/';
  };

  /**
   * The user needs to authenticate.
   *
   * @param {function} callback the callback method that should be called after login.
   */
  RestFileBrowser.prototype.loginUser = function(callback) {
    if(!this.loginDialog) {
      this.loginDialog = this.createLoginDialog();
    }

    this.latestCallback = callback;
    this.loginDialog.getElement().innerHTML =
      '<iframe id="rest-login-iframe" style="width:100%; height:100%;border:none;" src="' +
      sync.options.PluginsOptions.getClientOption('restServerUrl') + 'rest-login"></iframe>'

    this.loginDialog.show();
    this.loginDialog.onSelect(function(e) {
      this.dialog.hide();
    }.bind(this));
  };


  /**
   * Creates the rest connector login dialog.
   *
   * @return {*}
   */
  RestFileBrowser.prototype.createLoginDialog = function() {
    // Listen for messages from the login iframe
    window.addEventListener('message',
      function(msg) {
        if(msg.data.action == 'login') {
          this.loginDialog.hide();
          this.loginDialog.getElement().innerHTML = '';

          this.latestCallback && this.latestCallback();
        }
      }.bind(this));

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
  goog.events.listen(workspace, sync.api.Workspace.EventType.EDITOR_LOADED, function(e) {
    var currDocUrl = e.editor.getUrl();

    // if the current root and url are not set we use the current document url.
    if (currDocUrl && currDocUrl.match('rest:\/\/')) {
      fileBrowser.requestUrlInfo_(currDocUrl,
        goog.bind(fileBrowser.setUrlInfo, fileBrowser));
    }
  });

  var webdavOpenAction = new sync.actions.OpenAction(fileBrowser);
  webdavOpenAction.setDescription('Open document from WebDAV server');
  webdavOpenAction.setActionId('rest-open-action');
  webdavOpenAction.setActionName('Rest');

  var webdavCreateAction = new sync.api.CreateDocumentAction(fileBrowser);
  webdavCreateAction.setDescription('Create a new document on a WebDAV server');
  webdavCreateAction.setActionId('rest-create-action');
  webdavCreateAction.setActionName('Rest');

  var actionsManager = workspace.getActionsManager();
  actionsManager.registerOpenAction(webdavOpenAction);
  actionsManager.registerCreateAction(webdavCreateAction);

  sync.util.loadCSSFile("../plugin-resources/rest-resources/rest.css");
})();
