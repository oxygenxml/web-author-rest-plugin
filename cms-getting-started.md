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

You can use the Web Author [embedding library](embedding-library/) for this purpose. 

Connect it to the CMS
---------------------

The previous steps enabled you to load in Web Author sample files stored on an embedded WebDAV server. Let's now open files stored on your CMS (or file server).

1. Install the oXygen XML Web Author REST API connector plugin. 

The plugin is configured by default to connect to a dummy implementation of the REST API that works with files on the server's file system. You can test your installation by opening the URL below:

```
rest-http://rest/dita/flowers/topics/flowers/gardenia.dita
```

2. To connect the plugin to your CMS you should implement the REST API [specified by this plugin](API-spec.md) and [configure](README.md#configuration) the plugin with the base URL of your REST API.


Next steps
----------
For more advanced use-cases here are some possible ways to further customize Web Author:
- Implement an additional plugin. Our manual contains [some instructions](https://oxygenxml.com/doc/versions/18.1/ug-editor/tasks/webapp-plugin-prototyping.html#webapp-plugin-prototyping) to get you started.
- Create a new XML framework or extend an existing one. You can use the ["Web Author Test Server Add-on"](https://oxygenxml.com/doc/versions/18.1/ug-editor/topics/customizing_frameworks.html) during development.
- Create a self-contained web application (.war file) with all these components that you can deploy to others.

