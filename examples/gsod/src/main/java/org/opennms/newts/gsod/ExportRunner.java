/*
 * Copyright 2014-2024, The OpenNMS Group
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opennms.newts.gsod;


import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionHandlerRegistry;
import org.kohsuke.args4j.ParserProperties;
import org.opennms.newts.api.Context;
import org.opennms.newts.api.Resource;
import org.opennms.newts.api.Results;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.SampleRepository;
import org.opennms.newts.api.Timestamp;

import com.google.common.base.Optional;
import com.google.inject.Guice;
import com.google.inject.Injector;

@SuppressWarnings("java:S106")
public class ExportRunner {

    static {
        OptionHandlerRegistry.getRegistry().registerHandler(Resource.class, ResourceOptionHandler.class);
    }

    private final SampleRepository m_repository;
    private final CmdLineParser m_parser = new CmdLineParser(this, ParserProperties.defaults().withUsageWidth(80));

    @Option(name = "-r", usage = "resource name to query (required)")
    private Resource m_resource;

    @Option(name = "-m", usage = "metric to query (required)")
    private String m_metric;

    @Option(name = "-s", usage = "query start time in milliseconds")
    private Long m_start;

    @Option(name = "-e", usage = "query end time milliseconds")
    private Long m_end;

    @Option(name = "-h", usage = "print this usage information")
    private boolean m_needsHelp;

    @Inject
    public ExportRunner(SampleRepository repository) {
        m_repository = repository;
    }

    private void printUsage(PrintStream writer) {
        writer.println("Usage: java ExportRunner [options...] ");
        m_parser.printUsage(writer);
    }

    private int go(String[] args) {
        try {
            m_parser.parseArgument(args);
        }
        catch (CmdLineException e) {
            System.err.println(e.getMessage());
            printUsage(System.err);
            return 1;
        }

        if (m_needsHelp) {
            printUsage(System.out);
            return 0;
        }

        if (m_resource == null || m_metric == null) {
            System.err.println("Missing required argument(s)");
            printUsage(System.err);
            return 1;
        }

        System.out.printf("timestamp,%s%n", m_metric);

        for (Results.Row<Sample> row : m_repository.select(Context.DEFAULT_CONTEXT, m_resource, timestamp(m_start), timestamp(m_end))) {
            System.out.printf("%s,%.2f%n", row.getTimestamp().asDate(), row.getElement(m_metric).getValue().doubleValue());
        }

        return 0;
    }

    @SuppressWarnings("java:S4738")
    private static Optional<Timestamp> timestamp(Long arg) {
        if (arg == null) return Optional.<Timestamp> absent();
        return Optional.of(new Timestamp(arg, TimeUnit.MILLISECONDS));
    }

    public static void main(String... args) {

        Injector injector = Guice.createInjector(new Config());
        ExportRunner runner = injector.getInstance(ExportRunner.class);

        System.exit(runner.go(args));

    }

}
