import React from "react";
import Header from "./Header";

/** Wraps a page in the fixed header, a skip link, and the top spacing the header needs. */
export function Page({ children, title }) {
  return (
    <div className="page">
      <a href="#main" className="skip-link">
        Skip to main content
      </a>
      <Header />
      <main id="main" className="page-body" tabIndex={-1} aria-label={title}>
        {children}
      </main>
    </div>
  );
}

export function StatusMessage({ children, isError = false }) {
  return (
    <div className={`status-message${isError ? " is-error" : ""}`} role={isError ? "alert" : "status"}>
      {children}
    </div>
  );
}

const DIFFICULTY_LABEL = { EASY: "Easy", MEDIUM: "Medium", HARD: "Hard" };

export function DifficultyBadge({ difficulty }) {
  if (!difficulty) return null;
  const key = String(difficulty).toUpperCase();
  const label = DIFFICULTY_LABEL[key];
  if (!label) return null;
  return <span className={`badge badge-${key.toLowerCase()}`}>{label}</span>;
}

/** Progress indicator used on the problem list. */
export function ProgressBar({ solved, total }) {
  const pct = total ? Math.round((solved / total) * 100) : 0;
  return (
    <div className="progress" role="group" aria-label="Your progress">
      <div className="progress-label">
        <span>
          {solved} of {total} solved
        </span>
        <span>{pct}%</span>
      </div>
      <div
        className="progress-bar"
        role="progressbar"
        aria-valuemin={0}
        aria-valuemax={total}
        aria-valuenow={solved}
        aria-valuetext={`${solved} of ${total} problems solved`}
      >
        <div style={{ width: `${pct}%` }} />
      </div>
    </div>
  );
}
