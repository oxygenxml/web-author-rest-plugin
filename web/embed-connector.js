
//return;


(function() {
  // run this code only when embeded.
  if(sync.util.getURLParameter('embedded') !== 'true') {
    return;
  }

  var Actions = {
    SET_READ_ONLY_STATUS: 'setReadOnly',
    INITIALIZE: 'initialize'
  };

  /**
   * The Embedded connector.
   *
   * @constructor
   */
  var EmbeddedConnector = function() {

    /**
     * A list of listeners which should be called when the link opened event is fired.
     * @type {Array<function>}
     * @private
     */
    this.linkOpenedListeners_ = [];

    // listen for post messages
    window.addEventListener('message', this.messageReceived.bind(this), false);

    goog.events.listen(workspace, sync.api.Workspace.EventType.BEFORE_EDITOR_LOADED,
      this.beforeEditorLoadedListener.bind(this));

    goog.events.listen(workspace, sync.api.Workspace.EventType.EDITOR_LOADED,
      function() {
        this.editorLoaded = true;
      }.bind(this));

    this.saveCallbacks = [];
    this.dirtyStatusListeners_ = [];
  };

  /**
   * Handler method for the BEFORE_EDITOR_LOADED event.
   *
   * @param {sync.api.Workspace.BeforeEditorOpenedEvent} e the event.
   */
  EmbeddedConnector.prototype.beforeEditorLoadedListener = function(e) {
    var editor = e.editor;
    // remove the ToggleChangeTracking action from the toolbar.
    goog.events.listen(editor, sync.api.Editor.EventTypes.ACTIONS_LOADED,
      this.actionsLoadedListener.bind(this, editor));

    goog.events.listen(editor, sync.api.Editor.EventTypes.LINK_OPENED, goog.bind(function(e) {
      for (var i = 0; i < this.linkOpenedListeners_.length; ++i) {
        this.linkOpenedListeners_[i](e);
      }
    }, this));

    goog.events.listen(editor, sync.api.Editor.EventTypes.DIRTY_STATUS_CHANGED, goog.bind(function(e) {
      for (var i = 0; i < this.dirtyStatusListeners_.length; ++i) {
        this.dirtyStatusListeners_[i](e);
      }
    }, this));
  };

  /**
   * Handler method for the ACTIONS_LOADED event.
   *
   * @param editor the editor.
   * @param e the event.
   */
  EmbeddedConnector.prototype.actionsLoadedListener = function(editor, e) {
    // replace the save action with one that offers hooks for before and after save.
    setTimeout(function() {
      if(!this.replacedSave) {
        this.replacedSave = true;
        var saveAction = editor.getActionsManager().getActionById("Author/Save");
        if (saveAction) {
          var oldActionPerformed = saveAction.actionPerformed.bind(saveAction);
          saveAction.actionPerformed = function(callback) {
            oldActionPerformed(function() {
              var i;
              for(i = 0; i < this.saveCallbacks.length; i++) {
                this.saveCallbacks[i]();
              }
              callback();
            }.bind(this));
          }.bind(this);
        }
      }
    }.bind(this), 0);
  };

  /**
   * Adds a listener for the link opened event.
   * @param {function} listener The listener to add.
   */
  EmbeddedConnector.prototype.addLinkOpenedListener = function (listener) {
    this.linkOpenedListeners_.push(listener);
  };

  /**
   * Removes a listener for the link opened event.
   * @param {function} listener The listener to remove.
   */
  EmbeddedConnector.prototype.removeLinkOpenedListener = function (listener) {
    goog.array.remove(this.linkOpenedListeners_, listener);
  };

  /**
   * Add a method that is called after the save action was invoked.
   *
   * @param {function} listener
   */
  EmbeddedConnector.prototype.addSaveListener = function(listener) {
    this.saveCallbacks.push(listener);
  };

  /**
   * Remove a save listener.
   *
   * @param {function} listener
   */
  EmbeddedConnector.prototype.removeSaveListener = function(listener) {
    goog.array.remove(this.saveCallbacks, listener);
  };

  /**
   * Add a method that is whenever the dirty status of the editor was changed.
   *
   * @param {function} listener
   */
  EmbeddedConnector.prototype.addDirtyStatusListener = function(listener) {
    this.dirtyStatusListeners_.push(listener);
  };

  /**
   * Remove a dirty status listener.
   *
   * @param {function} listener
   */
  EmbeddedConnector.prototype.removeSaveListener = function(listener) {
    goog.array.remove(this.dirtyStatusListeners_, listener);
  };

  /**
   * Function that handles all the messages received from the embeding library.
   *
   * @param e the message received message.
   */
  EmbeddedConnector.prototype.messageReceived = function(e) {
    var message = e.data;
    var action = message.action;
    var params = message.params;

    switch (action) {
      case Actions.SET_READ_ONLY_STATUS:
        this.setReadOnlyStatus(params);

        break;
      case Actions.INITIALIZE:
        // initialize only once.
        this.initialize(params);

        break;
    }
  };

  /**
   * Sets the readonly status based on the received parameters.
   *
   * @param params the parameters object that determines the readonly status.
   */
  EmbeddedConnector.prototype.setReadOnlyStatus = function(params) {
    if(this.editorLoaded) {
      var newValue = params.readOnly;
      var reason = params.reason || '';
      var op = new sync.ops.ServerOperation('ro.sync.ecss.extensions.commons.operations.SetReadOnlyStatusOperation',
        workspace.currentEditor.controller);

      op.doOperation(function(e) {
        },
        {
          "read-only": newValue,
          reason: reason
        }, null);
    } else {
      goog.events.listen(workspace, sync.api.Workspace.EventType.EDITOR_LOADED,
        this.setReadOnlyStatus.bind(this, params));
    }
  };

  /**
   * Initialize the WebAuthor based on the options passed by the embedding app.
   *
   * @param params init params.
   */
  EmbeddedConnector.prototype.initialize = function(params) {
    if( !this.initialized) {
      this.initialized = true;

      if (params.forceTrackChanges) {
        this.forceTrackChanges();
      }

      // notify the callback frame the editor initialized.
      this.postToCallbackFrame({
        action: 'initialized'
      });
    }
  };

  /**
   * Post the message to the callback iframe.
   *
   * @param message the message to post to the iframe.
   */
  EmbeddedConnector.prototype.postToCallbackFrame = function(message) {
    var siblingFrames = window.top.frames, i;
    for (i = 0; i < siblingFrames.length; i ++) {
      // do not post messages to self.
      var frameName = null;
      try {
        frameName = siblingFrames[i].name;
      } catch (err) {}

      if (frameName != 'WebAuthor-frame') {
        siblingFrames[i].postMessage(message, '*');
      }
    }
  };

  /**
   * Removes the ShowXML action from the toolbar and action manager.
   */
  EmbeddedConnector.prototype.removeShowXMLAction = function() {
    goog.events.listen(workspace, sync.api.Workspace.EventType.BEFORE_EDITOR_LOADED, function(event) {
      // remove the ToggleChangeTracking action from the toolbar.
      goog.events.listen(event.editor, sync.api.Editor.EventTypes.ACTIONS_LOADED, function(e) {
        if(e.actionsConfiguration && e.actionsConfiguration.toolbars && e.actionsConfiguration.toolbars[0].name == "Review") {
          var children = e.actionsConfiguration.toolbars[0].children;
          var i;
          for(i = 0; i < children.length; i++) {
            var child = children[i];
            if(child.name == 'More...') {
              var moreChildren = child.children;
              var moreChild;
              var j;
              for(j = 0; j < moreChildren.length; j++) {
                moreChild = moreChildren[j];
                if(moreChild.id == 'Author/ShowXML') {
                  // remove the ShoXML action.
                  children.splice(j, 1);
                  break;
                }
              }
              break;
            }
          }
        }
      });
    });
  };

  // When embedded, hide the App-bar.
  workspace.getViewManager().hideAppBar();
  
  // Initialize the connector.
  window.EmbeddedConnector = new EmbeddedConnector();

})();
