Node.js Example Implementation of the API specification
===================

Following a very basic example implementation of the API specification using node.js.

```js
const express = require('express');
const app = express();
const PORT = 3000;
const BASE_URL = '/api';

// In-memory 'database' for files and folders
const db = {
  files: {
    "document1.xml": "<root>test</root>"
  },
  folders: {
    "": [
      {"name": "document1.xml", "folder": false},
      {"name": "images", "folder": true},
      {"name": "topics", "folder": true}
    ],
    "images/": [
      {"name": "flower.jpeg", "folder": false}
    ]
  },
};

app.use(
  express.raw({
    inflate: true,
    limit: '50mb',
    type: () => true
  })
);

// Logging middleware
app.use((req, res, next) => {
  const currentTime = new Date().toISOString();
  console.log(`[${currentTime}] ${req.method} request to ${req.originalUrl}`);
  next();
});

// Helper function to decode and strip prefix from the URL
function decodeAndStripUrlPrefix(url) {
  return decodeURIComponent(url || "").replace('rest://cms/', '');
}

// Route for opening (retrieving) a file
app.get(`${BASE_URL}/files`, (req, res) => {
  const decodedFileUrl = decodeAndStripUrlPrefix(req.query.url);
  console.log('Decoded URL:', decodedFileUrl);
  
  if (db.files[decodedFileUrl]) {
    let content = db.files[decodedFileUrl];
    console.log('file content', content);
    res.type('application/octet-stream');
    res.charset = 'utf-8';
    res.send(content);
  } else {
    res.status(404).json({ message: 'File not found' });
  }
});

// Route for saving (uploading) a file
app.put(`${BASE_URL}/files`, (req, res) => {
  const decodedFileUrl = decodeAndStripUrlPrefix(req.query.url);
  
  if (Buffer.isBuffer(req.body)) {
    // Convert buffer to string
    const content = req.body.toString('utf-8');
    console.log('Content being stored:', content);
    
    // Save or update the file content in the in-memory 'database'
    db.files[decodedFileUrl] = content;
    res.status(200).send({ message: "File saved successfully." });
  } else {
    // If it's not a buffer, log and return an error
    console.log('Unexpected req.body type:', req.body);
    res.status(400).send({ message: "Invalid file data." });
  }
});


// Route for folder-based browsing
app.get(`${BASE_URL}/folders`, (req, res) => {
  const decodedFolderUrl = decodeAndStripUrlPrefix(req.query.url);
  console.log('Decoded URL:', decodedFolderUrl);
  
  // Return the list of files/folders in the requested folder
  res.json(db.folders[decodedFolderUrl] || []);
});

// Error handler middleware
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).json({ message: 'Internal server error' });
});

app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`);
});
```

In the plugin's config page, use 

- REST Server URL: `http://localhost:3000/api`