import React, { useState } from "react";
import axios from "axios";

function Admin() {
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [functionSignature, setFunctionSignature] = useState("");
  const [starterCode, setStarterCode] = useState("");
  const [testCases, setTestCases] = useState([{ input: "", expected: "" }]);
  const [message, setMessage] = useState("");

  const handleAddTestCase = () => {
    setTestCases([...testCases, { input: "", expected: "" }]);
  };

  const handleTestCaseChange = (idx, field, value) => {
    const updated = [...testCases];
    updated[idx][field] = value;
    setTestCases(updated);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage("");
    try {
      // 1. Create the problem
      const problemRes = await axios.post("/api/problems", {
        title,
        description,
        functionSignature,
        starterCode,
      });
      const problemId = problemRes.data.id;

      // 2. Add test cases
      for (const tc of testCases) {
        await axios.post(`/api/problems/${problemId}/testcases`, {
          input: tc.input,
          expected: tc.expected,
        });
      }
      setMessage("Problem and test cases added!");
      setTitle("");
      setDescription("");
      setFunctionSignature("");
      setStarterCode("");
      setTestCases([{ input: "", expected: "" }]);
    } catch (err) {
      setMessage("Error saving problem.");
    }
  };

  return (
    <div style={{ maxWidth: 600, margin: "40px auto" }}>
      <h2>Add New Problem</h2>
      <form onSubmit={handleSubmit}>
        <div>
          <label>Title:</label>
          <input value={title} onChange={e => setTitle(e.target.value)} required />
        </div>
        <div>
          <label>Description:</label>
          <textarea value={description} onChange={e => setDescription(e.target.value)} required />
        </div>
        <div>
          <label>Function Signature:</label>
          <input value={functionSignature} onChange={e => setFunctionSignature(e.target.value)} required />
        </div>
        <div>
          <label>Starter Code:</label>
          <textarea value={starterCode} onChange={e => setStarterCode(e.target.value)} />
        </div>
        <div>
          <label>Test Cases:</label>
          {testCases.map((tc, idx) => (
            <div key={idx} style={{ marginBottom: 8 }}>
              <input
                placeholder="Input (e.g. 2,3)"
                value={tc.input}
                onChange={e => handleTestCaseChange(idx, "input", e.target.value)}
                required
              />
              <input
                placeholder="Expected (e.g. 5)"
                value={tc.expected}
                onChange={e => handleTestCaseChange(idx, "expected", e.target.value)}
                required
              />
            </div>
          ))}
          <button type="button" onClick={handleAddTestCase}>Add Test Case</button>
        </div>
        <button type="submit">Save Problem</button>
      </form>
      {message && <div style={{ marginTop: 16 }}>{message}</div>}
    </div>
  );
}

export default Admin;