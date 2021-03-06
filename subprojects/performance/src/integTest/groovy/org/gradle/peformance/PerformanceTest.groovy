/*
 * Copyright 2009 the original author or authors.
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

package org.gradle.peformance

import org.gradle.peformance.fixture.PerformanceTestRunner
import spock.lang.Specification
import spock.lang.Unroll

import static org.gradle.peformance.fixture.DataAmount.kbytes
import static org.gradle.peformance.fixture.Duration.millis

/**
 * by Szczepan Faber, created at: 2/9/12
 */
class PerformanceTest extends Specification {

    @Unroll("Project '#testProject' clean build")
    def "clean build"() {
        expect:
        def result = new PerformanceTestRunner(testProject: testProject,
                tasksToRun: ['clean', 'build'],
                runs: runs,
                warmUpRuns: 1,
                targetVersions: ['1.0', 'last'],
                maxExecutionTimeRegression: maxExecutionTimeRegression,
                maxMemoryRegression: [kbytes(1400), kbytes(1400)]
        ).run()
        result.assertCurrentVersionHasNotRegressed()

        where:
        testProject       | runs | maxExecutionTimeRegression
                                   //max regression for 1.0 is very generous
        "small"           | 5    | [millis(50000), millis(500)]
        "multi"           | 5    | [millis(50000), millis(1000)]
        "lotDependencies" | 5    | [millis(50000), millis(1000)]
    }

    @Unroll("Project '#testProject' up-to-date build")
    def "build"() {
        expect:
        def result = new PerformanceTestRunner(testProject: testProject,
                tasksToRun: ['build'],
                runs: runs,
                warmUpRuns: 1,
                maxExecutionTimeRegression: [maxExecutionTimeRegression],
                maxMemoryRegression: [kbytes(1400)]
        ).run()
        result.assertCurrentVersionHasNotRegressed()

        where:
        testProject       | runs | maxExecutionTimeRegression
        "small"           | 5    | millis(500)
        "multi"           | 5    | millis(1000)
        "lotDependencies" | 5    | millis(1000)
    }

    @Unroll("Project '#testProject' dependency report")
    def "dependency report"() {
        expect:
        def result = new PerformanceTestRunner(testProject: testProject,
                tasksToRun: ['dependencyReport'],
                runs: runs,
                warmUpRuns: 1,
                maxExecutionTimeRegression: [maxExecutionTimeRegression],
                maxMemoryRegression: [kbytes(1400)]
        ).run()
        result.assertCurrentVersionHasNotRegressed()

        where:
        testProject       | runs | maxExecutionTimeRegression
        "lotDependencies" | 5    | millis(1000)
    }

    @Unroll("Project '#testProject' eclipse")
    def "eclipse"() {
        expect:
        def result = new PerformanceTestRunner(testProject: testProject,
                tasksToRun: ['eclipse'],
                runs: runs,
                warmUpRuns: 1,
                maxExecutionTimeRegression: [maxExecutionTimeRegression],
                maxMemoryRegression: [kbytes(1400)]
        ).run()
        result.assertCurrentVersionHasNotRegressed()

        where:
        testProject       | runs | maxExecutionTimeRegression
        "small"           | 5    | millis(500)
        "multi"           | 5    | millis(1000)
        "lotDependencies" | 5    | millis(1000)
    }

    @Unroll("Project '#testProject' idea")
    def "idea"() {
        expect:
        def result = new PerformanceTestRunner(testProject: testProject,
                tasksToRun: ['idea'],
                runs: runs,
                warmUpRuns: 1,
                maxExecutionTimeRegression: [maxExecutionTimeRegression],
                maxMemoryRegression: [kbytes(1400)]
        ).run()
        result.assertCurrentVersionHasNotRegressed()

        where:
        testProject       | runs | maxExecutionTimeRegression
        "small"           | 5    | millis(500)
        "multi"           | 5    | millis(1000)
        "lotDependencies" | 5    | millis(1000)
    }

    @Unroll("Project '#testProject'")
    def "verbose tests with report"() {
        when:
        def result = new PerformanceTestRunner(testProject: testProject,
                tasksToRun: ['cleanTest', 'test'],
                runs: runs,
                args: ['-q'],
                warmUpRuns: 1,
                targetVersions: versions,
                maxExecutionTimeRegression: maxExecutionTimeRegression,
                maxMemoryRegression: maxMemoryRegression
        ).run()

        then:
        result.assertCurrentVersionHasNotRegressed()

        where:
        testProject         | runs | versions        | maxExecutionTimeRegression   | maxMemoryRegression
        //needs to be tuned when html report is efficient
        "withTestNG"        | 4    | ['1.2', 'last'] | [millis(1200), millis(200)]  | [kbytes(0), kbytes(0)]
        "withVerboseJUnits" | 4    | ['last']        | [millis(1200)]               | [kbytes(0)]
    }
}