import { getCurrentUser, getToken, login, logout } from "./auth";

/** Builds an unsigned JWT with the given payload - only the payload is read client-side. */
function fakeToken(payload) {
  const encode = (obj) => btoa(JSON.stringify(obj)).replace(/=+$/, "");
  return `${encode({ alg: "HS256" })}.${encode(payload)}.signature`;
}

const inAnHour = Math.floor(Date.now() / 1000) + 3600;
const anHourAgo = Math.floor(Date.now() / 1000) - 3600;

beforeEach(() => localStorage.clear());

test("returns no user when nothing is stored", () => {
  expect(getCurrentUser()).toBeNull();
});

test("reads the username and role out of a valid token", () => {
  login(fakeToken({ sub: "alice", role: "ADMIN", exp: inAnHour }));
  expect(getCurrentUser()).toEqual({ username: "alice", role: "ADMIN" });
});

test("defaults to the USER role when the token omits it", () => {
  login(fakeToken({ sub: "bob", exp: inAnHour }));
  expect(getCurrentUser()).toEqual({ username: "bob", role: "USER" });
});

test("discards an expired token instead of reporting a signed-in user", () => {
  login(fakeToken({ sub: "carol", role: "USER", exp: anHourAgo }));
  expect(getCurrentUser()).toBeNull();
  expect(getToken()).toBeNull();
});

test("survives a malformed token", () => {
  login("not-a-jwt");
  expect(getCurrentUser()).toBeNull();
});

test("logout clears the token", () => {
  login(fakeToken({ sub: "dave", exp: inAnHour }));
  logout();
  expect(getCurrentUser()).toBeNull();
});
