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

package org.dcm4chee.archive.monitoring.impl.jdbc;

import javax.servlet.ServletContext;

import org.dcm4chee.archive.monitoring.impl.config.ModuleConfiguration;
import org.dcm4chee.archive.monitoring.impl.core.module.MonitoringModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 *
 */
public class JdbcMonitoringModule implements MonitoringModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcMonitoringModule.class);
    
    private static final String MODULE_NAME = "jdbc";
    
    private ModuleConfiguration cfg;
    
    @Override
    public void start() {
        ServletContext servletContext = MonitoringContextListener.getServletContext();
        if(servletContext == null) {
            LOGGER.error("Cannot start JDBC monitoring module before web context is initialized");
            return;
        }
        
        Parameters.initialize(cfg);
        
        JdbcWrapper jdbcWrapper = JdbcWrapper.SINGLETON;
        
        String levelString = cfg.getLevel();
        LEVEL level = JdbcMonitoringModule.LEVEL.valueOf(levelString);
        AbstractLevelStrategy strategy = null;
        switch(level) {
        case SERVICE:
            strategy = new ServiceLevelStrategy();
            break;
        case SERVICE_INSTANCE:
            strategy = new ServiceInstanceLevelStrategy();
            break;
        case STATEMENT_INSTANCE:
            strategy = new StatementInstanceLevelStrategy();
            break;
        default:
            break;
        }
        
        jdbcWrapper.setLevelStrategy(strategy);
        
        jdbcWrapper.initServletContext(servletContext);
        
        jdbcWrapper.rebindDataSources();
    }
    
    public void setConfiguration(ModuleConfiguration cfg) {
        this.cfg = cfg;
    }
    
    @Override
    public void stop() {
        JdbcWrapper jdbcWrapper = JdbcWrapper.SINGLETON;
        jdbcWrapper.stop();
    }

    @Override
    public String getName() {
        return MODULE_NAME;
    }
    
    public static enum LEVEL {
        SERVICE,
        SERVICE_INSTANCE,
        STATEMENT_INSTANCE
    }
}
