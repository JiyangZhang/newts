/*
 * Copyright 2014, The OpenNMS Group
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
package org.opennms.newts.stress;


import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.TimeUnit;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;


abstract class Dispatcher {

    protected final Worker[] m_threads;
    protected final MetricRegistry m_metricRegistry = new MetricRegistry();
    protected final ConsoleReporter m_reporter;

    Dispatcher(Config config) {
        checkNotNull(config, "config argument");
        m_threads = new Worker[config.getThreads()];
        m_reporter = ConsoleReporter.forRegistry(m_metricRegistry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        m_reporter.start(30, TimeUnit.SECONDS);
    }

    abstract void go() throws InterruptedException;

    void setupReport() {
        ConsoleReporter reporter = ConsoleReporter.forRegistry(m_metricRegistry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();

    }

    void shutdown() throws InterruptedException {
        // Trigger shutdown on all threads
        for (Worker w : m_threads) {
            w.shutdown();
        }

        for (Worker w : m_threads) {
            w.join();
        }

        m_reporter.stop();
    }

    void printReport() {
        m_reporter.report();
    }

}
