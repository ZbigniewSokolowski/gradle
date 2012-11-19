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

import org.apache.commons.io.IOUtils;
import org.gradle.api.tasks.testing.*;
import org.gradle.internal.UncheckedException;
import org.gradle.util.TextUtil;

import java.io.*;
import java.util.*;

/**
 * by Szczepan Faber, created at: 11/13/12
 */
public class TestReportDataCollector implements TestListener, TestOutputListener, TestResultsProvider {

    private final Map<String, TestClassResult> tests = new HashMap<String, TestClassResult>();
    private final File resultsDir;
    private CachingFileWriter cachingFileWriter = new CachingFileWriter();

    public TestReportDataCollector(File resultsDir) {
        this.resultsDir = resultsDir;
    }

    public void beforeSuite(TestDescriptor suite) {
    }

    public void afterSuite(TestDescriptor suite, TestResult result) {
        TestClassResult classResult = tests.get(suite.getClassName());
        if (classResult != null) {
            classResult.setEndTime(result.getEndTime());
        }
        if (suite.getParent() == null) {
            cachingFileWriter.closeAll();
        }
    }

    public void beforeTest(TestDescriptor testDescriptor) {
    }

    public void afterTest(TestDescriptor testDescriptor, TestResult result) {
        if (!testDescriptor.isComposite()) {
            TestMethodResult methodResult = new TestMethodResult(testDescriptor.getName(), result);
            TestClassResult classResult = tests.get(testDescriptor.getClassName());
            if (classResult == null) {
                classResult = new TestClassResult(result.getStartTime());
                tests.put(testDescriptor.getClassName(), classResult);
            }

            classResult.add(methodResult);
        }
    }

    public void onOutput(TestDescriptor testDescriptor, TestOutputEvent outputEvent) {
        String className = testDescriptor.getClassName();
        if (className == null) {
            //this means that we receive an output before even starting any class.
            //we don't have a place for such output in any of the reports so skipping.
            return;
        }
        //the format is optimized for the junit xml results
        String message = TextUtil.escapeCDATA(outputEvent.getMessage());
        cachingFileWriter.write(outputsFile(className, outputEvent.getDestination()), message.getBytes());
    }

    private File outputsFile(String className, TestOutputEvent.Destination destination) {
        return destination == TestOutputEvent.Destination.StdOut? standardOutputFile(className) : standardErrorFile(className);
    }

    private File standardErrorFile(String className) {
        return new File(resultsDir, className + ".stderr.bin");
    }

    private File standardOutputFile(String className) {
        return new File(resultsDir, className + ".stdout.bin");
    }

    public void provideOutputs(String className, TestOutputEvent.Destination destination, Writer writer) {
        File file = outputsFile(className, destination);
        if (!file.exists()) {
            return; //test has no outputs
        }
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            IOUtils.copy(in, writer);
        } catch (IOException e) {
            throw UncheckedException.throwAsUncheckedException(e);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    public Map<String, TestClassResult> provideResults() {
        return tests;
    }
}