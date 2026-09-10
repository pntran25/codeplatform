import { render, screen, waitFor } from "@testing-library/react";
import App from "./App";
import api from "./axios";

jest.mock("./axios", () => ({
  __esModule: true,
  default: { get: jest.fn(), post: jest.fn(), put: jest.fn(), delete: jest.fn() },
  errorMessage: (_err, fallback) => fallback,
}));

// Monaco pulls in web workers that jsdom cannot run.
jest.mock("@monaco-editor/react", () => ({
  __esModule: true,
  default: () => <div data-testid="editor" />,
}));

beforeEach(() => {
  localStorage.clear();
  jest.clearAllMocks();
  window.history.pushState({}, "", "/");
});

test("renders the landing page", () => {
  render(<App />);
  // The wordmark also appears in the header and footer, so pin this to the hero heading.
  expect(screen.getByRole("heading", { level: 1, name: "CODEXA" })).toBeInTheDocument();
});

test("lists problems returned by the API", async () => {
  api.get.mockResolvedValue({ data: [{ id: 1, title: "Two Sum" }] });
  window.history.pushState({}, "", "/problems");

  render(<App />);

  expect(await screen.findByText("Two Sum")).toBeInTheDocument();
});

test("shows an error instead of hanging when the problem list fails to load", async () => {
  api.get.mockRejectedValue(new Error("network"));
  window.history.pushState({}, "", "/problems");

  render(<App />);

  await waitFor(() => expect(screen.getByText(/could not load problems/i)).toBeInTheDocument());
});

test("shows a not-found message for a missing problem rather than loading forever", async () => {
  api.get.mockRejectedValue({ response: { status: 404 } });
  window.history.pushState({}, "", "/problems/999");

  render(<App />);

  await waitFor(() =>
    expect(screen.getByText(/that problem does not exist/i)).toBeInTheDocument()
  );
});

test("prompts anonymous visitors to log in before running code", async () => {
  api.get.mockResolvedValue({
    data: { id: 1, title: "Add", description: "d", functionSignature: "def add(a, b):", testCases: [] },
  });
  window.history.pushState({}, "", "/problems/1");

  render(<App />);

  const runButton = await screen.findByRole("button", { name: /log in to run code/i });
  expect(runButton).toBeDisabled();
});
