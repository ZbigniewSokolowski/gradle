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
import org.gradle.api.internal.tasks.testing.junit.binary.TestClassResult
import org.gradle.api.internal.tasks.testing.junit.binary.TestResultsProvider
import org.gradle.api.tasks.testing.TestOutputEvent
import org.gradle.api.internal.tasks.testing.junit.binary.TestMethodResult
import org.gradle.api.internal.tasks.testing.results.DefaultTestResult
import org.gradle.api.tasks.testing.TestResult

import static java.util.Collections.emptyList
import static java.util.Arrays.asList
import javax.xml.stream.XMLOutputFactory

/**
 * by Szczepan Faber, created at: 11/16/12
 */
class SaxJUnitXmlResultWriterTest extends Specification {

    def provider = Mock(TestResultsProvider)
    def generator = new SaxJUnitXmlResultWriter("localhost", provider, XMLOutputFactory.newFactory())

    def "test"() {
        StringWriter sw = new StringWriter()
        TestClassResult result = new TestClassResult(System.currentTimeMillis())
        result.add(new TestMethodResult("some test", new DefaultTestResult(TestResult.ResultType.SUCCESS, 10, 25, 1, 1, 0, emptyList())))
        result.add(new TestMethodResult("some failing test", new DefaultTestResult(TestResult.ResultType.FAILURE, 15, 25, 1, 0, 1, asList(new RuntimeException("Boo! ]]> cdata check!")))))

        provider.provideOutputs("com.foo.FooTest", TestOutputEvent.Destination.StdOut, sw) >> { args ->
            sw.write("1st output message\n")
            sw.write("2nd output message\n")
            sw.write(args[2].transform("cdata check: ]]> end\n"))
        }
        provider.provideOutputs("com.foo.FooTest", TestOutputEvent.Destination.StdErr, sw) >> { /* no std err */}

        when:
        generator.write("com.foo.FooTest", result, sw)

        then:
        println sw
    }
}
