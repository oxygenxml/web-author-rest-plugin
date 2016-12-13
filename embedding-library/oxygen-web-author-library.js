/** @Constructor for the WebAuthor editor. */
/**
 * Embedded WebAuthor editor.
 *
 * @param container the DOM node in which to load the WebAuthor.
 * @param options the WebAuthor loading options.
 * @constructor
 */
WebAuthor = function(container, options) {
  container.classList.add('WebAuthor-container');
  this.iframe = document.createElement('iframe');
  this.iframe.classList.add('WebAuthor-frame');
  this.iframe.style.display = 'block';
  this.iframe.style.width = '100%';
  this.iframe.style.height = '100%';
  this.iframe.style.border = '0px';
  this.iframe.name = "WebAuthor-frame";

  this.url = options.url;
  var fullUrl = this.getFullUrl_(options);

  this.iframe.src = fullUrl;
  container.appendChild(this.iframe);

  this.callbackIframe = this.createCallbackIframe_(container);


  var initParams = {};
  if(options.forceTrackChanges) {
    initParams["forceTrackChanges"] = options.forceTrackChanges;
  }
  // try to initialize the WebAuthor before it is already loaded.
  this.intervalId = setInterval(function() {
    this.sendMessage_({
      action: 'initialize',
      params: initParams
    });
  }.bind(this), 200);
};

/**
 * Creates and registers all the listeners for the callback iframe.
 *
 * @return {*} the callback iframe.
 *
 * @private
 */
WebAuthor.prototype.createCallbackIframe_ = function(container) {
  var iframe = document.createElement('iframe');
  iframe.style.display = 'none';
  iframe.onload = function() {
    iframe.contentWindow.addEventListener('message', this.messageReceived_.bind(this));
  }.bind(this);

  container.ownerDocument.body.appendChild(iframe);
  return iframe;
};

/**
 * Posts the message to the WebAuthor iframe window.
 *
 * @param message the message to post.
 *
 * @private
 */
WebAuthor.prototype.sendMessage_ = function(message) {
  this.iframe.contentWindow.postMessage(message, this.url);
};

/**
 * Handles the messages received from the WebAuthor iframe.
 *
 * @param e the mssage event.
 *
 * @private
 */
WebAuthor.prototype.messageReceived_ = function(e) {
  if(e.data.action = 'initialized') {
    clearInterval(this.intervalId);
  } else {
    console.log('callback message: ' + e.data);
  }
};

/**
 * Computes the WebAuthor full URL based on the options passed.
 *
 * @param options the options for editor opening.
 *
 * @return {*|string} the url to open taking into consideration the options.
 *
 * @private
 */
WebAuthor.prototype.getFullUrl_ = function(options) {
  var fullUrl = options.url;
  var query = '?embedded=true';
  var documentUrl = options.documentUrl;
  if(documentUrl) {
    query += '&url=' + encodeURIComponent(documentUrl);
  }
  return fullUrl + query;
};

/**
 * Sets the embedded editor as read-only.
 *
 * @param readOnly the new aditor read-only status.
 * @param opt_reason optional lock owner name to be displayed to the user.
 */
WebAuthor.prototype.setReadOnly = function(readOnly, opt_reason) {
  this.sendMessage_({
    action: 'setReadOnly',
    params: {
      readOnly: readOnly,
      reason: opt_reason
    }
  });
};
