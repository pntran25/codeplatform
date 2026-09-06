import React, { useId, useState } from "react";
import api, { errorMessage } from "./axios";
import { login } from "./auth";

const MIN_PASSWORD_LENGTH = 8;

export default function LoginRegister({ onLogin }) {
  const id = useId();
  const [isLogin, setIsLogin] = useState(true);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const switchMode = () => {
    setIsLogin((wasLogin) => !wasLogin);
    setError("");
    setNotice("");
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setNotice("");
    setSubmitting(true);
    try {
      if (isLogin) {
        const res = await api.post("/api/auth/login", { username: username.trim(), password });
        login(res.data.token);
        if (onLogin) onLogin(res.data);
      } else {
        await api.post("/api/auth/register", { username: username.trim(), password });
        // Keep the username so the person can log straight in.
        setIsLogin(true);
        setPassword("");
        setNotice("Account created - enter your password again to log in.");
      }
    } catch (err) {
      setError(errorMessage(err, isLogin ? "Login failed." : "Registration failed."));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div>
      <h2>{isLogin ? "Log in" : "Create an account"}</h2>
      <form onSubmit={handleSubmit} noValidate>
        <div className="field">
          <label htmlFor={`${id}-username`}>Username</label>
          <input
            id={`${id}-username`}
            type="text"
            autoComplete="username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
            autoFocus
          />
        </div>
        <div className="field">
          <label htmlFor={`${id}-password`}>Password</label>
          <input
            id={`${id}-password`}
            type="password"
            autoComplete={isLogin ? "current-password" : "new-password"}
            aria-describedby={isLogin ? undefined : `${id}-password-hint`}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
          {!isLogin && (
            <div id={`${id}-password-hint`} className="field-hint" style={{ marginTop: 6 }}>
              At least {MIN_PASSWORD_LENGTH} characters.
            </div>
          )}
        </div>
        {error && (
          <div className="form-error" role="alert">
            {error}
          </div>
        )}
        {notice && (
          <div className="form-success" role="status">
            {notice}
          </div>
        )}
        <button type="submit" style={{ width: "100%" }} disabled={submitting}>
          {submitting ? "Please wait…" : isLogin ? "Log in" : "Register"}
        </button>
      </form>
      <button type="button" onClick={switchMode} className="login-switch">
        {isLogin ? "Need an account? Register" : "Already have an account? Log in"}
      </button>
    </div>
  );
}
