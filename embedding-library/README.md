Embed Web Author in your web page (deprecated)
----------------------------------------------

This library allows you to embed the Web Author in a web page of your application. You just have to add the following script in the head of your web page: 

```javascript
<script src="oxygen-web-author-library.js"></script>
```

The library can be found at [oxygen-web-author-library.js](oxygen-web-author-library.js)

Then instantiate the `WebAuthor` editor passing it the container element and some loading options as below:

```javascript
var editor = new WebAuthor(container, {
  // Web Author server URL.
  url: 'http://localhost:8080/oxygen-xml-web-author/app/oxygen.html',
  // The URL of the document, stored on an embedded WebDAV found in the 'All Platforms' distribution.
  documentUrl: 'webdav-http://localhost:8080/oxygen-xml-web-author/plugins-dispatcher/webdav-server/dita/flowers/topics/flowers/gardenia.dita'
});
```
