package com.example.codeplatform.service;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.example.codeplatform.model.TestCase;

class PythonHarnessTest {

    private static TestCase testCase(String input, String expected) {
        TestCase tc = new TestCase();
        tc.setInput(input);
        tc.setExpected(expected);
        return tc;
    }

    @Test
    void extractsFunctionName() {
        assertThat(PythonHarness.functionName("def add(a, b):")).isEqualTo("add");
        assertThat(PythonHarness.functionName("  def  two_sum (nums, target):")).isEqualTo("two_sum");
        assertThat(PythonHarness.functionName("not a signature")).isNull();
        assertThat(PythonHarness.functionName(null)).isNull();
    }

    @Test
    void detectsPrintCalls() {
        assertThat(PythonHarness.callsPrint("    print(x)")).isTrue();
        assertThat(PythonHarness.callsPrint("    print (x)")).isTrue();
    }

    @Test
    void ignoresPrintInCommentsAndLookalikeNames() {
        assertThat(PythonHarness.callsPrint("    # print(x)")).isFalse();
        assertThat(PythonHarness.callsPrint("    return pprint(x)")).isFalse();
        assertThat(PythonHarness.callsPrint("    return obj.print(x)")).isFalse();
        assertThat(PythonHarness.callsPrint("    return '#'  # print(x)")).isFalse();
        assertThat(PythonHarness.callsPrint(null)).isFalse();
    }

    @Test
    void buildsScriptThatSplatsArgumentsPerCase() {
        String script = PythonHarness.buildScript(
                "def add(a, b):", "    return a + b", "add",
                List.of(testCase("2,3", "5"), testCase("4,5", "9")));

        assertThat(script).contains("def add(a, b):", "    return a + b");
        assertThat(script).contains("    [2,3],", "    [4,5],");
        assertThat(script).contains("print(add(*__args))");
    }

    @Test
    void keepsResultsAlignedWhenACaseReturnsMultipleLines() {
        String raw = String.join("\n",
                PythonHarness.CASE_MARKER,
                "line one",
                "line two",
                PythonHarness.CASE_MARKER,
                "9",
                "");

        List<String> blocks = PythonHarness.splitOutput(raw, 2);

        assertThat(blocks).containsExactly("line one\nline two", "9");
    }

    @Test
    void padsWithNullWhenTheScriptDiedEarly() {
        String raw = PythonHarness.CASE_MARKER + "\n5\n";

        assertThat(PythonHarness.splitOutput(raw, 3)).containsExactly("5", null, null);
        assertThat(PythonHarness.splitOutput(null, 2)).containsExactly(null, null);
    }

    @Test
    void normalizeStripsOneLayerOfMatchingQuotes() {
        assertThat(PythonHarness.normalize("\"hello\"")).isEqualTo("hello");
        assertThat(PythonHarness.normalize("'hello'")).isEqualTo("hello");
        assertThat(PythonHarness.normalize("  5 ")).isEqualTo("5");
        assertThat(PythonHarness.normalize("\"")).isEqualTo("\"");
        assertThat(PythonHarness.normalize(null)).isNull();
    }
}
