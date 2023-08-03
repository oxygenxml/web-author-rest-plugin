Cookie-based Authentication
===========================

If the REST API requires cookie-based authentication, it should be deployed on the same domain as Web Author.

Below we will denote:
    - the base URL of the REST API as `$BASE_URL`. An example value would be: `http://example.com/oxygen-cms/v1/`
    - the base URL of Web Author as `$WEB_AUTHOR_URL`. An example value would be `http://example.com/oxygen-xml-web-author/app/oxygen.html`.

The resulting architecture would look like in the image below:

![Authentication architecture](cookie-auth-architecture.png)

Requests Journey
----------------

1. Web Author UI loads in the browser
2. Web Author UI makes a request to the Web Author Server to retrieve the document. The HTTP request naturally contains the session cookie (e.g. sid=1f4e)
3. Web Author Server receives the request from the Web Author UI and, thanks to web-author-rest-plugin, it starts a request to CMS REST API Server with the cookies received from the Web Author UI (the cookies are forwarded by web-author-rest-plugin)
4. CMS REST API Server receives the request from the Web Author Server and:

    * usually checks if the user represented by the received session Cookie is authenticated and authorized. If the user is not authenticated (or it was in the past but now its session expired) it must return 401 HTTP status code.
    * if everything is OK, it returns the document content
5. Then Web Author Server:

    * if receives 401 HTTP code will make Web Author UI show the CMS authentication page ($BASE_URL/rest-login)
    * if receives success HTTP code will successfully load the document

Re-authentication
-----------------

One solution is to embed Web Author in a page of your application and make sure that the user is authenticated before opening the editor.

However, if you choose to allow users to open Web Author independently of your application, or if you use expiring login sessions, the user may need to re-login during an editing session.

To implement this re-login flow you should do the following:

1. When Web Author connects to the API using no cookies or expired cookies, return `401` status code. 
2. Implement the following HTTP endpoint to show a login form to the user.

  ```
  $BASE_URL/rest-login
  ```
If need only to run JS code in the loaded iframe, without displaying it to the user (your login mechanism does not require user interaction), you can toggle the _Use invisible login form_ plugin option. This will load the page in an invisible iframe.
 
 **Hint**: you can redirect him to your existing login form.
  
3. After the user logs in, you should redirect to 
  ```
  $WEB_AUTHOR_URL/plugins-dispatcher/rest-login-callback
  ```
(this notifies the WebAuthor that the login process completed and it should retry the action that failed)