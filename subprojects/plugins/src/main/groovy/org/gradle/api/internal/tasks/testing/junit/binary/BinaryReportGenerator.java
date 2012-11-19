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
import java.util.*;

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
        if (suite.getParent() == null) {
            for (OutputStream outputStream : openFiles.values()) {
                IOUtils.closeQuietly(outputStream);
            }
            openFiles.clear();
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

    LinkedHashMap<String, OutputStream> openFiles = new LinkedHashMap<String, OutputStream>();

    public void onOutput(TestDescriptor testDescriptor, TestOutputEvent outputEvent) {
        String className = testDescriptor.getClassName();
        if (className == null) {
            //TODO SF ugly
            //this means that we receive an output before even starting any class. We don't have a place for such output.
            return;
        }
        try {
            OutputStream out;
            if (openFiles.containsKey(className + outputEvent.getDestination())) {
                out = openFiles.get(className + outputEvent.getDestination());
            } else {
                File outputsFile = outputsFile(className, outputEvent.getDestination());
                out = new FileOutputStream(outputsFile, true);
                openFiles.put(className + outputEvent.getDestination(), out);
                if (openFiles.size() > 10) {
                    Iterator<Map.Entry<String, OutputStream>> iterator = openFiles.entrySet().iterator();
                    IOUtils.closeQuietly(iterator.next().getValue());
                    iterator.remove();
                }
            }
            out.write(outputEvent.getMessage().getBytes());
        } catch(IOException e) {
            throw UncheckedException.throwAsUncheckedException(e);
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
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            IOUtils.copy(in, writer);
        } catch (EOFException e) {
            //we're done
            //TODO SF ugly
        } catch (IOException e) {
            throw UncheckedException.throwAsUncheckedException(e);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }
}