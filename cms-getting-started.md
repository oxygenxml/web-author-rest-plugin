Getting started with integrating Web Author 
===========================================

This tutorial will explain how to develop a simple integration with oXygen XML Web Author.

Download Web Author and run it
------------------------------

Download the "All Platforms" version of Web Author from the [oXygen website](https://oxygenxml.com/xml_web_author/download_oxygenxml_web_author.html?os=All) and follow the instructions on that page to start it.

You will be asked for a license. You can request a trial license in the application for the time being which will work for 30 days. Meanwhile, you can contact support@oxygenxml.com to request a development license for an extended period of time.

You can now open the same files presented on the Dashboard. These files are stored on an embedded WebDAV server.

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

Connect it to the CMS
---------------------

Now, you were only able to load sample files stored on the embedded WebDAV server. However, the goal is to load files stored on your CMS (or file server).

oXygen provides a plugin that let's you connect Web Author to your CMS just by implementing a simple REST API. 
[Download](https://github.com/oxygenxml/web-author-rest-connector/releases) the plugin and 
[install it](https://www.oxygenxml.com/doc/versions/18.1.0/ug-webauthor/topics/webapp-configure-plugins.html).

The plugin is configured by default to connect to a dummy implementation of the REST API that works with files on the server's file system. You can try to open the URL below:

```
rest-http://localhost:8080/oxygen-xml-web-author/plugins-dispatcher/rest/dita/flowers/topics/flowers/gardenia.dita
```

Now, comes the REST API implementation step. The specification of the REST API can be found [here](API-spec.md). 


Next steps
----------
For more advanced use-cases here are some possible ways to further customize Web Author:
- Implement an additional plugin. Our manual contains [some instructions](https://oxygenxml.com/doc/versions/18.1/ug-editor/tasks/webapp-plugin-prototyping.html#webapp-plugin-prototyping) to get you started.
- Create a new XML framework or extend an existing one. You can use the ["Web Author Test Server Add-on"](https://oxygenxml.com/doc/versions/18.1/ug-editor/topics/customizing_frameworks.html) during development.
- Create a self-contained application with all these components that you can deploy to others.

