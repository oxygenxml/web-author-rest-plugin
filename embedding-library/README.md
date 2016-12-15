Embed Web Author in your web page
---------------------------------

You may want to embed the Web Author in a webpage of your application. You just have to add the following script in the head of your page: 

```javascript
<script src="oxygen-web-author-library.js"></script>
```

The library can be found at [embedding-library/oxygen-web-author-library.js](embedding-library/oxygen-web-author-library.js)

Then instantiate the `WebAuthor` editor passing it the container element and some loading options as below:

```javascript
var editor = new WebAuthor(container, {                                                                                                                        
  url: 'http://localhost:8080/oxygen-xml-web-author/app/oxygen.html',                                                                                          ```
  documentUrl: 'webdav-https://www.oxygenxml.com/webapp-demo-aws/plugins-dispatcher/webdav-server/dita/flowers/topics/flowers/gardenia.dita'                   
});                                                                                                                                                            
```
