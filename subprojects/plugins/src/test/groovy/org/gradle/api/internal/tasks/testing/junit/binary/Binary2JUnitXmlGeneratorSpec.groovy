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

package org.gradle.api.internal.tasks.testing.junit.binary

import spock.lang.Specification
import org.junit.Rule
import org.gradle.util.TemporaryFolder
import org.gradle.api.internal.tasks.testing.DefaultTestSuiteDescriptor

/**
 * by Szczepan Faber, created at: 11/16/12
 */
class Binary2JUnitXmlGeneratorSpec extends Specification {

    @Rule final TemporaryFolder temp = new TemporaryFolder()

    def "generates xml results"() {
        given:
        def binaryGenerator = new TestReportDataCollector(temp.createDir("bin"))
        binaryGenerator.beforeSuite(new DefaultTestSuiteDescriptor("1.1", "Gradle tests"))
        binaryGenerator.beforeSuite(new DefaultTestSuiteDescriptor("1.1", "Gradle tests"))


        def generator = new NewJUnitXmlReportGenerator()
        def xml = temp.createDir("xml")

        when:
        generator.generate(xml, binaryGenerator)

        then:
        println(xml.list())
    }
}
