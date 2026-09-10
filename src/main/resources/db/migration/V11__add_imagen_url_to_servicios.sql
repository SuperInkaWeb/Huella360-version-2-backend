-- Migration to add imagen_url column to servicios table
ALTER TABLE servicios ADD COLUMN imagen_url TEXT;
