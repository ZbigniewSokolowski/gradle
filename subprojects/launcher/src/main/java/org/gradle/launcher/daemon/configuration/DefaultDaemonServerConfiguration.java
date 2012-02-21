/*
 * Copyright 2009 the original author or authors.
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

package org.gradle.launcher.daemon.configuration;

import java.io.File;

/**
 * by Szczepan Faber, created at: 2/21/12
 */
public class DefaultDaemonServerConfiguration implements DaemonServerConfiguration {

    private final String daemonUid;
    private final File daemonBaseDir;
    private final int idleTimeoutMs;

    public DefaultDaemonServerConfiguration(String daemonUid, File daemonBaseDir, int idleTimeoutMs) {
        this.daemonUid = daemonUid;
        this.daemonBaseDir = daemonBaseDir;
        this.idleTimeoutMs = idleTimeoutMs;
    }

    public File getBaseDir() {
        return daemonBaseDir;
    }

    public int getIdleTimeout() {
        return idleTimeoutMs;
    }

    public String getUid() {
        return daemonUid;
    }
}