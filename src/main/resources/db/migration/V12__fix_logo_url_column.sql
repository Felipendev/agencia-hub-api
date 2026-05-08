-- Fix logo_url column to support base64 data URLs (which can be very large)
ALTER TABLE agencies ALTER COLUMN logo_url TYPE TEXT;
