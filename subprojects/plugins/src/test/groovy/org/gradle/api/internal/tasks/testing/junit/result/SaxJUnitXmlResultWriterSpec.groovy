/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.tasks.testing.junit.result

import spock.lang.Specification

import org.gradle.api.tasks.testing.TestOutputEvent

import org.gradle.api.internal.tasks.testing.results.DefaultTestResult
import org.gradle.api.tasks.testing.TestResult

import static java.util.Collections.emptyList
import static java.util.Arrays.asList
import javax.xml.stream.XMLOutputFactory
import org.gradle.integtests.fixtures.JUnitTestExecutionResult
import org.gradle.integtests.fixtures.TestClassExecutionResult
import org.gradle.integtests.fixtures.JUnitTestClassExecutionResult

import static org.hamcrest.core.IsEqual.equalTo
import static org.hamcrest.Matchers.equalTo
import static org.gradle.api.tasks.testing.TestResult.ResultType.*

/**
 * by Szczepan Faber, created at: 11/16/12
 */
class SaxJUnitXmlResultWriterSpec extends Specification {

    private provider = Mock(TestResultsProvider)
    private generator = new SaxJUnitXmlResultWriter("localhost", provider, XMLOutputFactory.newFactory())

    def "writes xml JUnit result"() {
        StringWriter sw = new StringWriter()
        TestClassResult result = new TestClassResult(new Date(1353344968049).getTime())
        result.add(new TestMethodResult("some test", new DefaultTestResult(SUCCESS, 10, 25, 1, 1, 0, emptyList())))
        result.add(new TestMethodResult("some test two", new DefaultTestResult(SUCCESS, 10, 25, 1, 1, 0, emptyList())))
        result.add(new TestMethodResult("some failing test", new DefaultTestResult(FAILURE, 15, 25, 1, 0, 1, asList(new RuntimeException("Boo! ]]> cdata check!")))))
        result.add(new TestMethodResult("some skipped test", new DefaultTestResult(SKIPPED, 15, 25, 1, 0, 1, asList())))

        provider.provideOutputs("com.foo.FooTest", TestOutputEvent.Destination.StdOut, sw) >> {
            sw.write("1st output message\n")
            sw.write("2nd output message\n")
            sw.write("cdata check: ]]&gt; end\n")
        }
        provider.provideOutputs("com.foo.FooTest", TestOutputEvent.Destination.StdErr, sw) >> { sw.write("err") }

        when:
        generator.write("com.foo.FooTest", result, sw)

        then:
        def fooTest = new JUnitTestClassExecutionResult(sw.toString(), "com.foo.FooTest")
        fooTest.assertTestCount(4, 1, 0)
        fooTest.assertTestFailed("some failing test", equalTo('java.lang.RuntimeException: Boo! ]]> cdata check!'))
        fooTest.assertTestsSkipped("some skipped test")
        fooTest.assertTestsExecuted("some test", "some test two", "some failing test")
        fooTest.assertStdout(equalTo("""1st output message
2nd output message
cdata check: ]]&gt; end
"""))
        fooTest.assertStderr(equalTo("err"))


        sw.toString().startsWith """<?xml version="1.0" encoding="UTF-8"?>
  <testsuite name="com.foo.FooTest" tests="4" failures="1" errors="0" timestamp="2012-11-19T17:09:28" hostname="localhost" time="0.05">
  <properties/>
    <testcase name="some test" classname="com.foo.FooTest" time="0.015"></testcase>
    <testcase name="some test two" classname="com.foo.FooTest" time="0.015"></testcase>
    <testcase name="some failing test" classname="com.foo.FooTest" time="0.01">
      <failure message="java.lang.RuntimeException: Boo! ]]&gt; cdata check!" type="java.lang.RuntimeException"><![CDATA[java.lang.RuntimeException: Boo! ]]&gt; cdata check!"""

        sw.toString().endsWith """]]></failure></testcase>
    <ignored-testcase name="some skipped test" classname="com.foo.FooTest" time="0.01"></ignored-testcase>
  <system-out><![CDATA[1st output message
2nd output message
cdata check: ]]&gt; end
]]></system-out>
  <system-err><![CDATA[err]]></system-err>
</testsuite>"""
    }

    def "writes results with empty outputs"() {
        StringWriter sw = new StringWriter()
        TestClassResult result = new TestClassResult(new Date(1353344968049).getTime())
        result.add(new TestMethodResult("some test", new DefaultTestResult(SUCCESS, 100, 300, 1, 1, 0, emptyList())))

        when:
        generator.write("com.foo.FooTest", result, sw)

        then:
        sw.toString() == """<?xml version="1.0" encoding="UTF-8"?>
  <testsuite name="com.foo.FooTest" tests="1" failures="0" errors="0" timestamp="2012-11-19T17:09:28" hostname="localhost" time="0.2">
  <properties/>
    <testcase name="some test" classname="com.foo.FooTest" time="0.2"></testcase>
  <system-out><![CDATA[]]></system-out>
  <system-err><![CDATA[]]></system-err>
</testsuite>"""
    }

}
