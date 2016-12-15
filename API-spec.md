CMS REST API
============ 

Assumptions
-----------

This plugin has several assumptions about the implementation of the API:

1. The API is deployed on the same domain as Web Author (maybe on different ports).
2. The API requests are authenticated using cookies.

Error responses
---------------

When the requests are made without the required cookies or with expired ones, the `401` (Not Authenticated) HTTP status code must be returned.

Basic file operations
---------------------

Each file is identified by an URL with a custom scheme. The file ID should be percent encoded in the following URLs.

| Action   | Endpoint  |
|----------|-----------|
| *Open*   | GET oxygen-cms/v1/files?url=file_id  |
| *Save*   | PUT oxygen-cms/v1/files?url=file_id  |
| *Upload* | POST oxygen-cms/v1/files?url=file_id  |
| *Delete* | DELETE oxygen-cms/v1/files?url=file_id  |

The file content encoding should be UTF-8 in both requests and responses of these endpoints.

User login
----------

When the plugin receives a `401` response from the API, meaning that the user is not authenticated, it will open the following URL for the user to login:

```
oxygen-cms/v1/login
```

You should implement this URL to show a login form to the user.
**Hint**: you can redirect her to your existing login form.

After the user logs in, your should redirect the user to 

```
oxygen-cms/v1/login-done
```

File browsing - folder based
----------------------------

If your file URLs have an hierarchical structure, you can use the default file browser by implementing the following REST endpoint:

| Action   | Endpoint  |
|----------|-----------|
| *List Folder*     | GET oxygen-cms/v1/folders?url=folder_url  |

The response should be a JSON object with the following format:

```javascript
[{name: "file1.dita"}, {name: "file2.dita"}]
```

Custom file browser
-------------------

If you do not have a hierarchical folder structure, you can register your own file browser.

TBD: 
```
oxygen-cms/v1/browse
```
After the user chose a file or an element, redirect her to 
```
oxygen-cms/v1/browse-done?url=...
```
