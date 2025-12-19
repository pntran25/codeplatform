import React, { useState } from "react";
import axios from "axios";

function Login({ onLogin }) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const res = await axios.post("/api/auth/login", { username, password });
      const token = res.data;
      localStorage.setItem("token", token);
      if (onLogin) onLogin(token);
    } catch (err) {
      setError(err.response?.data || "Login failed");
    }
    setLoading(false);
  };

  return (
    <form onSubmit={handleSubmit} style={{ maxWidth: 320, margin: "40px auto", display: "flex", flexDirection: "column", gap: 16 }}>
      <h2>Login</h2>
      <input
        type="text"
        value={username}
        onChange={e => setUsername(e.target.value)}
        placeholder="Username"
        required
      />
      <input
        type="password"
        value={password}
        onChange={e => setPassword(e.target.value)}
        placeholder="Password"
        required
      />
      <button type="submit" style={{ fontFamily: 'Gill Sans, "Gill Sans MT", Calibri, "Trebuchet MS", sans-serif' }} disabled={loading}>{loading ? "Logging in..." : "Login"}</button>
      {error && <div style={{ color: "salmon" }}>{error}</div>}
    </form>
  );
}

export default Login;
