/**
 * Base URL for backend media/image endpoints.
 *
 * In development: Vite proxy rewrites '/api/...' → 'http://localhost:8080/api/...'
 *   so we need an empty string here (images use the full path like /api/bikes/...)
 *   but static image paths like `/uploads/...` are served directly by backend.
 *
 * In production on Render: set VITE_API_BASE_URL to the backend Render URL,
 *   e.g. https://auto-consultancy-backend.onrender.com
 *   This env var is set at build time (not runtime).
 *
 * Usage: imgUrl(bike.primaryImageUrl)  →  full absolute URL for <img src>
 */
export const BACKEND_BASE = import.meta.env.VITE_API_BASE_URL || '';

/** Build a full image URL from a relative backend path. Returns null if path is falsy. */
export const imgUrl = (path) => (path ? `${BACKEND_BASE}${path}` : null);
