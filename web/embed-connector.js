(function() {
  // run this code only when embeded.
  if(true != workspace.embedded) {
    return;
  }

  Actions = {
    SET_READ_ONLY_STATUS: 'setReadOnly',
    INITIALIZE: 'initialize'
  };

  /**
   * The Embedded connector.
   *
   * @constructor
   */
  EmbeddedConnector = function() {
    // listen for post messages
    window.addEventListener('message', this.messageReceived.bind(this), false);
  }

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
    var siblingFrames = window.top.frames
    var i;
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
   * Removes the toggleChangeTracking toolbar action and
   * enables change tracking.
   *
   */
  EmbeddedConnector.prototype.forceTrackChanges = function() {
    goog.events.listen(workspace, sync.api.Workspace.EventType.BEFORE_EDITOR_LOADED, function(event) {
      // remove the ToggleChangeTracking action from the toolbar.
      goog.events.listen(event.editor, sync.api.Editor.EventTypes.ACTIONS_LOADED, function(e) {
        var i;
        var action;
        if (e.actionsConfiguration && e.actionsConfiguration.toolbars && e.actionsConfiguration.toolbars[0].name == "Review") {
          var actions = e.actionsConfiguration.toolbars[0].children;
          for (i = 0; i < actions.length; i ++) {
            action = actions[i];
            if (action.type == 'action' && action.id == 'Author/TrackChanges') {
              actions.splice(i, 1);
            }
          }
          // enable track changes
          var am = event.editor.getActionsManager();
          var toggleAction = am.getActionById('Author/TrackChanges');
          toggleAction.isSelected(function(isSelected) {
            if ( !isSelected) {
              toggleAction.actionPerformed();
            }
          }.bind(this));

          // remove the action from actions manager.
          am.unregisterAction('Author/TrackChanges');
        }
      });
    });
  };

  // Initialize the connector.
  var connector = new EmbeddedConnector();


})();