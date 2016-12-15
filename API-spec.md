CMS REST API
============ 

Assumptions
-----------

This plugin has several assumptions about the implementation of the API:

1. The API is deployed on the same domain as Web Author (maybe on different ports). Let's call its base URL `$BASE_URL`.
2. The API requests are authenticated using cookies.

These are not hard requirements but meeting them greatly simplifies the integration.

Error responses
---------------

When the requests are made without the required cookies or with expired ones, the `401` (Not Authenticated) HTTP status code must be returned.

Basic file operations
---------------------

Each file is identified by an URL with a custom scheme. The file ID should be percent encoded in the following URLs.

| Action   | Endpoint  |
|----------|-----------|
| *Open*   | GET    `$BASE_URL`/oxygen-cms/v1/files?url=file_id  |
| *Save*   | PUT    `$BASE_URL`oxygen-cms/v1/files?url=file_id  |
| *Upload* | POST   `$BASE_URL`oxygen-cms/v1/files?url=file_id  |
| *Delete* | DELETE `$BASE_URL`oxygen-cms/v1/files?url=file_id  |

The file content encoding should be UTF-8 in both requests and responses of these endpoints.

User Authentication
-------------------

One solution is to embed Web Author in a page of your application and make sure that the user is authenticated before opening the editor.

However, if you choose to allow users to open Web Author independently of your application, or if you use expiring login sessions, the user may need to re-login during an editing session.

To implement this re-login flow, when the plugin receives a `401` status code from the API, meaning that the user is not authenticated, it will open the following URL for the user to login:

```
$BASE_URL/oxygen-cms/v1/login
```

You should implement this URL to show a login form to the user.
**Hint**: you can redirect her to your existing login form.

After the user logs in, your should redirect the user to 

```
$WEB_AUTHOR_URL/plugins-dispatcher/rest/login-done
```

File browsing
-------------

Some of the editing actions require the user to browse for a file in the CMS (inserting an image) or for an element inside an XML document (inserting a cross reference). To present a browsing widget to the user in these cases there are two options.   

### Folder-based browsing widget

If your file URLs have an hierarchical structure, you can use the default file browser by implementing the following REST endpoint:

| Action   | Endpoint  |
|----------|-----------|
| *List Folder*     | GET `$BASE_URL`/oxygen-cms/v1/folders?url=folder_url  |

The response should be a JSON object with the following format:

```javascript
[{name: "file1.dita"}, {name: "file2.dita"}]
```

### Custom file browsing widget

If your files do not have a hierarchical folder structure, you can register your own file browser.

When the user needs to choose an URL of a CMS resource, the folloing URL will be open for her:
```
$BASE_URL/oxygen-cms/v1/browse
```

After the user chose the resource URL, your job is to redirect her to 
```
$WEB_AUTHOR_URL/plugins-dispatcher/rest/browse-done?url=...
```
