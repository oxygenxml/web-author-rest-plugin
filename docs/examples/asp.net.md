ASP.NET Example Implementation of the API specification
===================

Following a very basic example implementation of the API specification in C#.

```C#
[Route("api/files")]
[HttpGet]
public HttpResponseMessage Open(string url)
{  
    long id = GetIdFromUrl(url); // get your file-id from the url
    
    FileModel file = FilesProvider.GetFile(id); // load the file by the id

    // write the response body, set the file name and content-type
    var result = new HttpResponseMessage(HttpStatusCode.OK)
    {
        Content = new ByteArrayContent(((MemoryStream)file.Content).ToArray())
    };        
    result.Content.Headers.ContentDisposition =
        new ContentDispositionHeaderValue("attachment")
        {
            FileName = file.Name;
        };
    result.Content.Headers.ContentType = new MediaTypeHeaderValue("application/octet-stream");    

    return result;
}

[Route("api/files")]
[HttpPut]
public HttpResponseMessage Save(string url)
{
    long id = GetIdFromUrl(url);      
    XDocument document = XDocument.Load(HttpContext.Current.Request.InputStream);

    //...validate and save the document...

    return new HttpResponseMessage(HttpStatusCode.OK);
}
```
