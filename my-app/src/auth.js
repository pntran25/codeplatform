import { useSyncExternalStore } from "react";

const TOKEN_KEY = "token";

const listeners = new Set();

function notify() {
  listeners.forEach((listener) => listener());
}

/** Decodes a JWT payload without verifying it - the server is the authority on validity. */
function decode(token) {
  try {
    const payload = token.split(".")[1];
    const json = atob(payload.replace(/-/g, "+").replace(/_/g, "/"));
    return JSON.parse(decodeURIComponent(escape(json)));
  } catch {
    return null;
  }
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

/**
 * Returns `{ username, role }` for the stored token, or `null` when there is no token or it has
 * expired. Reading the expiry here means a stale token logs the user out instead of producing a
 * confusing wall of 401s.
 */
export function getCurrentUser() {
  const token = getToken();
  if (!token) return null;

  const claims = decode(token);
  if (!claims) return null;
  if (claims.exp && claims.exp * 1000 <= Date.now()) {
    localStorage.removeItem(TOKEN_KEY);
    return null;
  }

  return { username: claims.sub, role: claims.role || "USER" };
}

export function login(token) {
  localStorage.setItem(TOKEN_KEY, token);
  notify();
}

export function logout() {
  localStorage.removeItem(TOKEN_KEY);
  notify();
}

function subscribe(listener) {
  listeners.add(listener);
  // Keep other tabs of the same app in sync.
  window.addEventListener("storage", listener);
  return () => {
    listeners.delete(listener);
    window.removeEventListener("storage", listener);
  };
}

// useSyncExternalStore requires a stable snapshot, so cache until the token changes.
let cachedToken;
let cachedUser = null;

function getSnapshot() {
  const token = getToken();
  if (token !== cachedToken) {
    cachedToken = token;
    cachedUser = getCurrentUser();
  }
  return cachedUser;
}

/** Subscribes a component to the signed-in user; re-renders on login, logout and cross-tab changes. */
export function useCurrentUser() {
  return useSyncExternalStore(subscribe, getSnapshot, () => null);
}
