import React, { useState } from 'react';
import axios from './axios';

export default function LoginRegister({ onLogin }) {
  const [isLogin, setIsLogin] = useState(true);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      if (isLogin) {
        const res = await axios.post('/api/auth/login', { username, password });
        localStorage.setItem('token', res.data.token);
        if (onLogin) onLogin(username);
      } else {
        await axios.post('/api/auth/register', { username, password });
        setIsLogin(true);
      }
    } catch (err) {
      setError(
        typeof err.response?.data === 'string'
          ? err.response.data
          : err.response?.data?.message || 'Error'
      );
    }
  };

  return (
    <div style={{ maxWidth: 400, margin: 'auto', padding: 20 }}>
      <h2 style={{ fontFamily: 'Gill Sans, "Gill Sans MT", Calibri, "Trebuchet MS", sans-serif' }}>{isLogin ? 'Login' : 'Register'}</h2>
      <form onSubmit={handleSubmit}>
        <input
          type="text"
          placeholder="Username"
          value={username}
          onChange={e => setUsername(e.target.value)}
          required
          style={{ width: '100%', marginBottom: 10 }}
        />
        <input
          type="password"
          placeholder="Password"
          value={password}
          onChange={e => setPassword(e.target.value)}
          required
          style={{ width: '100%', marginBottom: 10 }}
        />
        {error && <div style={{ color: 'red', marginBottom: 10 }}>{error}</div>}
        <button type="submit" style={{ width: '100%', fontFamily: 'Gill Sans, "Gill Sans MT", Calibri, "Trebuchet MS", sans-serif'  }}>
          {isLogin ? 'Login' : 'Register'}
        </button>
      </form>
      <button onClick={() => setIsLogin(!isLogin)} style={{ marginTop: 10, width: '100%', fontFamily: 'Gill Sans, "Gill Sans MT", Calibri, "Trebuchet MS", sans-serif' }}>
        {isLogin ? 'Need an account? Register' : 'Already have an account? Login'}
      </button>
    </div>
  );
}
