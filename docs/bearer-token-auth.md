Bearer Token Authentication (Deprecated)
========================================

The [cookie-based authentication](./cookie-based-auth.md) is recommended because it provides an authentication provider that can be used to secure all accesses to Web Author.

If the REST API requires authentication with bearer tokens you have to provide the value of this token to Web Author using one of the following approaches:
1. Using the `bearer.token.provider` [Loading Option](https://www.oxygenxml.com/doc/help.php?product=waCustom&pageId=web_author_api_concepts#web_author_api_concepts__loading-option). The value should be a function that returns the token as a string.
3. By setting the `bearer.token` option in the follwoing Java API method: `ro.sync.ecss.extensions.api.webapp.access.WebappEditingSessionLifecycleListener.editingSessionAboutToBeStarted()`
4. Using an HTTP POST request made from JS code inside an Web Author plugin: `$WEB_AUTHOR_URL/plugins-dispatcher/rest-bearer-token` with the form-param `token`.

If the token expires before the Web Author session expires, you can renew it before it expires by using the last approach presented above.

