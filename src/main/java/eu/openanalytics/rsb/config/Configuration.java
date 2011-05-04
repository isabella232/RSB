/*
 *   R Service Bus
 *   
 *   Copyright (c) Copyright of OpenAnalytics BVBA, 2010-2011
 *
 *   ===========================================================================
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.openanalytics.rsb.config;

import java.io.File;
import java.net.URI;
import java.util.Map;

/**
 * Defines the configuration of RSB, which is injected in all components in order to support runtime
 * changes.
 * 
 * @author "OpenAnalytics <rsb.development@openanalytics.eu>"
 */
public interface Configuration {
    public static final String DEFAULT_JSON_CONFIGURATION_FILE = "rsb-configuration.json";

    public static final String R_SCRIPTS_CATALOG_SUBDIR = "r_scripts";
    public static final String SWEAVE_FILE_CATALOG_SUBDIR = "sweave_files";
    public static final String EMAIL_REPLIES_CATALOG_SUBDIR = "email_replies";

    /**
     * Directory where a catalog of R scripts are stored.
     */
    File getRScriptsCatalogDirectory();

    /**
     * Directory where a catalog of Sweave files are stored.
     */
    File getSweaveFilesCatalogDirectory();

    /**
     * Directory where a catalog of Email replies are stored.
     */
    File getEmailRepliesCatalogDirectory();

    /**
     * Directory where result files are written.
     */
    File getResultsDirectory();

    /**
     * Directory where ActiveMQ stores its persisted data.
     */
    File getActiveMqWorkDirectory();

    /**
     * URI of the RServi RMI pool.
     */
    URI getDefaultRserviPoolUri();

    /**
     * Optional mapping of application names and RServi RMI pool URIs.
     */
    Map<String, URI> getApplicationSpecificRserviPoolUris();

    /**
     * Maximum time a job request can be pending its response (in milliseconds).
     */
    int getJobTimeOut();

    /**
     * Number of concurrent job workers per queue, which must be computed based on the number of
     * nodes in the RServi pool and the number of job queues (one global plus one per "boosted"
     * application).
     */
    int getNumberOfConcurrentJobWorkersPerQueue();

    /**
     * The job statistics handler class to instantiate.
     */
    String getJobStatisticsHandlerClass();

    /**
     * The configuration specific to the job statistics handler.
     */
    Map<String, Object> getJobStatisticsHandlerConfiguration();
}
