Getting started with integrating Web Author 
===========================================

This tutorial will explain how to develop a simple integration with oXygen XML Web Author.

Download Web Author and run it
------------------------------

Download the "All Platforms" version of Web Author from the [oXygen website](https://oxygenxml.com/xml_web_author/download_oxygenxml_web_author.html?os=All) and follow the instructions on that page to start it.

You will be asked for a license. You can request a trial license in the application for the time being which will work for 30 days. Meanwhile, you can contact support@oxygenxml.com to request a development license for an extended period of time.

You can now open the sample files presented on the Dashboard. These files are stored on an embedded WebDAV server.

Connect it to the CMS
---------------------

The previous steps enabled you to load in Web Author sample files stored on an embedded WebDAV server. Let's now open files stored on your CMS (or file server).

1. [Install](../README.md#plugin-installation) the oXygen XML Web Author REST API connector plugin. 

2. To connect the plugin to your CMS you should implement the REST API [specified by this plugin](API-spec.md) and [configure](../README.md#plugin-configuration) the plugin with the base URL of your REST API.


Next steps
----------
For a more detailed documentation, please see our [Integration and Customization Guide](https://www.oxygenxml.com/doc/versions/19.0.0/ug-waCustom/index.html).
