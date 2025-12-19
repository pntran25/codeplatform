import React, { useState } from "react";
import { Link } from "react-router-dom";
import logo from './asset/images/codexa.png';
import LoginRegister from "./LoginRegister";

function Header() {
  const [showLogin, setShowLogin] = useState(false);
  const [username, setUsername] = useState(() => localStorage.getItem('username') || '');

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    setUsername('');
  };

  // Update username on login
  const handleLogin = (uname) => {
    setUsername(uname);
    localStorage.setItem('username', uname);
    setShowLogin(false);
  };

  return (
    <>
      <div className="main-header">
        <div style={{ display: 'flex', gap: 32, flex: 1 }}>
          <Link to="/">
            <img
              src={logo}
              alt='Codexa Logo'
              style={{ width: 40, height: 40, marginTop: 16}} // moved logo down
            />
          </Link>
          <Link to="/problems" className="header-tab">Problems</Link>
        </div>
        <div style={{ marginRight: 32, display: 'flex', gap: 16 }}>
          {username ? (
            <>
              <span style={{ color: '#fff', fontFamily: 'Gill Sans, "Gill Sans MT", Calibri, "Trebuchet MS", sans-serif', fontWeight: 700, fontSize: 18, marginRight: 8, marginTop: 20 }}>
                {username}
              </span>
              <button onClick={handleLogout} style={{ background: '#23232b', color: '#fff', border: '1px solid #fff2', borderRadius: 6, padding: '4px 12px', cursor: 'pointer' }}>
                Logout
              </button>
            </>
          ) : (
            <span
              title="Login/Register"
              style={{
                cursor: 'pointer',
                color: '#fff',
                fontSize: 0,
                fontFamily: 'Butch Lite',
                fontWeight: 700,
                letterSpacing: 1,
                padding: '0 8px',
                userSelect: 'none',
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                width: 36,
                height: 36,

                transition: 'background 0.2s',
                overflow: 'hidden',
              }}
              onClick={() => setShowLogin(true)}
            >
              <img src={require('./asset/images/user.png')} alt="Login/Register" style={{ width: 42, height: 42, opacity: 0.5 }} />
            </span>
          )}
        </div>
      </div>
      {showLogin && (
        <div className="login-overlay">
          <div className="login-modal">
            <button onClick={() => setShowLogin(false)} className="login-modal-close">&times;</button>
            <LoginRegister onLogin={handleLogin} />
          </div>
        </div>
      )}
    </>
  );
}

export default Header;