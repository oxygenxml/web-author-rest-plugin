oXygen XML Web Author connector for a generic REST API
======================================================

The goal of this plugin is to make it simple to integrate Web Author with a CMS or file server.

To this end, this plugins defines a generic [REST API](API-spec.md) to be implement by the CMS or file server. 

This plugin also contains a [library](embedding-library) that can be used to embed Web Author in your application's web page.


Installation
-----------

[Download](https://github.com/oxygenxml/web-author-rest-connector/releases) the plugin and 
[install it](https://www.oxygenxml.com/doc/versions/18.1.0/ug-webauthor/topics/webapp-configure-plugins.html) in the Web Author Admin Page.

Configuration
-------------

The plugn can be [cofigured](https://www.oxygenxml.com/doc/versions/18.1.0/ug-webauthor/topics/webapp-configure-plugins.html) in the Web Author Admin Page.

It has the following configuration options:

| Option   | Description  |
|----------|-----------|
| *REST Server URL*   | The base URL for the REST endpoints. E.g. `http://example.com/oxygen-cms/v1/` |


