REST API for CMS-es
===================

Assumptions
-----------

This plugin has several assumptions about the authentication of the API:

1. Either the API requests are authenticated using cookies
  - In this case the REST API should be deployed on the same domain as Web Author.
2. Or, the API requests are authenticated using bearer token

Basic file operations
---------------------

Each file is identified by an URL with the `rest://` scheme. The file URL should be percent encoded when used as a query parameter.

| Action   | Endpoint  | Request Body | Expected Response |
|----------|-----------|---------------------------|--------------------------------|
| *Open*   | GET    `$BASE_URL`/files?url=file_url  | - | octet-stream |
| *Save*   | PUT    `$BASE_URL`/files?url=file_url  | octet-stream | - |
| *Upload* | POST   `$BASE_URL`/files?url=file_url  | octet-stream | - |
| *Delete* | DELETE `$BASE_URL`/files?url=file_url  | - | - |

The file content encoding should be `UTF-8` in both requests and responses of these endpoints.
An example implementation for ASP.NET can be found [here](examples/asp.net.md).

Authentication
--------------

In the Administration Page you should select the "CMS" Authentication Provider.

Details about the currently authenticated user, should be provided at the following endpoint: `$BASE_URL/me` and should be returned as JSON in the following format:

```
{
  id: "string",
  name: "string",
  email: "string"
}
```

There are two authentication approaches:
1. [Based on Cookies](cookie-based-auth.md)
2. [Based on bearer tokens](bearer-token-auth.md)

Headers
-------

Each request made by Web Author will include the following headers:

| Header Name |  Value      | Comment                         |
|-------------|-------------|---------------------------------|
| *User-Agent*| Oxygen/VV.V | VV.V is the Web Author version  |
| *X-Requested-With* | RC   | Can be used for CSRF protection |
| *Cookie* | name1=value1;name2=value2 | The cookies used by the CMS, if bearer token is not provided|
| *Authorization* | Bearer token | In case it is provided |

Error responses
---------------

When the requests are made without the required cookies or with expired ones, the `401` (Not Authenticated) HTTP status code must be returned.

If an error occured while processing the request, the API can return an error status code: `4XX` or `5XX`. The body of the response whould be a JSON message with the following format:

```Javascript
{
  "message": "Cannot process request due to ... "
}
```


File browsing
-------------

Some of the editing actions require the user to browse for a file in the CMS (e.g. when inserting an image) or for an element inside an XML document (inserting a cross reference). To present a browsing widget to the user in these cases there are two options.   

### Folder-based browsing widget

If your file URLs have an hierarchical structure, you can use the default file browsing widget by implementing the following REST endpoint:

| Action   | Endpoint  |
|----------|-----------|
| *List Folder*     | GET `$BASE_URL`/oxygen-cms/v1/folders?url=folder_url  |

The response should be a JSON array of objects with the following format:

```javascript
[{"name": "file.ditamap","folder":false},{"name":"topics","folder":true}]
```

### Custom file browsing widget

In some cases, the files do not have a hierarchical folder structure, and the user can rely on labels or full text search to find content. In this case, you can register a custom file browsing widget by using the `workspace.setUrlChooser()` JavaScript API.
