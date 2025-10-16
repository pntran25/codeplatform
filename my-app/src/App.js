
import React, { useState, useEffect } from "react";
import { BrowserRouter as Router, Routes, Route, Link, useParams } from "react-router-dom";
import MonacoEditor from "@monaco-editor/react";
import axios from "./axios";
import Admin from "./Admin";
import "./App.css";
import { useNavigate } from "react-router-dom";
import Header from "./Header";


function ProblemListPage() {
  const [problems, setProblems] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    axios.get("/api/problems")
      .then(res => {
        setProblems(res.data || []);
        setLoading(false);
      })
      .catch(() => setLoading(false));
  }, []);

  if (loading) return <div className="leetcode-container"> <h1>Loading problems...</h1></div>;
  if (!problems.length) return <div className="leetcode-container">No problems found.</div>;
      
  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <Header />
      <div className="leetcode-container" style={{ flex: 1, marginTop: 90 }}>
        <h2 style={{ color: '#702effff', fontWeight: 700, marginBottom: 24 }}>Problems</h2>
        <ul className="problem-list">
          {problems.map(p => (
            <li key={p.id}>
              <Link to={`/problems/${p.id}`} className="problem-link">
                {p.title}
              </Link>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}

function CodingPlatform() {
  const { problemId } = useParams();
  const [problem, setProblem] = useState(null);
  const [functionBody, setFunctionBody] = useState("");
  const [output, setOutput] = useState("");
  const [loading, setLoading] = useState(false);
  const [results, setResults] = useState([]);
  const navigate = useNavigate(); 

  useEffect(() => {
    if (!problemId) return;
    axios.get(`/api/problems/${problemId}`)
      .then(res => {
        if (!res.data) {
          setProblem(null);
          setFunctionBody("    # Write your code here\n");
          return;
        }
        setProblem(res.data);
        if (res.data.starterCode) {
          const lines = res.data.starterCode.split("\n");
          setFunctionBody(lines.slice(1).join("\n"));
        } else {
          setFunctionBody("    # Write your code here\n");
        }
      })
      .catch(() => {
        setProblem(null);
        setFunctionBody("    # Write your code here\n");
      });
  }, [problemId]);

  const handleRunCode = async () => {
    setLoading(true);
    setOutput("");
    setResults([]);
    try {
      const response = await axios.post("/api/execute", {
        functionBody,
        problemId,
        userId: 1
      });
      setOutput(response.data.rawOutput || "");
      setResults(response.data.results || []);
    } catch (err) {
      setOutput("Error running code.");
    }
    setLoading(false);
  };

  if (!problem) return <div className="leetcode-container">Loading...</div>;

  return (
    <>
      <div className="leetcode-container">
        <div className="top-bar">
          <div style={{ flex: 1, textAlign: "left" }}>
            <h2 style={{ color: '#d5d6d8ff', fontWeight: 700 }}>{problem.title}</h2>
          </div>
          <button className="back-to-menu-btn" onClick={() => navigate("/problems")}> 
            ←  Back to Problems
          </button>
        </div>
        <div className="problem-section">
          <p style={{ color: '#bbb' }}>{problem.description}</p>
          <pre><b>Function Signature:</b> {problem.functionSignature}</pre>
          <div style={{color: '#ffb347', marginBottom: 8, fontWeight: 500}}>
            <b>Note:</b> Your function <u>must use <code>return</code></u> to output the answer. Do <b>not</b> use <code>print</code>.
          </div>
          {problem.testCases && Array.isArray(problem.testCases) && problem.testCases.length > 0 && (
            <details>
              <summary>Show Test Cases</summary>
              <ul>
                {problem.testCases.map((tc, idx) => (
                  <li key={idx}>
                    <b>Input:</b> {tc.input} | <b>Expected:</b> {tc.expected}
                  </li>
                ))}
              </ul>
            </details>
          )}
        </div>
        <div className="editor-section">
          <MonacoEditor
            height="300px"
            defaultLanguage="python"
            value={problem.functionSignature + "\n" + functionBody}
            onChange={value => {
              const lines = value.split("\n");
              setFunctionBody(lines.slice(1).join("\n"));
            }}
            theme="vs-dark"
          />
          <button className="run-btn" onClick={handleRunCode} disabled={loading}>
            {loading ? "Running..." : "Run Code"}
          </button>
          <div className="output-section">
            <label>Test Case Results:</label>
            <ul>
              {results.map((r, i) => (
                <li key={i} style={{ color: r.pass ? "lightgreen" : "salmon" }}>
                  Input: {JSON.stringify(r.input)} | Expected: {JSON.stringify(r.expected)} | Got: {JSON.stringify(r.actual)} | {r.pass ? "PASS" : "FAIL"}
                  {r.error && <span style={{ color: 'orange', marginLeft: 8 }}>({r.error})</span>}
                </li>
              ))}
            </ul>
            {output && (
              <>
                <label>Raw Output:</label>
                <pre>{output}</pre>
              </>
            )}
          </div>
        </div>
      </div>

    </>
  );
}

function MainCard() {
  return (
    <div style={{ height: '100vh', display: 'flex', flexDirection: 'column' }}>
      <Header />
      <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', marginTop: 0, flexDirection: 'column' }}>
      <p style={{ color: "#4334a580", fontSize: 30 }}>this is</p>
      <h1 className="site-title" style={{ textAlign: 'center'}}>CODEXA</h1>
      </div>
    </div>
    );
}

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/admin" element={<Admin />} />
        <Route path="/problems/:problemId" element={<CodingPlatform />} />
        <Route path="/problems" element={<ProblemListPage />} />
        <Route path="/" element={<MainCard />} />
      </Routes>
    </Router>
  );
}

export default App;