import React, { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import MonacoEditor from "@monaco-editor/react";
import api, { errorMessage } from "./axios";
import { useCurrentUser } from "./auth";
import { DifficultyBadge, Page, StatusMessage } from "./ui";

const DEFAULT_BODY = "    # Write your code here\n";
const AUTOSAVE_DELAY_MS = 1200;

/** The editor shows the signature on line 1; only the lines after it are the user's code. */
function bodyOf(fullCode) {
  return (fullCode ?? "").split("\n").slice(1).join("\n");
}

function starterBodyFor(problem) {
  return problem.starterCode ? bodyOf(problem.starterCode) : DEFAULT_BODY;
}

export default function ProblemPage() {
  const { problemId } = useParams();
  const navigate = useNavigate();
  const currentUser = useCurrentUser();

  const [problem, setProblem] = useState(null);
  const [status, setStatus] = useState("loading");
  const [functionBody, setFunctionBody] = useState(DEFAULT_BODY);
  const [solved, setSolved] = useState(false);

  const [running, setRunning] = useState(false);
  const [results, setResults] = useState(null);
  const [output, setOutput] = useState("");
  const [runError, setRunError] = useState("");

  // "clean" = matches what the server has; "dirty" = edited since; "saving"; "saved"; "error"
  const [saveState, setSaveState] = useState("clean");
  const lastSavedRef = useRef(null);
  const resultsRef = useRef(null);

  // Load the problem and, for a signed-in user, the code they last saved on it.
  useEffect(() => {
    let cancelled = false;
    setStatus("loading");
    setResults(null);
    setOutput("");
    setRunError("");
    setSaveState("clean");

    const problemRequest = api.get(`/api/problems/${problemId}`);
    const savedRequest = currentUser
      ? api.get(`/api/submissions/problem/${problemId}`).catch(() => ({ data: null }))
      : Promise.resolve({ data: null });

    Promise.all([problemRequest, savedRequest])
      .then(([problemRes, savedRes]) => {
        if (cancelled) return;
        const loadedProblem = problemRes.data;
        const saved = savedRes.data;
        setProblem(loadedProblem);
        setSolved(Boolean(saved?.solved));
        const body = saved?.code ? saved.code : starterBodyFor(loadedProblem);
        setFunctionBody(body);
        lastSavedRef.current = saved?.code ?? null;
        setStatus("ready");
      })
      .catch((err) => {
        if (!cancelled) setStatus(err.response?.status === 404 ? "missing" : "error");
      });

    return () => {
      cancelled = true;
    };
  }, [problemId, currentUser]);

  // Debounced autosave so a signed-in user never loses work by navigating away.
  useEffect(() => {
    if (status !== "ready" || !currentUser) return undefined;
    if (functionBody === lastSavedRef.current) return undefined;

    setSaveState("dirty");
    const timer = setTimeout(async () => {
      setSaveState("saving");
      try {
        await api.put(`/api/submissions/problem/${problemId}`, { code: functionBody });
        lastSavedRef.current = functionBody;
        setSaveState("saved");
      } catch {
        setSaveState("error");
      }
    }, AUTOSAVE_DELAY_MS);
    return () => clearTimeout(timer);
  }, [functionBody, problemId, currentUser, status]);

  const handleRunCode = useCallback(async () => {
    if (running || !currentUser) return;
    setRunning(true);
    setOutput("");
    setRunError("");
    setResults(null);
    try {
      const response = await api.post("/api/execute", { functionBody, problemId: Number(problemId) });
      const data = response.data;
      setOutput(data.rawOutput || "");
      setResults(data.results || []);
      if (data.solved) setSolved(true);
      if (data.error) setRunError(data.error);
      // The run also persisted the code server-side.
      lastSavedRef.current = functionBody;
      setSaveState("saved");
    } catch (err) {
      setRunError(
        err.response?.status === 401 ? "Log in to run code." : errorMessage(err, "Could not run your code.")
      );
    } finally {
      setRunning(false);
      // Move screen-reader and keyboard focus to the outcome.
      resultsRef.current?.focus();
    }
  }, [functionBody, problemId, running, currentUser]);

  const handleReset = () => {
    if (!problem) return;
    if (functionBody !== starterBodyFor(problem) && !window.confirm("Replace your code with the starter code?")) {
      return;
    }
    setFunctionBody(starterBodyFor(problem));
  };

  // Ctrl/Cmd+Enter runs from anywhere on the page, including inside the editor.
  useEffect(() => {
    const onKeyDown = (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key === "Enter") {
        e.preventDefault();
        handleRunCode();
      }
    };
    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, [handleRunCode]);

  if (status === "loading") {
    return (
      <Page title="Problem">
        <StatusMessage>Loading problem…</StatusMessage>
      </Page>
    );
  }
  if (status !== "ready") {
    return (
      <Page title="Problem">
        <StatusMessage isError>
          {status === "missing" ? "That problem does not exist." : "Could not load this problem."}
        </StatusMessage>
        <div style={{ textAlign: "center" }}>
          <button type="button" className="button-secondary" onClick={() => navigate("/problems")}>
            Back to problems
          </button>
        </div>
      </Page>
    );
  }

  const signature = problem.functionSignature || "";
  const passed = results ? results.filter((r) => r.pass).length : 0;
  const allPassed = results && results.length > 0 && passed === results.length;

  const saveLabel = {
    clean: "",
    dirty: "Unsaved changes",
    saving: "Saving…",
    saved: "Saved",
    error: "Could not save",
  }[saveState];

  return (
    <Page title={problem.title}>
      <div className="leetcode-container">
        <div className="top-bar">
          <div className="problem-title-row">
            <h1 className="problem-title">{problem.title}</h1>
            <DifficultyBadge difficulty={problem.difficulty} />
            {solved && (
              <span className="badge badge-easy" aria-label="You have solved this problem">
                ✓ Solved
              </span>
            )}
          </div>
          <button type="button" className="back-to-menu-btn" onClick={() => navigate("/problems")}>
            ← Back to problems
          </button>
        </div>

        <div className="workspace">
          <section className="problem-section" aria-labelledby="problem-heading">
            <h2 id="problem-heading">Problem</h2>
            <p className="problem-description">{problem.description}</p>
            <pre aria-label="Function signature">{signature}</pre>
            <div className="problem-note" role="note">
              Your function must <code>return</code> its answer - do not use <code>print</code>.
            </div>
            {problem.testCases?.length > 0 && (
              <details>
                <summary>Show test cases ({problem.testCases.length})</summary>
                <ul className="testcase-list">
                  {problem.testCases.map((tc, i) => (
                    <li key={tc.id ?? i}>
                      <b>Input</b> {tc.input}
                      <br />
                      <b>Expected</b> {tc.expected}
                    </li>
                  ))}
                </ul>
              </details>
            )}
          </section>

          <section className="editor-section" aria-labelledby="editor-heading">
            <div className="editor-toolbar">
              <h2 id="editor-heading" style={{ margin: 0 }}>
                Your solution
              </h2>
              <span className={`save-state is-${saveState}`} role="status" aria-live="polite">
                {currentUser ? saveLabel : "Log in to save your code"}
              </span>
            </div>

            <div className="editor-frame">
              <MonacoEditor
                height="340px"
                defaultLanguage="python"
                value={`${signature}\n${functionBody}`}
                onChange={(value) => setFunctionBody(bodyOf(value))}
                theme="vs-dark"
                options={{
                  minimap: { enabled: false },
                  fontSize: 14,
                  scrollBeyondLastLine: false,
                  tabSize: 4,
                  insertSpaces: true,
                  accessibilitySupport: "auto",
                  ariaLabel: "Python code editor",
                }}
              />
            </div>

            <div className="editor-actions">
              <button
                type="button"
                className="run-btn"
                onClick={handleRunCode}
                disabled={running || !currentUser}
                aria-describedby="run-hint"
              >
                {running ? "Running…" : currentUser ? "Run code" : "Log in to run code"}
              </button>
              <button type="button" className="button-secondary button-small" onClick={handleReset}>
                Reset to starter code
              </button>
              <span id="run-hint" className="kbd-hint">
                <kbd>Ctrl</kbd> + <kbd>Enter</kbd> to run
              </span>
            </div>

            <div className="output-section" ref={resultsRef} tabIndex={-1} aria-live="polite" aria-atomic="true">
              <div className="output-header">
                <label style={{ margin: 0 }}>Test results</label>
                {results && (
                  <span className={`results-summary ${allPassed ? "result-pass" : "result-fail"}`}>
                    {passed}/{results.length} passed
                  </span>
                )}
              </div>

              {allPassed && (
                <div className="solved-banner" style={{ marginBottom: 10 }}>
                  <span aria-hidden="true">🎉</span> All test cases pass - problem solved!
                </div>
              )}

              {runError && (
                <div className="form-error" role="alert" style={{ marginTop: 0 }}>
                  {runError}
                </div>
              )}

              {results && results.length > 0 && (
                <ul className="results-list">
                  {results.map((r, i) => (
                    <li key={i}>
                      <span className={`result-status ${r.pass ? "result-pass" : "result-fail"}`}>
                        {r.pass ? "PASS" : "FAIL"}
                      </span>
                      <span className="result-detail">
                        <span>
                          <b>Input</b> {r.input}
                        </span>
                        <span>
                          <b>Expected</b> {r.expected}
                        </span>
                        <span>
                          <b>Got</b> {r.actual === null || r.actual === undefined ? "—" : r.actual}
                        </span>
                        {r.error && <span className="result-error">{r.error}</span>}
                      </span>
                    </li>
                  ))}
                </ul>
              )}

              {!results && !runError && (
                <p className="output-placeholder" style={{ margin: 0 }}>
                  {currentUser
                    ? "Run your code to check it against the test cases."
                    : "Log in to run your code against the test cases."}
                </p>
              )}

              {output && (
                <details style={{ marginTop: 12 }}>
                  <summary>Raw output</summary>
                  <pre>{output}</pre>
                </details>
              )}
            </div>
          </section>
        </div>
      </div>
    </Page>
  );
}
