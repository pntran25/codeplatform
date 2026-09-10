import React, { useEffect } from "react";
import { BrowserRouter as Router, Routes, Route, Link } from "react-router-dom";
import Admin from "./Admin";
import Header from "./Header";
import ProblemList from "./ProblemList";
import ProblemPage from "./ProblemPage";
import { useCurrentUser } from "./auth";
import { Footer, Page, StatusMessage } from "./ui";
import "./App.css";

const STEPS = [
  { title: "Pick a problem", body: "Browse the set and filter by difficulty until something looks worth an hour." },
  { title: "Write the function", body: "The signature is fixed on line one. Everything below it is yours." },
  { title: "Run the tests", body: "Green across the board marks the problem solved on your account." },
];

const SAMPLE_CODE = (
  <>
    <span className="tok-key">def</span> <span className="tok-fn">two_sum</span>(nums, target):{"\n"}
    {"    "}
    <span className="tok-com"># one pass, remembering what we have seen</span>
    {"\n"}
    {"    "}seen = {"{}"}
    {"\n"}
    {"    "}
    <span className="tok-key">for</span> i, n <span className="tok-key">in</span>{" "}
    <span className="tok-fn">enumerate</span>(nums):{"\n"}
    {"        "}
    <span className="tok-key">if</span> target - n <span className="tok-key">in</span> seen:{"\n"}
    {"            "}
    <span className="tok-key">return</span> [seen[target - n], i]{"\n"}
    {"        "}seen[n] = i{"\n"}
    {"    "}
    <span className="tok-key">return</span> []
  </>
);

/**
 * The animated field behind the landing page: three orbs on independent orbits under a
 * lattice that lights up as a slow sweep crosses it. Purely decorative.
 */
function LandingBackdrop() {
  return (
    <div className="landing-bg" aria-hidden="true">
      <span className="orb orb-a" />
      <span className="orb orb-b" />
      <span className="orb orb-c" />
      <span className="lattice" />
      <span className="lattice-sweep">
        <i />
      </span>
      <span className="stardust stardust-near" />
      <span className="stardust stardust-far" />
      <span className="horizon" />
    </div>
  );
}

function Landing() {
  const currentUser = useCurrentUser();

  // The landing owns its own background field, so the site-wide one steps aside.
  useEffect(() => {
    document.body.classList.add("landing-active");
    return () => document.body.classList.remove("landing-active");
  }, []);

  return (
    <div className="page landing">
      <a href="#main" className="skip-link">
        Skip to main content
      </a>
      <LandingBackdrop />
      <Header />

      <main id="main" tabIndex={-1}>
        <section className="hero">
          <p className="hero-kicker">this is</p>
          <h1 className="site-title gradient-text">CODEXA</h1>

          <div className="hero-actions">
            <Link to="/problems" className="button button-lg">
              Browse problems →
            </Link>
            {currentUser?.role === "ADMIN" ? (
              <Link to="/admin" className="button button-secondary button-lg">
                Manage problems
              </Link>
            ) : (
              <a href="#how" className="button button-secondary button-lg">
                How it works
              </a>
            )}
          </div>

          <div className="hero-window" aria-hidden="true">
            <div className="hero-window-bar">
              <i />
              <i />
              <i />
              <span>two_sum.py</span>
            </div>
            <pre>{SAMPLE_CODE}</pre>
            <div className="hero-window-foot">✓ 4 / 4 test cases passed</div>
          </div>
        </section>

        <section className="section" id="how" aria-labelledby="how-heading">
          <div className="container">
            <div className="section-head">
              <p className="eyebrow">How it works</p>
              <h2 id="how-heading">Three steps, then you&rsquo;re coding</h2>
            </div>
            <div className="steps">
              {STEPS.map((s) => (
                <article className="step" key={s.title}>
                  <h3>{s.title}</h3>
                  <p>{s.body}</p>
                </article>
              ))}
            </div>
          </div>
        </section>

        <section className="section" aria-labelledby="cta-heading">
          <div className="container">
            <div className="cta-band">
              <h2 id="cta-heading">Ready when you are</h2>
              <p>
                {currentUser
                  ? `Welcome back, ${currentUser.username}. Your saved code is waiting.`
                  : "Create an account to save your solutions and track what you have solved."}
              </p>
              <Link to="/problems" className="button button-lg">
                Start solving
              </Link>
            </div>
          </div>
        </section>
      </main>

      <Footer />
    </div>
  );
}

function NotFound() {
  return (
    <Page title="Not found">
      <div className="leetcode-container">
        <StatusMessage>
          Page not found. <Link to="/">Go home</Link>
        </StatusMessage>
      </div>
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
