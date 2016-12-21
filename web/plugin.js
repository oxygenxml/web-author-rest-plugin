(function() {
  goog.events.listen(workspace, sync.api.Workspace.EventType.BEFORE_EDITOR_LOADED, function(e) {
    var url = e.options.url;
    // If the URL starts with http:, use thw webdav protocol handler.
    if (url.match(/^rest-https?:/)) {
      // set the workspace UrlChooser
      workspace.setUrlChooser(fileBrowser);

      // The editor is about to be loaded.
      var editor = e.editor;

      // Register the toolbar actions.
      goog.events.listenOnce(editor, sync.api.Editor.EventTypes.ACTIONS_LOADED, function(e) {
        var logoutAction = new LogOutAction(editor);
        var logoutActionId = 'Rest/Logout';
        editor.getActionsManager().registerAction(logoutActionId, logoutAction);
        var toolbar = e.actionsConfiguration.toolbars[0];

        var moreMenu = toolbar.children[toolbar.children.length - 1];
        moreMenu.children.push(
          {id: logoutActionId, type: "action"}
        );
      });

      // Listen for messages sent from the server-side code.
      goog.events.listen(editor, sync.api.Editor.EventTypes.CUSTOM_MESSAGE_RECEIVED, function(e) {
        var context = e.context;
        var url = e.message.message;

        // pop-up an authentication window,
        console.log('We should login');
      });
    }
  });

  /**
   * The Log out action for WebDAV
   *
   * @constructor
   */
  function LogOutAction (editor) {
    this.editor = editor;
  }
  goog.inherits(LogOutAction, sync.actions.AbstractAction);

  /**
   * Constructs and returns the log-out confirmation dialog.
   *
   * @return {sync.api.Dialog} The dialog used to confirm teh log-out action.
   */
  LogOutAction.prototype.getDialog = function() {
    if (!this.dialog) {
      this.dialog = workspace.createDialog();
      this.dialog.setTitle('Log out');
      this.dialog.setButtonConfiguration([{key: 'yes', caption: 'Logout'}, {key: 'no', caption: 'Cancel'}]);

      var dialogHtml = '<div><div>';
      dialogHtml += 'Are you sure you want to log-out? ';
      if (this.editor && this.editor.isDirty()) {
        dialogHtml += '<b>All your unsaved changes will be lost</b>'
      }
      dialogHtml += '</div></div>';

      this.dialog.getElement().innerHTML = dialogHtml;
    }
    return this.dialog;
  };

  /**
   * Called when the Logout button is clicked
   *
   * @override
   */
  LogOutAction.prototype.actionPerformed = function() {
    this.dialog = this.getDialog();
    this.dialog.onSelect(goog.bind(function (choice, e) {
    	// TODO: handle logout.
      if (choice == 'yes') {
        e.preventDefault();
        goog.net.XhrIo.send(
          '../plugins-dispatcher/login?action=logout',
          goog.bind(function () {
            // hide the dialog once we logged out.
            this.dialog.hide();

            localStorage.removeItem('rest.latestUrl');
            localStorage.removeItem('rest.latestRootUrl');
            localStorage.removeItem('rest.user');

            // if we are editing we go to dashboard.
            if(sync.util.getURLParameter('url')) {
              this.editor && this.editor.setDirty(false);
              sync.util.setUrlParameter('url');
              window.location.reload();
            } else {
              // on dashboard, hide the dialogs.
              fileBrowser.switchToRepoConfig();
              fileBrowser.dialog.hide();
              fileBrowser.candidateUrl = null;
            }
          }, this),
          'POST');
      }
    }, this));
    this.dialog.setPreferredSize(320, 185);
    this.dialog.show();
  };

  /** @override */
  LogOutAction.prototype.getDisplayName = function() {
    return "Logout";
  };

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
      var content = '<div class="webdav-repo-preview">' +
        '<div class="domain-icon" style="' +
        'background-image: url(' + sync.util.getImageUrl('/images/SharePointWeb16.png', sync.util.getHdpiFactor()) +
        ');vertical-align: middle"></div>' +
        new sync.util.Url(url).getDomain();
      // add an edit button only of there are no enforced servers
      // or there are more than one enforced server.
      content += '</div>'
      element.innerHTML = content;
      var button = element.querySelector('.webdav-domain-edit');
      if(button) {
        button.title = "Edit server URL";
        goog.events.listen(button, goog.events.EventType.CLICK,
          goog.bind(this.switchToRepoConfig, this, element))
      }
    }
    this.dialog.setPreferredSize(null, 700);
  };

  /** @override */
  RestFileBrowser.prototype.renderRepoEditing = function(element) {
    if(false/*this.enforcedServers.length > 0*/) {
      var dialogContent = '<div class="enforced-servers-config">' +
        'Server URL: <select id="rest-browse-url">';
      var i;
      for(i = 0; i < this.enforcedServers.length; i++) {
        var serverUrl = this.enforcedServers[i];
        if(serverUrl) {
          dialogContent += '<option value="' + serverUrl + '" '
          dialogContent += (serverUrl == localStorage.getItem('rest.latestEnforcedURL') ? 'selected' : '') + '>';
          dialogContent += serverUrl;
          dialogContent += '</option>';
        }
      }
      dialogContent += '</select></div>';
      element.innerHTML = dialogContent;
    } else {
      var url = this.getCurrentFolderUrl();
      var latestUrl = this.getLatestUrl();
      // if none was set we let it empty.
      var editUrl = latestUrl || url || '';
      if (editUrl && (editUrl.indexOf('rest-') == 0)) {
        editUrl = editUrl.substring(7);
      }
      var button = element.querySelector('.webdav-domain-edit');
      element.title = "";
      goog.events.removeAll(button);

      element.style.paddingLeft = '5px';
      // the webdavServerPlugin additional content.
      var wevdavServerPluginContent = '';
      // if the webdav-server-plugin is installed display a button to use it.
      if (this.isServerPluginInstalled) {
        wevdavServerPluginContent =
          '<div class="webdav-builtin-server">' +
          '<div class="webdav-use-builtin-btn">Use built-in server</div>' +
          '<input readonly class="webdav-builtin-url" value="' + webdavServerPluginUrl + '">' +
          '</div>';
      }
      element.innerHTML =
        '<div class="webdav-config-dialog">' +
        '<label>Server URL: <input id="rest-browse-url" type="text" autocorrect="off" autocapitalize="none" autofocus/></label>' +
        wevdavServerPluginContent +
        '</div>';
      element.querySelector('#rest-browse-url').value = editUrl;
    }
    var prefferedHeight = 190;
    this.dialog.setPreferredSize(null, prefferedHeight);
  };

  /** @override */
  RestFileBrowser.prototype.handleOpenRepo = function(element, e) {
    var input = document.getElementById('rest-browse-url');
    var url = input.value.trim();

    // if an url was provided we instantiate the file browsing dialog.
    if(url) {
      if(url.match('(rest-)?https?:\/\/')) {
        var processedUrl = this.processURL(url);
        if(!processedUrl.startsWith('rest-')) {
          processedUrl = 'rest-' + processedUrl;
        }
        this.requestUrlInfo_(processedUrl);
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
    var restPrefix = 'rest-';
    if(url && url.indexOf(restPrefix) == 0) {
      url = url.substring(restPrefix.length);
    }

    var callback = opt_callback || goog.bind(this.openUrlInfo, this);
    goog.net.XhrIo.send(
      '../plugins-dispatcher/rest-url-info?url=' + encodeURIComponent(url),
      goog.bind(this.handleUrlInfoReceived, this, url, callback));
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
    	console.log('login');
    	
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
    // TODO: uncomment local storage settings.
    if(info.rootUrl) {
      var rootUrl = this.processURL(info.rootUrl);
      //localStorage.setItem('rest.latestRootUrl', rootUrl);
      this.setRootUrl(rootUrl);
    } else {
      localStorage.removeItem('rest.latestRootUrl')
    }

    var urlObj = new sync.util.Url(url);
    //localStorage.setItem('rest.latestUrl', urlObj.getFolderUrl());
    this.setInitialUrl_(url);
  };

  /**
   * Further processes the url.
   *
   * @param url the url to process.
   *
   * @return {string} the processed url.
   */
  RestFileBrowser.prototype.processURL = function(url) {
    var processedUrl = url;

    // if the url does not start with 'rest' prepend it to the url.
    if(!(url.indexOf('rest-') == 0)) {
      processedUrl = 'rest-' + processedUrl;
    }
    return processedUrl;
  };

  /**
   *
   * @return {string} the latest root url.
   */
  RestFileBrowser.prototype.getLatestRootUrl = function() {
    var lastRootUrl = this.enforcedUrl || localStorage.getItem('rest.latestRootUrl');
    if (!lastRootUrl && this.isServerPluginInstalled) {
      lastRootUrl = webdavServerPluginUrl;
    }
    return lastRootUrl;
  };

  /**
   * Getter of the last usedUrl.
   *
   * @return {String} the last set url.
   */
  RestFileBrowser.prototype.getLatestUrl = function() {
    var latestUrl = localStorage.getItem('rest.latestUrl');
    // if the latest url is not in local storage we check if the
    // webdav-server-plugin is installed and we use it.
    if(!latestUrl && this.isServerPluginInstalled) {
      latestUrl = webdavServerPluginUrl;
    }

    return latestUrl;
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
      function (e) {
        var url = e.message.message;

        console.log('login');
        
      });
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
    if (currDocUrl && currDocUrl.match(/^rest-https?:/)) {
      var lastRootUrl = localStorage.getItem('rest.latestRootUrl');
      // If the latest root url is not a parent of the current document url, we need to compute the root url.
      if (!lastRootUrl || currDocUrl.indexOf(lastRootUrl) === -1) {
        fileBrowser.requestUrlInfo_('rest-' + currDocUrl,
          goog.bind(fileBrowser.setUrlInfo, fileBrowser));
      }
    }
  });
  // TODO: change icon
  // the large icon url, hidpi enabled.
  var iconUrl = sync.util.computeHdpiIcon('../plugin-resources/webdav/Webdav70.png');

  var webdavOpenAction = new sync.actions.OpenAction(fileBrowser);
  webdavOpenAction.setLargeIcon(iconUrl);
  webdavOpenAction.setDescription('Open document from WebDAV server');
  webdavOpenAction.setActionId('rest-open-action');
  webdavOpenAction.setActionName('Rest');

  var webdavCreateAction = new sync.api.CreateDocumentAction(fileBrowser);
  webdavCreateAction.setLargeIcon(iconUrl);
  webdavCreateAction.setDescription('Create a new document on a WebDAV server');
  webdavCreateAction.setActionId('rest-create-action');
  webdavCreateAction.setActionName('Rest');

  var actionsManager = workspace.getActionsManager();
  actionsManager.registerOpenAction(webdavOpenAction);
  actionsManager.registerCreateAction(webdavCreateAction);

  sync.util.loadCSSFile("../plugin-resources/webdav/webdav.css");
})();
