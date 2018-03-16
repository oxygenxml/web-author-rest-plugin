oXygen XML Web Author REST API 
==============================

The goal of this project is to make it simple to integrate Web Author with a CMS or file server.

It project contains the following parts:

1. A [tutorial](docs/cms-getting-started.md) to help you get started with your CMS integration.
1. A [REST API specification](docs/API-spec.md) that needs to be implemented by the CMS.
1. A [plugin](https://github.com/oxygenxml/web-author-rest-connector/releases) that connects Web Author to CMS-es that implement this REST API.

Plugin installation
-------------------

[Download the latest release ](https://github.com/oxygenxml/web-author-rest-connector/releases) of the plugin and 
[install it](https://www.oxygenxml.com/doc/versions/18.1.0/ug-webauthor/topics/webapp-configure-plugins.html) in the Web Author Admin Page.

Plugin configuration
--------------------

The plugn can be [cofigured](https://www.oxygenxml.com/doc/versions/18.1.0/ug-webauthor/topics/webapp-configure-plugins.html) in the Web Author Admin Page.

It has the following configuration options:

| Option   | Description  |
|----------|-----------|
| *REST Server URL*   | The base URL for the REST endpoints. E.g. `http://example.com/oxygen-cms/v1/` |
| *Root RegExp*   | Sometimes you want to use files URLs like: `rest://cms/project1/file.dita`, but in the File Browser you do not want to let the user navigate between projects. In this case *Root RegExp* would be `rest://cms/[^/]+/`. |
| *Use invisible login form*  | Whether the `$BASE_URL/rest-login` page should be in an invisible iframe rather than a dialog. |
