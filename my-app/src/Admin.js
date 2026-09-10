import React, { useCallback, useEffect, useId, useState } from "react";
import api, { errorMessage } from "./axios";
import { useCurrentUser } from "./auth";
import { DifficultyBadge, Page, StatusMessage } from "./ui";
import userImage from "./asset/images/guy.png";

const EMPTY_TEST_CASE = { input: "", expected: "" };
const EMPTY_FORM = {
  title: "",
  difficulty: "",
  description: "",
  functionSignature: "",
  starterCode: "",
  testCases: [{ ...EMPTY_TEST_CASE }],
};

function formFromProblem(problem) {
  return {
    title: problem.title || "",
    difficulty: problem.difficulty || "",
    description: problem.description || "",
    functionSignature: problem.functionSignature || "",
    starterCode: problem.starterCode || "",
    testCases:
      problem.testCases?.length > 0
        ? problem.testCases.map((tc) => ({ input: tc.input || "", expected: tc.expected || "" }))
        : [{ ...EMPTY_TEST_CASE }],
  };
}

function Admin() {
  const currentUser = useCurrentUser();
  const id = useId();

  const [problems, setProblems] = useState([]);
  const [listStatus, setListStatus] = useState("loading");
  const [form, setForm] = useState(EMPTY_FORM);
  const [editingId, setEditingId] = useState(null);
  const [message, setMessage] = useState(null);
  const [saving, setSaving] = useState(false);

  const loadProblems = useCallback(() => {
    setListStatus("loading");
    api
      .get("/api/problems")
      .then((res) => {
        setProblems(res.data || []);
        setListStatus("ready");
      })
      .catch(() => setListStatus("error"));
  }, []);

  useEffect(() => {
    if (currentUser?.role === "ADMIN") loadProblems();
  }, [currentUser, loadProblems]);

  // The server also enforces this; the check here only avoids showing a form that cannot succeed.
  if (currentUser?.role !== "ADMIN") {
    return (
      <Page title="Admin">
        <div className="access-denied" role="alert">
          {currentUser ? "Access denied: admins only" : "Log in as an admin to manage problems"}
          <div>
            <img src={userImage} alt="" />
          </div>
        </div>
      </Page>
    );
  }

  const setField = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }));

  const updateTestCase = (index, field, value) =>
    setForm((f) => ({
      ...f,
      testCases: f.testCases.map((tc, i) => (i === index ? { ...tc, [field]: value } : tc)),
    }));

  const addTestCase = () => setForm((f) => ({ ...f, testCases: [...f.testCases, { ...EMPTY_TEST_CASE }] }));

  const removeTestCase = (index) =>
    setForm((f) => ({
      ...f,
      testCases: f.testCases.length === 1 ? f.testCases : f.testCases.filter((_, i) => i !== index),
    }));

  const startEdit = (problem) => {
    setEditingId(problem.id);
    setForm(formFromProblem(problem));
    setMessage(null);
    document.getElementById(`${id}-title`)?.focus();
  };

  const cancelEdit = () => {
    setEditingId(null);
    setForm(EMPTY_FORM);
    setMessage(null);
  };

  const handleDelete = async (problem) => {
    if (!window.confirm(`Delete "${problem.title}"? This also removes everyone's saved code for it.`)) return;
    try {
      await api.delete(`/api/problems/${problem.id}`);
      if (editingId === problem.id) cancelEdit();
      setMessage({ type: "success", text: `Deleted "${problem.title}".` });
      loadProblems();
    } catch (err) {
      setMessage({ type: "error", text: errorMessage(err, "Could not delete the problem.") });
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage(null);

    if (!/^\s*def\s+\w+\s*\(/.test(form.functionSignature)) {
      setMessage({ type: "error", text: 'Function signature must look like "def add(a, b):".' });
      return;
    }

    setSaving(true);
    const payload = {
      title: form.title.trim(),
      difficulty: form.difficulty || null,
      description: form.description,
      functionSignature: form.functionSignature.trim(),
      starterCode: form.starterCode,
      testCases: form.testCases.map((tc) => ({ input: tc.input.trim(), expected: tc.expected.trim() })),
    };
    try {
      if (editingId) {
        await api.put(`/api/problems/${editingId}`, payload);
        setMessage({ type: "success", text: `Updated "${payload.title}".` });
      } else {
        await api.post("/api/problems", payload);
        setMessage({ type: "success", text: `Created "${payload.title}".` });
      }
      setEditingId(null);
      setForm(EMPTY_FORM);
      loadProblems();
    } catch (err) {
      setMessage({ type: "error", text: errorMessage(err, "Could not save the problem.") });
    } finally {
      setSaving(false);
    }
  };

  return (
    <Page title="Admin">
      <div className="leetcode-container">
        <div className="list-header">
          <div>
            <p className="eyebrow">Admin</p>
            <h1>Manage problems</h1>
            <p className="page-sub">Add, edit and retire the problems everyone sees.</p>
          </div>
        </div>

        {message && (
          <div className={message.type === "error" ? "form-error" : "form-success"} role={message.type === "error" ? "alert" : "status"}>
            {message.text}
          </div>
        )}

        <div className="admin-layout">
          <section className="admin-panel" aria-labelledby="admin-list-heading">
            <h2 id="admin-list-heading">
              Existing problems
              {listStatus === "ready" && <span className="badge badge-neutral">{problems.length}</span>}
            </h2>
            {listStatus === "loading" && <StatusMessage>Loading…</StatusMessage>}
            {listStatus === "error" && <StatusMessage isError>Could not load problems.</StatusMessage>}
            {listStatus === "ready" && problems.length === 0 && (
              <p className="output-placeholder">No problems yet - add one with the form.</p>
            )}
            {listStatus === "ready" && problems.length > 0 && (
              <ul className="admin-problem-list">
                {problems.map((p) => (
                  <li key={p.id} className={p.id === editingId ? "is-editing" : ""}>
                    <span className="admin-problem-title">
                      <span>{p.title}</span>
                      <DifficultyBadge difficulty={p.difficulty} />
                    </span>
                    <span className="admin-problem-actions">
                      <button
                        type="button"
                        className="button-secondary button-small"
                        onClick={() => startEdit(p)}
                        aria-label={`Edit ${p.title}`}
                      >
                        Edit
                      </button>
                      <button
                        type="button"
                        className="button-danger button-small"
                        onClick={() => handleDelete(p)}
                        aria-label={`Delete ${p.title}`}
                      >
                        Delete
                      </button>
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section className="admin-panel" aria-labelledby="admin-form-heading">
            <h2 id="admin-form-heading">{editingId ? "Edit problem" : "Add a new problem"}</h2>
            <form onSubmit={handleSubmit}>
              <div className="field">
                <label htmlFor={`${id}-title`}>Title</label>
                <input id={`${id}-title`} value={form.title} onChange={setField("title")} required />
              </div>

              <div className="field">
                <label htmlFor={`${id}-difficulty`}>Difficulty</label>
                <select id={`${id}-difficulty`} value={form.difficulty} onChange={setField("difficulty")}>
                  <option value="">Not set</option>
                  <option value="EASY">Easy</option>
                  <option value="MEDIUM">Medium</option>
                  <option value="HARD">Hard</option>
                </select>
              </div>

              <div className="field">
                <label htmlFor={`${id}-description`}>Description</label>
                <textarea
                  id={`${id}-description`}
                  value={form.description}
                  onChange={setField("description")}
                  required
                />
              </div>

              <div className="field">
                <label htmlFor={`${id}-signature`}>Function signature</label>
                <p className="field-hint" id={`${id}-signature-hint`}>
                  One Python <code>def</code> line, e.g. <code>def add(a, b):</code>
                </p>
                <input
                  id={`${id}-signature`}
                  aria-describedby={`${id}-signature-hint`}
                  value={form.functionSignature}
                  onChange={setField("functionSignature")}
                  placeholder="def add(a, b):"
                  required
                  className="code"
                />
              </div>

              <div className="field">
                <label htmlFor={`${id}-starter`}>Starter code (optional)</label>
                <p className="field-hint" id={`${id}-starter-hint`}>
                  Shown in the editor before the user starts. Line 1 should be the signature.
                </p>
                <textarea
                  id={`${id}-starter`}
                  className="code"
                  aria-describedby={`${id}-starter-hint`}
                  value={form.starterCode}
                  onChange={setField("starterCode")}
                  placeholder={"def add(a, b):\n    # Write your code here"}
                />
              </div>

              <fieldset>
                <legend>Test cases</legend>
                <p className="field-hint">
                  Input is the Python argument list, e.g. <code>[2,7,11,15], 9</code>. Expected is what the
                  function should return, as Python would print it, e.g. <code>[0, 1]</code>.
                </p>
                {form.testCases.map((tc, idx) => (
                  <div key={idx} className="admin-testcase">
                    <input
                      aria-label={`Test case ${idx + 1} input`}
                      placeholder="Input, e.g. 2,3"
                      value={tc.input}
                      onChange={(e) => updateTestCase(idx, "input", e.target.value)}
                      required
                    />
                    <input
                      aria-label={`Test case ${idx + 1} expected output`}
                      placeholder="Expected, e.g. 5"
                      value={tc.expected}
                      onChange={(e) => updateTestCase(idx, "expected", e.target.value)}
                      required
                    />
                    <button
                      type="button"
                      className="button-secondary button-small"
                      style={{ minHeight: 44 }}
                      onClick={() => removeTestCase(idx)}
                      disabled={form.testCases.length === 1}
                      aria-label={`Remove test case ${idx + 1}`}
                    >
                      Remove
                    </button>
                  </div>
                ))}
                <button type="button" className="button-secondary button-small" onClick={addTestCase}>
                  + Add test case
                </button>
              </fieldset>

              <div className="form-actions">
                <button type="submit" disabled={saving}>
                  {saving ? "Saving…" : editingId ? "Save changes" : "Create problem"}
                </button>
                {editingId && (
                  <button type="button" className="button-secondary" onClick={cancelEdit}>
                    Cancel
                  </button>
                )}
              </div>
            </form>
          </section>
        </div>
      </div>
    </Page>
  );
}

export default Admin;
