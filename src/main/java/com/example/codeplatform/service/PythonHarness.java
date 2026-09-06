package com.example.codeplatform.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.codeplatform.model.TestCase;

/**
 * Builds the Python script sent to the execution service and parses its output back into per-test
 * results.
 *
 * <p>Each case is preceded by a boundary marker on its own line. Splitting on that marker is what
 * keeps results aligned when a case returns a multi-line value - naively matching the i-th output
 * line to the i-th test case silently shifts every later result.
 */
public final class PythonHarness {

    /** Unlikely to collide with anything a candidate's solution prints. */
    public static final String CASE_MARKER = "__CASE_BOUNDARY_9f3a__";
    public static final String ERROR_MARKER = "__CASE_ERROR_9f3a__";

    private static final Pattern PRINT_CALL = Pattern.compile("(?<![\\w.])print\\s*\\(");
    private static final Pattern FUNCTION_NAME = Pattern.compile("^\\s*def\\s+([A-Za-z_]\\w*)\\s*\\(");
    private static final Pattern CASE_SPLIT = Pattern.compile(Pattern.quote(CASE_MARKER) + "\\R");

    private PythonHarness() {
    }

    /** Extracts {@code add} from {@code "def add(a, b):"}, or {@code null} if the signature is unusable. */
    public static String functionName(String signature) {
        if (signature == null) {
            return null;
        }
        Matcher m = FUNCTION_NAME.matcher(signature);
        return m.find() ? m.group(1) : null;
    }

    /**
     * Reports whether the submitted body calls {@code print}, ignoring {@code #} comments.
     *
     * <p>Checked before execution so an invalid submission does not consume execution-API quota.
     */
    public static boolean callsPrint(String functionBody) {
        if (functionBody == null) {
            return false;
        }
        for (String line : functionBody.split("\\R")) {
            if (PRINT_CALL.matcher(stripComment(line)).find()) {
                return true;
            }
        }
        return false;
    }

    /** Drops a trailing {@code #} comment, respecting quotes so a {@code #} inside a string survives. */
    private static String stripComment(String line) {
        boolean inSingle = false;
        boolean inDouble = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\\') {
                i++; // skip the escaped character
            } else if (c == '\'' && !inDouble) {
                inSingle = !inSingle;
            } else if (c == '"' && !inSingle) {
                inDouble = !inDouble;
            } else if (c == '#' && !inSingle && !inDouble) {
                return line.substring(0, i);
            }
        }
        return line;
    }

    /**
     * Assembles the runnable script.
     *
     * @param signature    the problem's {@code def ...:} line
     * @param functionBody the indented body the candidate wrote
     * @param functionName the name declared by {@code signature}
     * @param testCases    inputs, each stored as a comma-separated argument list (e.g. {@code "2,3"})
     */
    public static String buildScript(String signature, String functionBody, String functionName,
                                     List<TestCase> testCases) {
        StringBuilder script = new StringBuilder();
        script.append(signature.stripTrailing()).append('\n');
        script.append(functionBody == null || functionBody.isBlank() ? "    pass" : functionBody.stripTrailing())
              .append("\n\n");

        script.append("__CASES__ = [\n");
        for (TestCase tc : testCases) {
            String args = tc.getInput() == null ? "" : tc.getInput().trim();
            // Wrapping in a list and splatting keeps a single list argument (e.g. "[1,2,3]")
            // distinct from two scalar arguments (e.g. "1,2").
            script.append("    [").append(args).append("],\n");
        }
        script.append("]\n\n");
        script.append("for __args in __CASES__:\n");
        script.append("    print(\"").append(CASE_MARKER).append("\")\n");
        script.append("    try:\n");
        script.append("        print(").append(functionName).append("(*__args))\n");
        script.append("    except Exception as __exc:\n");
        script.append("        print(\"").append(ERROR_MARKER)
              .append("\" + type(__exc).__name__ + \": \" + str(__exc))\n");
        return script.toString();
    }

    /**
     * Splits raw stdout into one block per test case.
     *
     * @return a list of exactly {@code expectedCount} entries; an entry is {@code null} when the
     *         script died before reaching that case.
     */
    public static List<String> splitOutput(String rawOutput, int expectedCount) {
        List<String> blocks = new ArrayList<>(expectedCount);
        if (rawOutput != null) {
            String[] parts = CASE_SPLIT.split(rawOutput, -1);
            // parts[0] is whatever preceded the first marker (normally empty).
            for (int i = 1; i < parts.length && blocks.size() < expectedCount; i++) {
                blocks.add(parts[i].strip());
            }
        }
        while (blocks.size() < expectedCount) {
            blocks.add(null);
        }
        return blocks;
    }

    /** Strips a single layer of matching quotes so {@code "5"} and {@code 5} compare equal. */
    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String t = value.strip();
        if (t.length() >= 2
                && ((t.startsWith("\"") && t.endsWith("\"")) || (t.startsWith("'") && t.endsWith("'")))) {
            return t.substring(1, t.length() - 1);
        }
        return t;
    }
}
