/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.pinterest.secor.common.monitoring;

import com.pinterest.secor.common.SecorConfig;
import com.pinterest.secor.util.StatsUtil;
import com.twitter.ostrich.admin.AdminServiceFactory;
import com.twitter.ostrich.admin.CustomHttpHandler;
import com.twitter.ostrich.admin.RuntimeEnvironment;
import com.twitter.ostrich.admin.StatsFactory;
import com.twitter.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;
import scala.collection.JavaConversions;
import scala.collection.Map$;
import scala.collection.immutable.List$;
import scala.collection.immutable.Map;
import scala.util.matching.Regex;

import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * OstrichAdminService initializes export of metrics to Ostrich.
 *
 * @author Pawel Garbacki (pawel@pinterest.com)
 */
public class OstrichAdminService {
    private static final Logger LOG = LoggerFactory.getLogger(OstrichAdminService.class);
    private final int mPort;
    private final boolean mPrometheusEnabled;

    public OstrichAdminService(SecorConfig config) {
        mPort = config.getOstrichPort();
        mPrometheusEnabled = config.getMicroMeterCollectorPrometheusEnabled();
    }

    public void start() {
        Duration[] defaultLatchIntervals = {Duration.apply(1, TimeUnit.MINUTES)};
        Map<String, CustomHttpHandler> handlers = mPrometheusEnabled ?
                new Map.Map1<>("/prometheus", new PrometheusHandler()) : Map$.MODULE$.empty();
        @SuppressWarnings("deprecation")
        AdminServiceFactory adminServiceFactory = new AdminServiceFactory(
            this.mPort,
            20,
            List$.MODULE$.<StatsFactory>empty(),
            Option.<String>empty(),
            List$.MODULE$.<Regex>empty(),
            handlers,
            JavaConversions
                .asScalaBuffer(Arrays.asList(defaultLatchIntervals)).toList()
        );
        RuntimeEnvironment runtimeEnvironment = new RuntimeEnvironment(this);
        adminServiceFactory.apply(runtimeEnvironment);
        try {
            Properties properties = new Properties();
            properties.load(this.getClass().getResource("build.properties").openStream());
            String buildRevision = properties.getProperty("build_revision", "unknown");
            LOG.info("build.properties build_revision: {}",
                     properties.getProperty("build_revision", "unknown"));
            StatsUtil.setLabel("secor.build_revision", buildRevision);
        } catch (Throwable t) {
            LOG.error("Failed to load properties from build.properties", t);
        }
    }
}
