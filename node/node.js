const express = require('express');
const AWS = require('aws-sdk');
const app = express();
const PORT =  3000;
require('dotenv').config();
const cors = require('cors');
app.use(cors());
// Configure AWS S3
const s3 = new AWS.S3({
  accessKeyId: process.env.AWS_ACCESS_KEY_ID,
  secretAccessKey: process.env.AWS_SECRET_ACCESS_KEY,
  region: process.env.AWS_REGION
});

// Route to list files from S3 with pagination
app.get('/list-files', async (req, res) => {
  var { bucket, continuationToken, maxKeys } = req.query;

  if (!bucket) {
    return res.status(400).json({ error: 'Bucket is required' });
  }

  let maxKeysValue = 10; // Default
  maxKeys*=5;
  if (maxKeys) {
    const parsedMaxKeys = parseInt(maxKeys, 10);
    if (isNaN(parsedMaxKeys) || parsedMaxKeys < 1 || parsedMaxKeys > 1000) {
      return res.status(400).json({ error: 'maxKeys must be a number between 1 and 1000' });
    }
    maxKeysValue = parsedMaxKeys;
  }

  try {
    const params = {
      Bucket: bucket,
      ContinuationToken: continuationToken,
      MaxKeys: maxKeysValue // Use the validated maxKeys
    };

    // List files from S3
    const data = await s3.listObjectsV2(params).promise();

    const response = {
      files: data.Contents.map(file => ({
        key: file.Key,
        lastModified: file.LastModified,
        size: file.Size,
        storageClass: file.StorageClass
      })),
      isTruncated: data.IsTruncated,
      nextContinuationToken: data.NextContinuationToken
    };

    res.json(response);
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Error listing files from S3', details: error.message });
  }
});

// Route to fetch a single file from S3
app.get('/fetch-file', async (req, res) => {
  const { bucket, key } = req.query;

  if (!bucket || !key) {
    return res.status(400).json({ error: 'Bucket and key are required' });
  }

  try {
    const params = {
      Bucket: bucket,
      Key: key
    };

    const data = await s3.getObject(params).promise();

    res.setHeader('Content-Type', data.ContentType);
    res.setHeader('Content-Disposition', `attachment; filename="${key}"`);
    res.send(data.Body);
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Error fetching file from S3' });
  }
});

// Start the server
app.listen(PORT, () => {
  console.log(`Server running on http://localhost:${PORT}`);
});
