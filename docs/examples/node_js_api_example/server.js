const express = require('express');
const bodyParser = require('body-parser');

const app = express();
const PORT = 3000;
const BASE_URL = '/api';

app.use(bodyParser.raw({ type: '*/*' })); // To handle octet-stream

// Logging middleware
app.use((req, res, next) => {
    const currentTime = new Date().toISOString();
    console.log(`[${currentTime}] ${req.method} request to ${req.originalUrl}`);
    next();
});

// In-memory database
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

// File operations
app.get(`${BASE_URL}/files`, (req, res) => {
    const decodedFileUrl = decodeURIComponent(req.query.url || "").replace('rest://cms/', '');
    console.log('Decoded URL:', decodedFileUrl);
    if (db.files[decodedFileUrl]) {
        return res.send(db.files[decodedFileUrl]);
    }
    res.status(404).json({ message: 'File not found' });
});

app.put(`${BASE_URL}/files`, (req, res) => {
    const decodedFileUrl = decodeURIComponent(req.query.url || "").replace('rest://cms/', '');
    console.log('Decoded URL:', decodedFileUrl);
    db.files[decodedFileUrl] = req.body.toString('utf-8');
    res.end();
});

app.post(`${BASE_URL}/files`, (req, res) => {
    const decodedFileUrl = decodeURIComponent(req.query.url || "").replace('rest://cms/', '');
    db.files[decodedFileUrl] = req.body.toString('utf-8');
    res.end();
});

app.delete(`${BASE_URL}/files`, (req, res) => {
    const decodedFileUrl = decodeURIComponent(req.query.url || "").replace('rest://cms/', '');
    delete db.files[decodedFileUrl];
    res.end();
});

// Folder-based browsing widget
app.get(`${BASE_URL}/folders`, (req, res) => {
    const decodedFolderUrl = decodeURIComponent(req.query.url || "").replace('rest://cms/', '');
    console.log('Decoded URL:', decodedFolderUrl);
    console.log('Data:', db.folders[decodedFolderUrl]);
    res.json(db.folders[decodedFolderUrl] || []);
});

// Error handler middleware
app.use((err, req, res, next) => {
    console.error(err.stack);
    res.status(500).json({ message: 'Cannot process request due to server error' });
});

app.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});