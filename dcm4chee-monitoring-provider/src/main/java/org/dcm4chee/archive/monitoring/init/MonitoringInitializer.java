//
/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), hosted at https://github.com/gunterze/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Agfa Healthcare.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.dcm4chee.archive.monitoring.init;

import static java.lang.String.format;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.dcm4chee.archive.monitoring.impl.config.Configuration;
import org.dcm4chee.archive.monitoring.impl.config.MonitoringBuilder;
import org.dcm4chee.archive.monitoring.impl.config.json.JsonMonitoringConfigurationProvider;
import org.dcm4chee.archive.monitoring.impl.core.clocks.Clocks;
import org.dcm4chee.archive.monitoring.impl.core.module.MonitoringModuleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EJB to initialize monitoring at application start-up.
 * 
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 */
@Singleton
@Startup
public class MonitoringInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MonitoringInitializer.class);
    
    private static final String CSP_MONITORING_CONFIG_FILE_PROP = "csp-monitoring-cfg-file";
    private static final String CSP_MONITORING_CONFIG_CLASSPATHFILE_PROP = "csp-monitoring-cfg-classpathfile";
    private static final String CSP_MONITORING_CONFIG_ENV_VAR = "CSP-MONITORING-CFG-FILE";
    private static final String CSP_MONITORING_CONFIG_CLASSPATHFILE_DEFAULT = "monitoring.cfg";
    
    
    @Inject
    private MonitoringModuleManager moduleManager;
    
	@PostConstruct
	public void initialize() {
	    ClassLoader classloader = this.getClass().getClassLoader();
	    
	    Configuration monitoringCfg = null;
	    
	    // 1) Try to load config from file system via system property
	    String configFilePath = System.getProperty(CSP_MONITORING_CONFIG_FILE_PROP);
	    if(configFilePath != null ) {
	        LOGGER.info(format("Trying to load monitoring configuration file specified by property '%s': %s", 
	                CSP_MONITORING_CONFIG_FILE_PROP, configFilePath ) );
	        monitoringCfg = new JsonMonitoringConfigurationProvider().createConfiguration(configFilePath);
	    } else {
	        // 2) Try to load config from classpath via system property
	        String classPathFileName = System.getProperty(CSP_MONITORING_CONFIG_CLASSPATHFILE_PROP);
	        if(classPathFileName != null) {
	            LOGGER.info("Trying to load monitoring configuration classpath file " + classPathFileName );
	            LOGGER.info(format("Trying to load monitoring configuration classpath file specified by property '%s': %s", 
	                    CSP_MONITORING_CONFIG_CLASSPATHFILE_PROP, classPathFileName ) );
	            monitoringCfg = new JsonMonitoringConfigurationProvider().createConfigurationFromClasspath(
	                    classloader, classPathFileName);
	        } else {
	            // 3) Try to load config from file system via environment variable
	            configFilePath = System.getenv(CSP_MONITORING_CONFIG_ENV_VAR);
	            if(configFilePath != null) {
	                LOGGER.info(format("Trying to load monitoring configuration file specified by environment variable '%s': %s",
	                        CSP_MONITORING_CONFIG_ENV_VAR, configFilePath ));
	                monitoringCfg = new JsonMonitoringConfigurationProvider().createConfiguration(configFilePath);
	            } else {
	                // 4) Try to load config from classpath file with default name
	                LOGGER.info(format("Trying to load monitoring configuration classpath default file: %s", 
	                        CSP_MONITORING_CONFIG_CLASSPATHFILE_DEFAULT) );
	                monitoringCfg = new JsonMonitoringConfigurationProvider().createConfigurationFromClasspath(
	                        classloader, CSP_MONITORING_CONFIG_CLASSPATHFILE_DEFAULT);
	            }
	        }
	    }
	    
	    if(monitoringCfg == null) {
	        LOGGER.error("Monitoring configuration file could not be loaded");
	        return;
	    }
	    
	    monitoringCfg.setClockProvider(Clocks.defaultClock());
	    monitoringCfg.setModuleManager(moduleManager);
	    
	    new MonitoringBuilder(monitoringCfg).createMetricProvider();
	}
	
}
