/*******************************************************************************
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *******************************************************************************/
package org.jetbrains.kotlin.core.launch;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class CompilerOutputParser {
    
    public static void parseCompilerMessagesFromReader(MessageCollector messageCollector, final Reader reader) {
        final StringBuilder stringBuilder = new StringBuilder();
        Reader wrappingReader = new Reader() {

            @Override
            public int read(char[] cbuf, int off, int len) throws IOException {
                int read = reader.read(cbuf, off, len);
                stringBuilder.append(cbuf, off, len);
                return read;
            }

            @Override
            public void close() throws IOException {
            }
        };
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            parser.parse(new InputSource(wrappingReader), new CompilerOutputSAXHandler(messageCollector));
        }
        catch (Throwable e) {
            String message = stringBuilder.toString();
            messageCollector.report(CompilerMessageSeverity.ERROR, message, null);
        }
        finally {
            try {
                reader.close();
            }
            catch (IOException e) {
                KotlinLogger.logError(e);
            }
        }
    }
    
    private static class CompilerOutputSAXHandler extends DefaultHandler {
        private static final Map<String, CompilerMessageSeverity> CATEGORIES = new HashMap<String, CompilerMessageSeverity>();
        
        static {
            CATEGORIES.put("error", CompilerMessageSeverity.ERROR);
            CATEGORIES.put("warning", CompilerMessageSeverity.WARNING);
            CATEGORIES.put("logging", CompilerMessageSeverity.LOGGING);
            CATEGORIES.put("output", CompilerMessageSeverity.OUTPUT);
            CATEGORIES.put("exception", CompilerMessageSeverity.EXCEPTION);
            CATEGORIES.put("info", CompilerMessageSeverity.INFO);
            CATEGORIES.put("messages", CompilerMessageSeverity.INFO); 
        }
             

        private final MessageCollector messageCollector;

        private final StringBuilder message = new StringBuilder();
        private final Stack<String> tags = new Stack<String>();
        private String path;
        private int line;
        private int column;

        public CompilerOutputSAXHandler(MessageCollector messageCollector) {
            this.messageCollector = messageCollector;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            tags.push(qName);

            message.setLength(0);

            String rawPath = attributes.getValue("path");
            path = rawPath == null ? null : rawPath;
            line = safeParseInt(attributes.getValue("line"), -1);
            column = safeParseInt(attributes.getValue("column"), -1);
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (tags.size() == 1) {
                String message = new String(ch, start, length);
                if (!message.trim().isEmpty()) {
                    messageCollector.report(CompilerMessageSeverity.ERROR, "Unhandled compiler output: " + message, null);
                }
            }
            else {
                message.append(ch, start, length);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (tags.size() == 1) {
                return;
            }
            String qNameLowerCase = qName.toLowerCase();
            CompilerMessageSeverity category = CATEGORIES.get(qNameLowerCase);
            if (category == null) {
                messageCollector.report(CompilerMessageSeverity.ERROR, "Unknown compiler message tag: " + qName, null);
                category = CompilerMessageSeverity.INFO;
            }
            String text = message.toString();

            messageCollector.report(category, text, CompilerMessageLocation.create(path, line, column, null));
            
            tags.pop();
        }
        
        private static int safeParseInt(String value, int defaultValue) {
            if (value == null) {
                return defaultValue;
            }
            try {
                return Integer.parseInt(value.trim());
            }
            catch (NumberFormatException e) {
                return defaultValue;
            }
        }
    }
}