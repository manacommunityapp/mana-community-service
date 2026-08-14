-- V49: Link event_gallery_item to media_objects via media_external_id
-- This allows EventGalleryService to generate fresh presigned/CloudFront URLs
-- from the MediaObject's S3 key at read time, instead of storing expiring presigned URLs.

ALTER TABLE event_gallery_item
    ADD COLUMN IF NOT EXISTS media_external_id UUID;
