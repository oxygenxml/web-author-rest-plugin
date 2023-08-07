(function() {
  // Do not accept a bearer token received as an URL parameter. 
  // This is a security risk, as the token will be visible in the browser history.
  // Also, it can be used to for the victim to impersonate the attacker.
  var windowUrl = new URL(window.location.href)
  if (windowUrl.searchParams.has('bearer.token')) {
    windowUrl.searchParams.delete('bearer.token');
    // Reload from the new URL
    window.location.href = windowUrl.href;
  }

  setTimeout(function() {
    // Try to be the last plugin that registers the listener. This way other plugins will be able to register
    // their bearer token provider before us.
    goog.events.listen(workspace, sync.api.Workspace.EventType.BEFORE_EDITOR_LOADED, function (e) {
      // If the URL has 'rest' protocol we use the rest protocol handler.
      if (e.options.url.match('rest:\/\/')) {
        // The token provider is a JS function that cannot be passed as an URL parameter.
        let tokenProvider = e.options['bearer.token.provider'];
        if (tokenProvider) {
          e.options['bearer.token'] = tokenProvider();
          delete e.options['bearer.token.provider'];
        }
      }
    });
  });
})();