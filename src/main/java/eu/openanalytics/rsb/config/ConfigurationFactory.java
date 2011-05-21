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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.validator.EmailValidator;

import eu.openanalytics.rsb.Util;
import eu.openanalytics.rsb.config.Configuration.DepositDirectoryConfiguration;
import eu.openanalytics.rsb.config.Configuration.DepositEmailConfiguration;

/**
 * Loads and validated an RSB configuration file and builds a configuration object out of it.
 * 
 * @author "OpenAnalytics <rsb.development@openanalytics.eu>"
 */
public abstract class ConfigurationFactory {
    private static final Log LOGGER = LogFactory.getLog(ConfigurationFactory.class);

    private ConfigurationFactory() {
        throw new UnsupportedOperationException("do not instantiate");
    }

    public static Configuration loadJsonConfiguration(final String configurationFile) throws IOException {
        final PersistedConfigurationAdapter pca = loadAndValidateJsonConfigurationFile(configurationFile);
        createMissingDirectories(pca);
        return pca;
    }

    // exposed for testing

    static PersistedConfigurationAdapter loadAndValidateJsonConfigurationFile(final String configurationFile) throws IOException {
        final URL configurationUrl = Thread.currentThread().getContextClassLoader().getResource(configurationFile);
        Validate.notNull(configurationUrl, "Impossible to find " + configurationFile + " on the classpath");

        final InputStream is = configurationUrl.openStream();
        final String json = IOUtils.toString(is);
        IOUtils.closeQuietly(is);

        final PersistedConfiguration pc = Util.fromJson(json, PersistedConfiguration.class);
        final PersistedConfigurationAdapter pca = new PersistedConfigurationAdapter(configurationUrl, pc);
        validate(pca);
        return pca;
    }

    static void validate(final PersistedConfigurationAdapter pca) throws IOException {
        LOGGER.info("Validating configuration: " + pca.getConfigurationUrl());
        Validate.notNull(pca.getActiveMqWorkDirectory(), "activeMqWorkDirectory can't be null: " + pca);
        Validate.notNull(pca.getResultsDirectory(), "rsbResultsDirectory can't be null: " + pca);
        Validate.notNull(pca.getDefaultRserviPoolUri(), "defaultRserviPoolUri can't be null: " + pca);
        Validate.notNull(pca.getSmtpConfiguration(), "smtpConfiguration can't be null: " + pca);

        if (StringUtils.isNotEmpty(pca.getAdministratorEmail())) {
            Validate.isTrue(EmailValidator.getInstance().isValid(pca.getAdministratorEmail()),
                    "if present, the administrator email must be valid");
        }

        if (pca.getDepositRootDirectories() != null) {

            for (final DepositDirectoryConfiguration depositRootDirectoryConfig : pca.getDepositRootDirectories()) {
                final String depositApplicationName = depositRootDirectoryConfig.getApplicationName();
                Validate.isTrue(Util.isValidApplicationName(depositApplicationName), "invalid deposit directory application name: "
                        + depositApplicationName);
            }
        }

        if (pca.getDepositEmailAccounts() != null) {
            for (final DepositEmailConfiguration depositEmailAccount : pca.getDepositEmailAccounts()) {
                Validate.isTrue(Util.isValidApplicationName(depositEmailAccount.getApplicationName()),
                        "invalid deposit email application name: " + depositEmailAccount.getApplicationName());

                if (depositEmailAccount.getResponseFileName() != null) {
                    final File responseFile = new File(pca.getEmailRepliesCatalogDirectory(), depositEmailAccount.getResponseFileName());
                    Validate.isTrue(responseFile.exists(), "missing response file: " + responseFile);
                }

                if (depositEmailAccount.getJobConfigurationFileName() != null) {
                    final File jobConfigurationFile = new File(pca.getEmailRepliesCatalogDirectory(),
                            depositEmailAccount.getJobConfigurationFileName());
                    Validate.isTrue(jobConfigurationFile.exists(), "missing job configuration file: " + jobConfigurationFile);
                }
            }
        }

        LOGGER.info("Successfully validated: " + pca);
    }

    static void createMissingDirectories(final PersistedConfigurationAdapter pca) throws IOException {
        FileUtils.forceMkdir(pca.getRScriptsCatalogDirectory());
        FileUtils.forceMkdir(pca.getSweaveFilesCatalogDirectory());
        FileUtils.forceMkdir(pca.getJobConfigurationCatalogDirectory());
        FileUtils.forceMkdir(pca.getEmailRepliesCatalogDirectory());

        if (pca.getDepositRootDirectories() != null) {
            for (final DepositDirectoryConfiguration depositRootDirectoryConfig : pca.getDepositRootDirectories()) {
                final File depositRootDir = depositRootDirectoryConfig.getRootDirectory();
                FileUtils.forceMkdir(depositRootDir);
                FileUtils.forceMkdir(new File(depositRootDir, Configuration.DEPOSIT_ACCEPTED_SUBDIR));
                FileUtils.forceMkdir(new File(depositRootDir, Configuration.DEPOSIT_JOBS_SUBDIR));
                FileUtils.forceMkdir(new File(depositRootDir, Configuration.DEPOSIT_RESULTS_SUBDIR));
            }
        }
    }
}
