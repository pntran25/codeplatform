import React, { useEffect } from "react";
import { BrowserRouter as Router, Routes, Route, Link } from "react-router-dom";
import Admin from "./Admin";
import Header from "./Header";
import ProblemList from "./ProblemList";
import ProblemPage from "./ProblemPage";
import { useCurrentUser } from "./auth";
import { Page, StatusMessage } from "./ui";
import "./App.css";

function Landing() {
  const currentUser = useCurrentUser();

  useEffect(() => {
    // The landing page is exactly one viewport tall; body padding would otherwise add a scrollbar.
    document.body.classList.add("no-scroll");
    return () => document.body.classList.remove("no-scroll");
  }, []);

  return (
    <div className="page" style={{ height: "100vh" }}>
      <a href="#main" className="skip-link">
        Skip to main content
      </a>
      <Header />
      <main id="main" className="hero" tabIndex={-1}>
        <p className="hero-kicker">this is</p>
        <h1 className="site-title">CODEXA</h1>
        <p className="hero-tagline">
          Practise Python coding problems in the browser. Your code and progress are saved to your account.
        </p>
        <div className="hero-actions">
          <Link to="/problems" className="button">
            Browse problems
          </Link>
          {currentUser?.role === "ADMIN" && (
            <Link to="/admin" className="button button-secondary">
              Manage problems
            </Link>
          )}
        </div>
      </main>
    </div>
  );
}

function NotFound() {
  return (
    <Page title="Not found">
      <StatusMessage>
        Page not found. <Link to="/">Go home</Link>
      </StatusMessage>
    </Page>
  );
}

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Landing />} />
        <Route path="/problems" element={<ProblemList />} />
        <Route path="/problems/:problemId" element={<ProblemPage />} />
        <Route path="/admin" element={<Admin />} />
        <Route path="*" element={<NotFound />} />
      </Routes>
    </Router>
  );
}

export default App;
