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

import org.gradle.api.internal.tasks.testing.junit.result.XmlTestSuiteWriter;
import org.gradle.api.internal.tasks.testing.junit.result.XmlTestSuiteWriterFactory;

import java.io.File;

/**
 * by Szczepan Faber, created at: 11/13/12
 */
public class Binary2JUnitXmlGenerator {

    XmlTestSuiteWriterFactory factory = new XmlTestSuiteWriterFactory();

    public void generate(File testResultsDir, BinaryReportGenerator binaryReportGenerator) {
        for (String className: binaryReportGenerator.tests.keySet()) {
            BinaryTestClassResult classResult = binaryReportGenerator.tests.get(className);
            XmlTestSuiteWriter writer = factory.create(testResultsDir, className, classResult.getStartTime());
            binaryReportGenerator.populateOutputs(className, writer);

            for (BinaryTestResult result : classResult.getResults()) {
                writer.addTestCase(result.name, result.result.getResultType(),
                    result.result.getEndTime() - result.result.getStartTime(), result.result.getExceptions());
            }
            writer.writeSuiteData(classResult.getEndTime() - classResult.getStartTime());
        }
    }
}