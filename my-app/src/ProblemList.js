import React, { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import api from "./axios";
import { useCurrentUser } from "./auth";
import { DifficultyBadge, Page, ProgressBar, StatusMessage } from "./ui";

export default function ProblemList() {
  const currentUser = useCurrentUser();
  const [problems, setProblems] = useState([]);
  const [submissions, setSubmissions] = useState([]);
  const [status, setStatus] = useState("loading");

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

  return (
    <Page title="Problems">
      <div className="leetcode-container">
        <div className="list-header">
          <h1>Problems</h1>
          {currentUser && status === "ready" && problems.length > 0 && (
            <ProgressBar solved={solvedCount} total={problems.length} />
          )}
        </div>

        {status === "loading" && <StatusMessage>Loading problems…</StatusMessage>}
        {status === "error" && (
          <StatusMessage isError>Could not load problems. Is the backend running?</StatusMessage>
        )}
        {status === "ready" && problems.length === 0 && <StatusMessage>No problems yet.</StatusMessage>}

        {status === "ready" && problems.length > 0 && (
          <>
            {!currentUser && (
              <p className="form-info" role="note">
                Log in to run code and have your progress saved.
              </p>
            )}
            <ul className="problem-list">
              {problems.map((p) => {
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
                      <span>
                        {p.title}
                        <span className="visually-hidden">
                          {solved ? " (solved)" : attempted ? " (in progress)" : ""}
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
          </>
        )}
      </div>
    </Page>
  );
}
