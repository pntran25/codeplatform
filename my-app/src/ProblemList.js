import React, { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import api from "./axios";
import { useCurrentUser } from "./auth";
import { DifficultyBadge, Page, ProgressBar, SkeletonList, Stat, StatusMessage } from "./ui";

const FILTERS = [
  { key: "ALL", label: "All" },
  { key: "EASY", label: "Easy" },
  { key: "MEDIUM", label: "Medium" },
  { key: "HARD", label: "Hard" },
  { key: "UNSOLVED", label: "Unsolved" },
];

export default function ProblemList() {
  const currentUser = useCurrentUser();
  const [problems, setProblems] = useState([]);
  const [submissions, setSubmissions] = useState([]);
  const [status, setStatus] = useState("loading");
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState("ALL");

  useEffect(() => {
    let cancelled = false;
    setStatus("loading");

    const problemsRequest = api.get("/api/problems");
    // Progress is per-user, so only ask for it when someone is signed in.
    const progressRequest = currentUser
      ? api.get("/api/submissions/mine").catch(() => ({ data: [] }))
      : Promise.resolve({ data: [] });

    Promise.all([problemsRequest, progressRequest])
      .then(([problemsRes, progressRes]) => {
        if (cancelled) return;
        setProblems(problemsRes.data || []);
        setSubmissions(progressRes.data || []);
        setStatus("ready");
      })
      .catch(() => {
        if (!cancelled) setStatus("error");
      });

    return () => {
      cancelled = true;
    };
  }, [currentUser]);

  const byProblem = useMemo(() => {
    const map = new Map();
    submissions.forEach((s) => map.set(s.problemId, s));
    return map;
  }, [submissions]);

  const solvedCount = problems.filter((p) => byProblem.get(p.id)?.solved).length;
  const attemptedCount = problems.filter((p) => {
    const progress = byProblem.get(p.id);
    return progress && !progress.solved;
  }).length;

  const visible = useMemo(() => {
    const needle = query.trim().toLowerCase();
    return problems.filter((p) => {
      if (needle && !(p.title || "").toLowerCase().includes(needle)) return false;
      if (filter === "ALL") return true;
      if (filter === "UNSOLVED") return !byProblem.get(p.id)?.solved;
      return String(p.difficulty || "").toUpperCase() === filter;
    });
  }, [problems, query, filter, byProblem]);

  const hasProblems = status === "ready" && problems.length > 0;

  return (
    <Page title="Problems">
      <div className="leetcode-container">
        <div className="list-header">
          <div>
            <p className="eyebrow">The set</p>
            <h1>Problems</h1>
            <p className="page-sub">Write a function, run the tests, move on to the next one.</p>
          </div>
          {currentUser && hasProblems && <ProgressBar solved={solvedCount} total={problems.length} />}
        </div>

        {status === "loading" && (
          <>
            <span className="visually-hidden" role="status">
              Loading problems…
            </span>
            <SkeletonList rows={6} />
          </>
        )}

        {status === "error" && (
          <StatusMessage isError>Could not load problems. Is the backend running?</StatusMessage>
        )}

        {status === "ready" && problems.length === 0 && <StatusMessage>No problems yet.</StatusMessage>}

        {hasProblems && (
          <>
            {currentUser ? (
              <div className="stat-row">
                <Stat value={problems.length} label="Problems" />
                <Stat value={solvedCount} label="Solved" tone="solved" />
                <Stat value={attemptedCount} label="In progress" tone="attempted" />
                <Stat value={problems.length - solvedCount - attemptedCount} label="Untouched" />
              </div>
            ) : (
              <p className="form-info" role="note">
                <span aria-hidden="true">✦</span> Log in to run code and have your progress saved.
              </p>
            )}

            <div className="toolbar">
              <div className="search-field">
                <svg
                  className="search-icon"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  aria-hidden="true"
                >
                  <circle cx="10.5" cy="10.5" r="6.5" />
                  <path d="m15.5 15.5 4 4" />
                </svg>
                <input
                  type="search"
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  placeholder="Search problems"
                  aria-label="Search problems by title"
                />
              </div>
              <div className="chip-row" role="group" aria-label="Filter problems">
                {FILTERS.map((f) => (
                  <button
                    key={f.key}
                    type="button"
                    className="chip"
                    aria-pressed={filter === f.key}
                    onClick={() => setFilter(f.key)}
                  >
                    {f.label}
                  </button>
                ))}
              </div>
              <span className="result-count" role="status">
                {visible.length} of {problems.length}
              </span>
            </div>

            {visible.length === 0 ? (
              <StatusMessage>Nothing matches that. Try a different search or filter.</StatusMessage>
            ) : (
              <ul className="problem-list">
                {visible.map((p) => {
                  const progress = byProblem.get(p.id);
                  const solved = Boolean(progress?.solved);
                  const attempted = Boolean(progress) && !solved;
                  return (
                    <li key={p.id}>
                      <Link to={`/problems/${p.id}`} className="problem-link">
                        <span
                          className={`solved-mark${solved ? " is-solved" : attempted ? " is-attempted" : ""}`}
                          aria-hidden="true"
                        >
                          ✓
                        </span>
                        <span className="problem-link-title">
                          <span>
                            {p.title}
                            <span className="visually-hidden">
                              {solved ? " (solved)" : attempted ? " (in progress)" : ""}
                            </span>
                          </span>
                        </span>
                        <span className="problem-link-meta">
                          {attempted && progress.totalCount > 0 && (
                            <span>
                              {progress.passedCount}/{progress.totalCount} passing
                            </span>
                          )}
                          <DifficultyBadge difficulty={p.difficulty} />
                        </span>
                      </Link>
                    </li>
                  );
                })}
              </ul>
            )}
          </>
        )}
      </div>
    </Page>
  );
}
