Bearer Token Authentication
===========================

If the REST API requires authentication with bearer tokens you have to provide the value of this token to Web Author using one of the following approaches:
1. Using the `bearer.token` [Loading Option](https://www.oxygenxml.com/doc/help.php?product=waCustom&pageId=web_author_api_concepts#web_author_api_concepts__loading-option) 
2. Using the `bearer.token` URL parameter (not recommended)
3. By setting the `bearer.token` option in the follwoing Java API method: `ro.sync.ecss.extensions.api.webapp.access.WebappEditingSessionLifecycleListener.editingSessionAboutToBeStarted()`
4. Using an HTTP request made from JS code inside an Web Author plugin: `$WEB_AUTHOR_URL/plugins-dispatcher/rest-bearer-token?token=...`

If the token expires before the Web Author session expires, you can renew it before it expires by using the last approach presented above.
