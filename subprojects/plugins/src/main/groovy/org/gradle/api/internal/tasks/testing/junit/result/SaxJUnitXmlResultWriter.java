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

package org.gradle.api.internal.tasks.testing.junit.result;

import org.apache.tools.ant.util.DateUtils;
import org.gradle.api.Transformer;
import org.gradle.api.internal.tasks.testing.junit.binary.BinaryTestClassResult;
import org.gradle.api.internal.tasks.testing.junit.binary.BinaryTestResult;
import org.gradle.api.internal.tasks.testing.junit.binary.OutputsProvider;
import org.gradle.api.tasks.testing.TestOutputEvent;
import org.gradle.api.tasks.testing.TestResult;
import org.gradle.internal.UncheckedException;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Set;

/**
 * by Szczepan Faber, created at: 11/13/12
 */
public class SaxJUnitXmlResultWriter {

    private final String hostName;
    private final OutputsProvider outputsProvider;
    private final XMLOutputFactory xmlOutputFactory;

    public SaxJUnitXmlResultWriter(String hostName, OutputsProvider outputsProvider, XMLOutputFactory xmlOutputFactory) {
        this.hostName = hostName;
        this.outputsProvider = outputsProvider;
        this.xmlOutputFactory = xmlOutputFactory;
    }

    public void write(String className, BinaryTestClassResult result, Writer output) throws IOException {
        try {
            XMLStreamWriter writer = xmlOutputFactory.createXMLStreamWriter(output);
            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeCharacters("\n  ");
            writer.writeStartElement("testsuite");
            writer.writeAttribute("name", className);
            writer.writeAttribute("tests", String.valueOf(result.getTestsCount()));
            writer.writeAttribute("failures", String.valueOf(result.getFailuresCount()));
            writer.writeAttribute("errors", "0");
            writer.writeAttribute("timestamp", DateUtils.format(result.getStartTime(), DateUtils.ISO8601_DATETIME_PATTERN));
            writer.writeAttribute("hostname", hostName);
            writer.writeAttribute("time", String.valueOf((result.getEndTime() - result.getStartTime()) / 1000.0));

            writer.writeCharacters("\n  ");
            writer.writeEmptyElement("properties");

            writeTests(writer, result.getResults(), className);

            writer.writeCharacters("\n  ");
            output.write("<system-out><![CDATA[");
            outputsProvider.provideOutputs(className, TestOutputEvent.Destination.StdOut, new XmlEncoder(), output);
            output.write("]]></system-out>");

            writer.writeCharacters("\n  ");
            output.write("<system-err><![CDATA[");
            outputsProvider.provideOutputs(className, TestOutputEvent.Destination.StdErr, new XmlEncoder(), output);
            output.write("]]></system-err>\n");

            writer.writeEndElement();
            writer.writeEndDocument();
        } catch (XMLStreamException e) {
            throw UncheckedException.throwAsUncheckedException(e);
        }
    }

    private void writeTests(XMLStreamWriter writer, Set<BinaryTestResult> results, String className) throws XMLStreamException {
        for (BinaryTestResult result : results) {
            writer.writeCharacters("\n    ");
            String testCase = result.result.getResultType() == TestResult.ResultType.SKIPPED ? "ignored-testcase" : "testcase";
            writer.writeStartElement(testCase);
            writer.writeAttribute("name", result.name);
            writer.writeAttribute("classname", className);
            writer.writeAttribute("time", String.valueOf((result.result.getEndTime() - result.result.getStartTime()) / 1000.0));

            for (Throwable failure : result.result.getExceptions()) {
                writer.writeCharacters("\n      ");
                writer.writeStartElement("failure");
                writer.writeAttribute("message", failureMessage(failure));
                writer.writeAttribute("type", failure.getClass().getName());

                writer.writeCData(escapeCDATA(stackTrace(failure)));

                writer.writeEndElement();
            }

            writer.writeEndElement();
        }
    }

    private static String escapeCDATA(String input) {
        return input.replaceAll("]]>", "]]&gt;");
    }

    private static class XmlEncoder implements Transformer<String, String> {
        public String transform(String original) {
            return escapeCDATA(original);
        }
    }

    //below methods are "inherited" from the original xml writer

    private String failureMessage(Throwable throwable) {
        try {
            return throwable.toString();
        } catch (Throwable t) {
            return String.format("Could not determine failure message for exception of type %s: %s",
                    throwable.getClass().getName(), t);
        }
    }

    private String stackTrace(Throwable throwable) {
        try {
            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);
            throwable.printStackTrace(writer);
            writer.close();
            return stringWriter.toString();
        } catch (Throwable t) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);
            t.printStackTrace(writer);
            writer.close();
            return stringWriter.toString();
        }
    }
}