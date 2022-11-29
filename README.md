oXygen XML Web Author REST API 
==============================

The goal of this project is to make it simple to integrate Web Author with a CMS or file server.

It project contains the following parts:

1. A [tutorial](docs/cms-getting-started.md) to help you get started with your CMS integration.
1. A [REST API specification](docs/API-spec.md) that needs to be implemented by the CMS.
1. A [plugin](https://www.oxygenxml.com/maven/com/oxygenxml/web-author-rest-plugin/) that connects Web Author to CMS-es that implement this REST API.

Plugin installation
-------------------

[Download the latest release ](https://www.oxygenxml.com/maven/com/oxygenxml/web-author-rest-plugin/) of the plugin and 
[install it](https://www.oxygenxml.com/doc/versions/18.1.0/ug-webauthor/topics/webapp-configure-plugins.html) in the Web Author Admin Page.

Plugin configuration
--------------------

The plugin can be [configured](https://www.oxygenxml.com/doc/versions/18.1.0/ug-webauthor/topics/webapp-configure-plugins.html) in the Web Author Admin Page.

It has the following configuration options:

| Option   | Description  |
|----------|-----------|
| *REST Server URL*   | The base URL for the REST endpoints. E.g. `http://example.com/oxygen-cms/v1/` |
| *Root RegExp*   | Sometimes you want to use file URLs like: `rest://cms/project1/file.dita`, but in the File Browser you do not want to let the user navigate between projects. In this case *Root RegExp* would be `rest://cms/[^/]+/`. |
| *Use invisible login form*  | Whether the `$BASE_URL/rest-login` page should be in an invisible iframe rather than a dialog. |

Copyright and License
---------------------
Copyright 2018 Syncro Soft SRL.

This project is licensed under [Apache License 2.0](https://github.com/oxygenxml/web-author-rest-plugin/blob/master/LICENSE)
