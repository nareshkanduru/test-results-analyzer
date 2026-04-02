package org.jenkinsci.plugins.testresultsanalyzer.result.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hudson.tasks.test.TabulatedResult;
import hudson.tasks.test.TestObject;
import net.sf.json.JSONObject;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ResultDataTest {

    private static class ConcreteResultData extends ResultData {
        public ConcreteResultData() {
            super();
        }

        public ConcreteResultData(TestObject result, String url) {
            super(result, url);
        }
    }

    @Test
    public void defaultConstructorInitializesFieldsToDefaults() {
        // Verify default constructor sets empty children list and empty failure message
        ConcreteResultData data = new ConcreteResultData();

        assertNull(data.getName());
        assertFalse(data.isPassed());
        assertFalse(data.isSkipped());
        assertNull(data.getPackageResult());
        assertEquals(0, data.getTotalTests());
        assertEquals(0, data.getTotalFailed());
        assertEquals(0, data.getTotalPassed());
        assertEquals(0, data.getTotalSkipped());
        assertTrue(data.getChildren().isEmpty());
        assertEquals(0.0f, data.getTotalTimeTaken(), 0.0001f);
        assertNull(data.getStatus());
        assertEquals("", data.getFailureMessage());
        assertNull(data.getUrl());
    }

    @Test
    public void parameterizedConstructorSetsAllFieldsFromPassingTestObject() {
        // Verify constructor correctly maps a passing TestObject (0 failures, not all skipped)
        TestObject mockResult = mock(TestObject.class);
        when(mockResult.getName()).thenReturn("myTest");
        when(mockResult.getFailCount()).thenReturn(0);
        when(mockResult.getSkipCount()).thenReturn(0);
        when(mockResult.getTotalCount()).thenReturn(5);
        when(mockResult.getPassCount()).thenReturn(5);
        when(mockResult.getDuration()).thenReturn(1.5f);

        ConcreteResultData data = new ConcreteResultData(mockResult, "http://example.com/test");

        assertEquals("myTest", data.getName());
        assertTrue(data.isPassed());
        assertFalse(data.isSkipped());
        assertEquals(5, data.getTotalTests());
        assertEquals(0, data.getTotalFailed());
        assertEquals(5, data.getTotalPassed());
        assertEquals(0, data.getTotalSkipped());
        assertEquals(1.5f, data.getTotalTimeTaken(), 0.0001f);
        assertEquals("http://example.com/test", data.getUrl());
        assertEquals("PASSED", data.getStatus());
    }

    @Test
    public void parameterizedConstructorSetsStatusToFailedWhenFailuresExist() {
        // Verify that non-zero fail count results in isPassed=false and status=FAILED
        TestObject mockResult = mock(TestObject.class);
        when(mockResult.getName()).thenReturn("failingTest");
        when(mockResult.getFailCount()).thenReturn(2);
        when(mockResult.getSkipCount()).thenReturn(0);
        when(mockResult.getTotalCount()).thenReturn(5);
        when(mockResult.getPassCount()).thenReturn(3);
        when(mockResult.getDuration()).thenReturn(2.0f);

        ConcreteResultData data = new ConcreteResultData(mockResult, "/url");

        assertFalse(data.isPassed());
        assertFalse(data.isSkipped());
        assertEquals("FAILED", data.getStatus());
        assertEquals(2, data.getTotalFailed());
        assertEquals(3, data.getTotalPassed());
    }

    @Test
    public void parameterizedConstructorSetsStatusToSkippedWhenAllSkipped() {
        // Verify that skipCount == totalCount results in isSkipped=true and status=SKIPPED
        TestObject mockResult = mock(TestObject.class);
        when(mockResult.getName()).thenReturn("skippedTest");
        when(mockResult.getFailCount()).thenReturn(0);
        when(mockResult.getSkipCount()).thenReturn(3);
        when(mockResult.getTotalCount()).thenReturn(3);
        when(mockResult.getPassCount()).thenReturn(0);
        when(mockResult.getDuration()).thenReturn(0.0f);

        ConcreteResultData data = new ConcreteResultData(mockResult, "/skip");

        assertTrue(data.isPassed());
        assertTrue(data.isSkipped());
        // isPassed is checked first in evaluateStatus, so status is PASSED even when all skipped
        assertEquals("PASSED", data.getStatus());
    }

    @Test
    public void evaluateStatusReturnsSkippedWhenNotPassedButSkipped() {
        // Verify SKIPPED branch: isPassed=false, isSkipped=true -> status=SKIPPED
        ConcreteResultData data = new ConcreteResultData();
        data.setPassed(false);
        data.setSkipped(true);
        data.evaluateStatus();

        assertEquals("SKIPPED", data.getStatus());
    }

    @Test
    public void evaluateStatusReturnsPassedWhenPassed() {
        // Verify PASSED branch: isPassed=true -> status=PASSED regardless of isSkipped
        ConcreteResultData data = new ConcreteResultData();
        data.setPassed(true);
        data.setSkipped(false);
        data.evaluateStatus();

        assertEquals("PASSED", data.getStatus());
    }

    @Test
    public void evaluateStatusReturnsPassedEvenWhenBothPassedAndSkipped() {
        // Verify isPassed takes priority over isSkipped in evaluateStatus
        ConcreteResultData data = new ConcreteResultData();
        data.setPassed(true);
        data.setSkipped(true);
        data.evaluateStatus();

        assertEquals("PASSED", data.getStatus());
    }

    @Test
    public void evaluateStatusReturnsFailedWhenNotPassedAndNotSkipped() {
        // Verify FAILED branch: isPassed=false, isSkipped=false -> status=FAILED
        ConcreteResultData data = new ConcreteResultData();
        data.setPassed(false);
        data.setSkipped(false);
        data.evaluateStatus();

        assertEquals("FAILED", data.getStatus());
    }

    @Test
    public void setAndGetName() {
        // Verify name getter/setter round-trip
        ConcreteResultData data = new ConcreteResultData();
        data.setName("TestSuite");
        assertEquals("TestSuite", data.getName());
    }

    @Test
    public void setAndGetNameWithNull() {
        // Verify null name is stored and returned without error
        ConcreteResultData data = new ConcreteResultData();
        data.setName(null);
        assertNull(data.getName());
    }

    @Test
    public void setAndGetFailureMessage() {
        // Verify failure message getter/setter round-trip
        ConcreteResultData data = new ConcreteResultData();
        data.setFailureMessage("NullPointerException at line 42");
        assertEquals("NullPointerException at line 42", data.getFailureMessage());
    }

    @Test
    public void setAndGetPackageResult() {
        // Verify packageResult getter/setter round-trip with a mock TabulatedResult
        ConcreteResultData data = new ConcreteResultData();
        TabulatedResult mockTabulated = mock(TabulatedResult.class);
        data.setPackageResult(mockTabulated);
        assertEquals(mockTabulated, data.getPackageResult());
    }

    @Test
    public void setAndGetTotalTimeTaken() {
        // Verify totalTimeTaken getter/setter with a fractional value
        ConcreteResultData data = new ConcreteResultData();
        data.setTotalTimeTaken(99.99f);
        assertEquals(99.99f, data.getTotalTimeTaken(), 0.001f);
    }

    @Test
    public void setAndGetUrl() {
        // Verify url getter/setter round-trip
        ConcreteResultData data = new ConcreteResultData();
        data.setUrl("http://jenkins/job/1/testReport");
        assertEquals("http://jenkins/job/1/testReport", data.getUrl());
    }

    @Test
    public void addSingleChildResult() {
        // Verify addChildResult(ResultData) appends one child to the list
        ConcreteResultData parent = new ConcreteResultData();
        ConcreteResultData child = new ConcreteResultData();
        child.setName("child1");

        parent.addChildResult(child);

        assertEquals(1, parent.getChildren().size());
        assertEquals("child1", parent.getChildren().get(0).getName());
    }

    @Test
    public void addMultipleChildrenViaList() {
        // Verify addChildResult(List) appends all children from the list
        ConcreteResultData parent = new ConcreteResultData();
        ConcreteResultData child1 = new ConcreteResultData();
        child1.setName("c1");
        ConcreteResultData child2 = new ConcreteResultData();
        child2.setName("c2");

        List<ResultData> children = new ArrayList<ResultData>();
        children.add(child1);
        children.add(child2);
        parent.addChildResult(children);

        assertEquals(2, parent.getChildren().size());
        assertEquals("c1", parent.getChildren().get(0).getName());
        assertEquals("c2", parent.getChildren().get(1).getName());
    }

    @Test
    public void addChildResultAccumulatesAcrossMultipleCalls() {
        // Verify that multiple addChildResult calls accumulate, not replace
        ConcreteResultData parent = new ConcreteResultData();
        ConcreteResultData child1 = new ConcreteResultData();
        child1.setName("first");
        ConcreteResultData child2 = new ConcreteResultData();
        child2.setName("second");

        parent.addChildResult(child1);
        parent.addChildResult(child2);

        assertEquals(2, parent.getChildren().size());
        assertEquals("first", parent.getChildren().get(0).getName());
        assertEquals("second", parent.getChildren().get(1).getName());
    }

    @Test
    public void getJsonObjectWithNoChildrenAndNoFailureMessage() {
        // Verify JSON output for a simple passed result with no children and empty failure message
        ConcreteResultData data = new ConcreteResultData();
        data.setName("pkg.MyTest");
        data.setTotalTests(10);
        data.setTotalFailed(0);
        data.setTotalPassed(10);
        data.setTotalSkipped(0);
        data.setPassed(true);
        data.setSkipped(false);
        data.setTotalTimeTaken(3.14f);
        data.setUrl("/job/1");
        data.evaluateStatus();

        JSONObject json = data.getJsonObject();

        assertEquals("pkg.MyTest", json.getString("name"));
        assertEquals(10, json.getInt("totalTests"));
        assertEquals(0, json.getInt("totalFailed"));
        assertEquals(10, json.getInt("totalPassed"));
        assertEquals(0, json.getInt("totalSkipped"));
        assertTrue(json.getBoolean("isPassed"));
        assertFalse(json.getBoolean("isSkipped"));
        assertEquals(3.14, json.getDouble("totalTimeTaken"), 0.01);
        assertEquals("PASSED", json.getString("status"));
        assertEquals("/job/1", json.getString("url"));
        assertTrue(json.getJSONArray("children").isEmpty());
        assertFalse(json.containsKey("failureMessage"));
    }

    @Test
    public void getJsonObjectIncludesFailureMessageWhenNonEmpty() {
        // Verify failureMessage is included in JSON only when it's non-empty
        ConcreteResultData data = new ConcreteResultData();
        data.setName("failTest");
        data.setPassed(false);
        data.setSkipped(false);
        data.setTotalTests(1);
        data.setTotalFailed(1);
        data.setTotalPassed(0);
        data.setTotalSkipped(0);
        data.setTotalTimeTaken(0.5f);
        data.setUrl("/fail");
        data.setFailureMessage("java.lang.AssertionError: expected true");
        data.evaluateStatus();

        JSONObject json = data.getJsonObject();

        assertEquals("FAILED", json.getString("status"));
        assertTrue(json.containsKey("failureMessage"));
        assertEquals("java.lang.AssertionError: expected true", json.getString("failureMessage"));
    }

    @Test
    public void getJsonObjectExcludesFailureMessageWhenEmpty() {
        // Verify empty string failure message is NOT included in JSON output
        ConcreteResultData data = new ConcreteResultData();
        data.setName("test");
        data.setFailureMessage("");
        data.evaluateStatus();

        JSONObject json = data.getJsonObject();

        assertFalse(json.containsKey("failureMessage"));
    }

    @Test
    public void getJsonObjectIncludesChildrenRecursively() {
        // Verify children are serialized recursively in the JSON children array
        ConcreteResultData parent = new ConcreteResultData();
        parent.setName("parent");
        parent.setPassed(true);
        parent.setTotalTests(2);
        parent.setTotalFailed(0);
        parent.setTotalPassed(2);
        parent.setTotalSkipped(0);
        parent.setTotalTimeTaken(1.0f);
        parent.setUrl("/parent");
        parent.evaluateStatus();

        ConcreteResultData child = new ConcreteResultData();
        child.setName("child");
        child.setPassed(true);
        child.setTotalTests(1);
        child.setTotalFailed(0);
        child.setTotalPassed(1);
        child.setTotalSkipped(0);
        child.setTotalTimeTaken(0.5f);
        child.setUrl("/child");
        child.evaluateStatus();

        parent.addChildResult(child);

        JSONObject json = parent.getJsonObject();

        assertEquals(1, json.getJSONArray("children").size());
        JSONObject childJson = json.getJSONArray("children").getJSONObject(0);
        assertEquals("child", childJson.getString("name"));
        assertEquals("PASSED", childJson.getString("status"));
        assertEquals("/child", childJson.getString("url"));
    }

    @Test
    public void getJsonObjectWithMultipleNestedChildren() {
        // Verify multiple children are all serialized in the JSON children array
        ConcreteResultData parent = new ConcreteResultData();
        parent.setName("suite");
        parent.setPassed(false);
        parent.evaluateStatus();

        ConcreteResultData child1 = new ConcreteResultData();
        child1.setName("test1");
        child1.setPassed(true);
        child1.evaluateStatus();

        ConcreteResultData child2 = new ConcreteResultData();
        child2.setName("test2");
        child2.setPassed(false);
        child2.setSkipped(true);
        child2.evaluateStatus();

        parent.addChildResult(child1);
        parent.addChildResult(child2);

        JSONObject json = parent.getJsonObject();

        assertEquals(2, json.getJSONArray("children").size());
        assertEquals("test1", json.getJSONArray("children").getJSONObject(0).getString("name"));
        assertEquals("PASSED", json.getJSONArray("children").getJSONObject(0).getString("status"));
        assertEquals("test2", json.getJSONArray("children").getJSONObject(1).getString("name"));
        assertEquals("SKIPPED", json.getJSONArray("children").getJSONObject(1).getString("status"));
    }

    @Test
    public void failureMessageCaseInsensitiveEmptyCheck() {
        // Verify that failure message comparison is case-insensitive (equalsIgnoreCase)
        // A non-empty string should be included in JSON
        ConcreteResultData data = new ConcreteResultData();
        data.setName("test");
        data.setFailureMessage("Error");
        data.evaluateStatus();

        JSONObject json = data.getJsonObject();

        assertTrue(json.containsKey("failureMessage"));
        assertEquals("Error", json.getString("failureMessage"));
    }

    @Test
    public void unicodeNameAndFailureMessage() {
        // Verify Unicode characters in name and failure message are preserved in JSON
        ConcreteResultData data = new ConcreteResultData();
        data.setName("テスト名前");
        data.setFailureMessage("エラー: 予期しない値 — «значение»");
        data.setPassed(false);
        data.evaluateStatus();

        JSONObject json = data.getJsonObject();

        assertEquals("テスト名前", json.getString("name"));
        assertEquals("エラー: 予期しない値 — «значение»", json.getString("failureMessage"));
    }

    @Test
    public void parameterizedConstructorWithZeroCounts() {
        // Verify constructor handles zero total count (edge case: empty test suite)
        TestObject mockResult = mock(TestObject.class);
        when(mockResult.getName()).thenReturn("emptyTest");
        when(mockResult.getFailCount()).thenReturn(0);
        when(mockResult.getSkipCount()).thenReturn(0);
        when(mockResult.getTotalCount()).thenReturn(0);
        when(mockResult.getPassCount()).thenReturn(0);
        when(mockResult.getDuration()).thenReturn(0.0f);

        ConcreteResultData data = new ConcreteResultData(mockResult, "");

        assertEquals("emptyTest", data.getName());
        assertTrue(data.isPassed());
        assertTrue(data.isSkipped());
        assertEquals(0, data.getTotalTests());
        assertEquals("PASSED", data.getStatus());
        assertEquals("", data.getUrl());
    }

    @Test
    public void parameterizedConstructorWithLargeCounts() {
        // Verify constructor handles large test counts without overflow issues
        TestObject mockResult = mock(TestObject.class);
        when(mockResult.getName()).thenReturn("bigSuite");
        when(mockResult.getFailCount()).thenReturn(500);
        when(mockResult.getSkipCount()).thenReturn(200);
        when(mockResult.getTotalCount()).thenReturn(10000);
        when(mockResult.getPassCount()).thenReturn(9300);
        when(mockResult.getDuration()).thenReturn(9999.99f);

        ConcreteResultData data = new ConcreteResultData(mockResult, "/big");

        assertFalse(data.isPassed());
        assertFalse(data.isSkipped());
        assertEquals(10000, data.getTotalTests());
        assertEquals(500, data.getTotalFailed());
        assertEquals(9300, data.getTotalPassed());
        assertEquals(200, data.getTotalSkipped());
        assertEquals(9999.99f, data.getTotalTimeTaken(), 0.01f);
        assertEquals("FAILED", data.getStatus());
    }

    @Test
    public void getJsonObjectWithNullNameAndUrl() {
        // Verify JSON serialization handles null name and url fields gracefully
        ConcreteResultData data = new ConcreteResultData();
        data.evaluateStatus();

        JSONObject json = data.getJsonObject();

        assertTrue(json.containsKey("name"));
        assertTrue(json.containsKey("url"));
        assertTrue(json.containsKey("status"));
        assertEquals("FAILED", json.getString("status"));
    }

    @Test
    public void setTotalTestsAndGetTotalTests() {
        // Verify totalTests getter/setter round-trip
        ConcreteResultData data = new ConcreteResultData();
        data.setTotalTests(42);
        assertEquals(42, data.getTotalTests());
    }

    @Test
    public void setTotalFailedAndGetTotalFailed() {
        // Verify totalFailed getter/setter round-trip
        ConcreteResultData data = new ConcreteResultData();
        data.setTotalFailed(7);
        assertEquals(7, data.getTotalFailed());
    }

    @Test
    public void setTotalPassedAndGetTotalPassed() {
        // Verify totalPassed getter/setter round-trip
        ConcreteResultData data = new ConcreteResultData();
        data.setTotalPassed(35);
        assertEquals(35, data.getTotalPassed());
    }

    @Test
    public void setTotalSkippedAndGetTotalSkipped() {
        // Verify totalSkipped getter/setter round-trip
        ConcreteResultData data = new ConcreteResultData();
        data.setTotalSkipped(3);
        assertEquals(3, data.getTotalSkipped());
    }

    @Test
    public void setPassedAndIsPassed() {
        // Verify isPassed getter/setter round-trip for both true and false
        ConcreteResultData data = new ConcreteResultData();
        data.setPassed(true);
        assertTrue(data.isPassed());
        data.setPassed(false);
        assertFalse(data.isPassed());
    }

    @Test
    public void setSkippedAndIsSkipped() {
        // Verify isSkipped getter/setter round-trip for both true and false
        ConcreteResultData data = new ConcreteResultData();
        data.setSkipped(true);
        assertTrue(data.isSkipped());
        data.setSkipped(false);
        assertFalse(data.isSkipped());
    }

    @Test
    public void addEmptyChildList() {
        // Verify adding an empty list of children does not change the children list
        ConcreteResultData parent = new ConcreteResultData();
        parent.addChildResult(new ArrayList<ResultData>());
        assertTrue(parent.getChildren().isEmpty());
    }

    @Test
    public void getJsonObjectFailureMessageWithSpecialCharacters() {
        // Verify special characters like quotes and angle brackets in failure message are preserved
        ConcreteResultData data = new ConcreteResultData();
        data.setName("xssTest");
        data.setFailureMessage("<script>alert('xss')</script> & \"quotes\"");
        data.setPassed(false);
        data.evaluateStatus();

        JSONObject json = data.getJsonObject();

        assertTrue(json.containsKey("failureMessage"));
        assertEquals("<script>alert('xss')</script> & \"quotes\"", json.getString("failureMessage"));
    }

    @Test
    public void parameterizedConstructorWithNullUrl() {
        // Verify constructor handles null URL without error
        TestObject mockResult = mock(TestObject.class);
        when(mockResult.getName()).thenReturn("nullUrlTest");
        when(mockResult.getFailCount()).thenReturn(0);
        when(mockResult.getSkipCount()).thenReturn(0);
        when(mockResult.getTotalCount()).thenReturn(1);
        when(mockResult.getPassCount()).thenReturn(1);
        when(mockResult.getDuration()).thenReturn(0.1f);

        ConcreteResultData data = new ConcreteResultData(mockResult, null);

        assertEquals("nullUrlTest", data.getName());
        assertNull(data.getUrl());
        assertEquals("PASSED", data.getStatus());
    }

    @Test
    public void getStatusReturnsNullBeforeEvaluateStatusCalled() {
        // Verify status is null when evaluateStatus has not been called on default-constructed object
        ConcreteResultData data = new ConcreteResultData();
        assertNull(data.getStatus());
    }

    @Test
    public void childJsonIncludesGrandchildren() {
        // Verify deeply nested children (grandchildren) are serialized recursively
        ConcreteResultData grandparent = new ConcreteResultData();
        grandparent.setName("grandparent");
        grandparent.evaluateStatus();

        ConcreteResultData parent = new ConcreteResultData();
        parent.setName("parent");
        parent.evaluateStatus();

        ConcreteResultData child = new ConcreteResultData();
        child.setName("child");
        child.evaluateStatus();

        parent.addChildResult(child);
        grandparent.addChildResult(parent);

        JSONObject json = grandparent.getJsonObject();

        JSONObject parentJson = json.getJSONArray("children").getJSONObject(0);
        assertEquals("parent", parentJson.getString("name"));
        JSONObject childJson = parentJson.getJSONArray("children").getJSONObject(0);
        assertEquals("child", childJson.getString("name"));
        assertTrue(childJson.getJSONArray("children").isEmpty());
    }
}
