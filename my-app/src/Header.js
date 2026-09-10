import React, { useEffect, useRef, useState } from "react";
import { NavLink, Link } from "react-router-dom";
import logo from "./asset/images/codexa.png";
import LoginRegister from "./LoginRegister";
import { logout, useCurrentUser } from "./auth";

const FOCUSABLE = 'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])';

/**
 * Accessible modal: traps Tab within itself, closes on Escape or backdrop click, and returns
 * focus to whatever opened it.
 */
function Modal({ label, onClose, children }) {
  const panelRef = useRef(null);
  const openerRef = useRef(document.activeElement);

  useEffect(() => {
    const panel = panelRef.current;
    const first = panel.querySelector(FOCUSABLE);
    if (first) first.focus();

    const onKeyDown = (e) => {
      if (e.key === "Escape") {
        e.preventDefault();
        onClose();
        return;
      }
      if (e.key !== "Tab") return;
      const focusable = Array.from(panel.querySelectorAll(FOCUSABLE)).filter((el) => !el.disabled);
      if (focusable.length === 0) return;
      const firstEl = focusable[0];
      const lastEl = focusable[focusable.length - 1];
      if (e.shiftKey && document.activeElement === firstEl) {
        e.preventDefault();
        lastEl.focus();
      } else if (!e.shiftKey && document.activeElement === lastEl) {
        e.preventDefault();
        firstEl.focus();
      }
    };
    document.addEventListener("keydown", onKeyDown);
    const opener = openerRef.current;
    return () => {
      document.removeEventListener("keydown", onKeyDown);
      if (opener && typeof opener.focus === "function") opener.focus();
    };
  }, [onClose]);

  return (
    <div className="login-overlay" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div ref={panelRef} className="login-modal" role="dialog" aria-modal="true" aria-label={label}>
        <button type="button" onClick={onClose} className="login-modal-close" aria-label="Close">
          &times;
        </button>
        {children}
      </div>
    </div>
  );
}

function Header() {
  const [showLogin, setShowLogin] = useState(false);
  const currentUser = useCurrentUser();
  const closeLogin = React.useCallback(() => setShowLogin(false), []);

  return (
    <>
      <header className="main-header">
        <nav className="header-nav" aria-label="Primary">
          <Link to="/" className="header-brand" aria-label="Codexa home">
            <img src={logo} alt="" className="header-logo" />
            <span className="header-wordmark">CODEXA</span>
          </Link>
          <NavLink to="/problems" className="header-tab">
            Problems
          </NavLink>
          {currentUser?.role === "ADMIN" && (
            <NavLink to="/admin" className="header-tab">
              Admin
            </NavLink>
          )}
        </nav>
        <div className="header-actions">
          {currentUser ? (
            <>
              <span className="header-user" title={currentUser.username}>
                <span className="header-avatar" aria-hidden="true">
                  {currentUser.username.slice(0, 1)}
                </span>
                <span className="header-username">
                  <span className="visually-hidden">Signed in as </span>
                  {currentUser.username}
                </span>
              </span>
              <button type="button" onClick={logout} className="header-logout">
                Log out
              </button>
            </>
          ) : (
            <button
              type="button"
              className="button button-small"
              aria-haspopup="dialog"
              onClick={() => setShowLogin(true)}
            >
              Log in
            </button>
          )}
        </div>
      </header>

      {showLogin && (
        <Modal label="Log in or register" onClose={closeLogin}>
          <LoginRegister onLogin={closeLogin} />
        </Modal>
      )}
    </>
  );
}

export default Header;
