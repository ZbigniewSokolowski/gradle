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
import org.gradle.api.Transformer;
import org.gradle.api.tasks.testing.*;
import org.gradle.internal.UncheckedException;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * by Szczepan Faber, created at: 11/13/12
 */
public class BinaryReportGenerator implements TestListener, TestOutputListener, OutputsProvider {

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
        File outputsFile = outputsFile(className, outputEvent.getDestination());
        ObjectOutputStream out = null;
        try {
            if (outputsFile.exists()) {
                out = new AppendingObjectOutputStream(new FileOutputStream(outputsFile, true));
            } else {
                out = new ObjectOutputStream(new FileOutputStream(outputsFile));
            }
            out.writeUTF(outputEvent.getMessage());
            out.close();
        } catch (IOException e) {
            UncheckedException.throwAsUncheckedException(e);
        } finally {
            IOUtils.closeQuietly(out);
        }
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

    public void provideOutputs(String className, TestOutputEvent.Destination destination, Transformer<String, String> transformer, Writer writer) {
        File file = outputsFile(className, destination);
        if (!file.exists()) {
            return; //test has no outputs
        }
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new FileInputStream(file));
            while(true) {
                String outputMessage = in.readUTF();
                writer.write(transformer.transform(outputMessage));
            }
        } catch (EOFException e) {
            //we're done
            //TODO SF ugly
        } catch (IOException e) {
            throw UncheckedException.throwAsUncheckedException(e);
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