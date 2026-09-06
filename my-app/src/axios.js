import axios from "axios";
import { getToken, logout } from "./auth";

/**
 * Dedicated instance so interceptors apply only to our own API calls, not to every axios import
 * in the app (or in a dependency).
 *
 * In development `baseURL` is empty and CRA's `proxy` setting forwards /api to the backend; set
 * REACT_APP_API_BASE_URL for deployments where the API lives on another origin.
 */
const api = axios.create({
  baseURL: process.env.REACT_APP_API_BASE_URL || "",
});

api.interceptors.request.use((config) => {
  const token = getToken();
  if (token && !config.url?.startsWith("/api/auth/")) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    // An expired or revoked token should drop us back to signed-out rather than leaving the UI
    // in a state where every action silently fails.
    if (error.response?.status === 401 && !error.config?.url?.startsWith("/api/auth/")) {
      logout();
    }
    return Promise.reject(error);
  }
);

/** Pulls a human-readable message out of an axios error, whatever shape the backend returned. */
export function errorMessage(error, fallback = "Something went wrong.") {
  const data = error?.response?.data;
  if (typeof data === "string" && data.trim()) return data;
  if (data?.message) return data.message;
  if (data?.error) return data.error;
  return fallback;
}

export default api;
