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

package org.gradle.api.internal.tasks.testing.junit.binary;

import org.gradle.api.tasks.testing.TestResult;

import java.util.HashSet;
import java.util.Set;

/**
 * by Szczepan Faber, created at: 11/13/12
 */
public class TestClassResult {

    Set<TestMethodResult> methodResults = new HashSet<TestMethodResult>();
    private final long startTime;
    private long endTime;
    private int failuresCount;

    public TestClassResult(long startTime) {
        this.startTime = startTime;
    }

    public void add(TestMethodResult methodResult) {
        if (methodResult.result.getResultType() == TestResult.ResultType.FAILURE) {
            failuresCount++;
        }
        methodResults.add(methodResult);
    }

    public Set<TestMethodResult> getResults() {
        return methodResults;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public int getTestsCount() {
        return methodResults.size();
    }

    public int getFailuresCount() {
        return failuresCount;
    }

    public long getDuration() {
        return endTime - startTime;
    }
}
