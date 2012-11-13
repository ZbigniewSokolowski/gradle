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
import org.gradle.api.internal.tasks.testing.junit.result.XmlTestSuiteWriter;
import org.gradle.api.tasks.testing.*;
import org.gradle.internal.UncheckedException;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * by Szczepan Faber, created at: 11/13/12
 */
public class BinaryReportGenerator implements TestListener, TestOutputListener {

    final Map<String, BinaryTestClassResult> tests = new HashMap<String, BinaryTestClassResult>();
    private final File resultsDir;

    public BinaryReportGenerator(File resultsDir) {
        this.resultsDir = resultsDir;
    }

    public void beforeSuite(TestDescriptor suite) {
    }

    public void afterSuite(TestDescriptor suite, TestResult result) {
        BinaryTestClassResult classResult = tests.get(suite.getClassName());
        if (classResult != null) {
            classResult.setEndTime(result.getEndTime());
        }
    }

    public void beforeTest(TestDescriptor testDescriptor) {
    }

    public void afterTest(TestDescriptor testDescriptor, TestResult result) {
        if (!testDescriptor.isComposite()) {
            BinaryTestResult binaryResult = new BinaryTestResult(testDescriptor.getName(), result);
            BinaryTestClassResult classResult = tests.get(testDescriptor.getClassName());
            if (classResult == null) {
                classResult = new BinaryTestClassResult(result.getStartTime());
                tests.put(testDescriptor.getClassName(), classResult);
            }

            classResult.add(binaryResult);
        }
    }

    public void onOutput(TestDescriptor testDescriptor, TestOutputEvent outputEvent) {
        String className = testDescriptor.getClassName();
        if (className == null) {
            //TODO SF ugly
            //this means that we receive an output before even starting any class. We don't have a place for such output.
            return;
        }
        File outputsFile = outputsFile(className);
        ObjectOutputStream out = null;
        try {
            if (outputsFile.exists()) {
                out = new AppendingObjectOutputStream(new FileOutputStream(outputsFile, true));
            } else {
                out = new ObjectOutputStream(new FileOutputStream(outputsFile));
            }
            out.writeBoolean(outputEvent.getDestination() == TestOutputEvent.Destination.StdOut);
            out.writeUTF(outputEvent.getMessage());
            out.close();
        } catch (IOException e) {
            UncheckedException.throwAsUncheckedException(e);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    private File outputsFile(String className) {
        return new File(resultsDir, className + ".output.bin");
    }

    public void populateOutputs(String className, XmlTestSuiteWriter testSuiteWriter) {
        File file = outputsFile(className);
        if (!file.exists()) {
            return; //test has no outputs
        }
        StringBuilder out = new StringBuilder();
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new FileInputStream(file));
            while(true) {
                boolean stdOut = in.readBoolean();
                TestOutputEvent.Destination dest = stdOut? TestOutputEvent.Destination.StdOut : TestOutputEvent.Destination.StdErr;
                String outputMessage = in.readUTF();
                testSuiteWriter.addOutput(dest, outputMessage);
                out.append(outputMessage);
            }
        } catch (EOFException e) {
            //ignore, the entire file was read
            return; //TODO SF ugly
        } catch (IOException e) {
            UncheckedException.throwAsUncheckedException(e);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    public class AppendingObjectOutputStream extends ObjectOutputStream {

        public AppendingObjectOutputStream(OutputStream out) throws IOException {
            super(out);
        }

        @Override
        protected void writeStreamHeader() throws IOException {
            //see http://stackoverflow.com/questions/1194656/appending-to-an-objectoutputstream/1195078#1195078
            reset();
        }
    }
}