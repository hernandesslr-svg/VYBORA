import { S3Client, PutObjectCommand, GetObjectCommand, HeadObjectCommand } from '@aws-sdk/client-s3';
import { getSignedUrl } from '@aws-sdk/s3-request-presigner';
let client;
function getClient() {
  if (client) return client;
  const required = ['S3_BUCKET', 'S3_REGION', 'S3_ACCESS_KEY_ID', 'S3_SECRET_ACCESS_KEY'];
  const missing = required.filter(k => !process.env[k]);
  if (missing.length) throw new Error(`Armazenamento não configurado: ${missing.join(', ')}`);
  client = new S3Client({ region: process.env.S3_REGION, endpoint: process.env.S3_ENDPOINT || undefined, forcePathStyle: process.env.S3_FORCE_PATH_STYLE === 'true', credentials: { accessKeyId: process.env.S3_ACCESS_KEY_ID, secretAccessKey: process.env.S3_SECRET_ACCESS_KEY } });
  return client;
}
export async function createUploadUrl(key, contentType, sizeBytes) {
  const command = new PutObjectCommand({ Bucket: process.env.S3_BUCKET, Key: key, ContentType: contentType, ContentLength: sizeBytes, ServerSideEncryption: process.env.S3_SERVER_SIDE_ENCRYPTION || undefined });
  return getSignedUrl(getClient(), command, { expiresIn: 300 });
}
export async function inspectObject(key) {
  const result = await getClient().send(new HeadObjectCommand({ Bucket: process.env.S3_BUCKET, Key: key }));
  return { sizeBytes: Number(result.ContentLength || 0), contentType: result.ContentType || null, checksumSha256: result.ChecksumSHA256 || null, etag: result.ETag || null, versionId: result.VersionId || null };
}
export async function createDownloadUrl(key) {
  return getSignedUrl(getClient(), new GetObjectCommand({ Bucket: process.env.S3_BUCKET, Key: key }), { expiresIn: 120 });
}
