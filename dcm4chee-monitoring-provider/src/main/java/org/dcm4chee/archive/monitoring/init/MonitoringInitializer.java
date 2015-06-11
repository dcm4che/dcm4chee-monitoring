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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.dcm4chee.archive.monitoring.impl.config.Configuration;
import org.dcm4chee.archive.monitoring.impl.config.ForwardRule;
import org.dcm4chee.archive.monitoring.impl.config.ForwardRuleConfiguration;
import org.dcm4chee.archive.monitoring.impl.config.ForwardRuleProvider;
import org.dcm4chee.archive.monitoring.impl.config.MetricConfiguration;
import org.dcm4chee.archive.monitoring.impl.config.MonitoringConfiguration;
import org.dcm4chee.archive.monitoring.impl.config.VariableResolver;
import org.dcm4chee.archive.monitoring.impl.config.json.JsonMonitoringConfigurationProvider;
import org.dcm4chee.archive.monitoring.impl.core.Constants;
import org.dcm4chee.archive.monitoring.impl.core.MetricFactory;
import org.dcm4chee.archive.monitoring.impl.core.MetricProvider;
import org.dcm4chee.archive.monitoring.impl.core.Util;
import org.dcm4chee.archive.monitoring.impl.core.clocks.Clocks;
import org.dcm4chee.archive.monitoring.impl.core.context.AttachedContextResolverProvider;
import org.dcm4chee.archive.monitoring.impl.core.context.MonitoringContext;
import org.dcm4chee.archive.monitoring.impl.core.context.MonitoringContextProvider;
import org.dcm4chee.archive.monitoring.impl.core.module.MonitoringModuleManager;
import org.dcm4chee.archive.monitoring.impl.util.UnitOfTime;
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
    
    private static final String CONFIG_FILE_NAME = "monitoring.cfg";
    
    @Inject
    private MonitoringModuleManager moduleManager;
    
	@PostConstruct
	public void initialize() {
	    ClassLoader classloader = this.getClass().getClassLoader();
	    Configuration monitoringCfg = new JsonMonitoringConfigurationProvider().createConfigurationFromClasspath(
	            classloader, CONFIG_FILE_NAME);
	    
	    if(monitoringCfg == null) {
	        LOGGER.error("No monitoring configuration found");
	        return;
	    }
	    
		MonitoringConfiguration cfg = new MonitoringConfiguration();
		cfg.setClockProvider(Clocks.defaultClock());
		cfg.setReservoirConfigurations(monitoringCfg.getMetricReservoirConfigurations());
		
		cfg.setInitialNodeConfigurations(monitoringCfg.getNodeConfigurations());
		cfg.setInitialGlobalEnabled(monitoringCfg.isEnabled());
		
		MetricProvider metricProvider = cfg.createMetricProvider();
		
		// configure metrics registry
		long consumedMetricTimeoutMillis = monitoringCfg.getRegistryConfiguration().getConsumedMetricTimeout();
		metricProvider.getMetricRegistry().setConsumedMetricTimeout(consumedMetricTimeoutMillis, UnitOfTime.MILLISECONDS);
		long metricCleanupDaeomonRunPeriodMillis = monitoringCfg.getRegistryConfiguration().getMetricCleanupDaemonRunPeriod();
		metricProvider.getMetricRegistry().startMetricCleanupDaemon(metricCleanupDaeomonRunPeriodMillis, UnitOfTime.MILLISECONDS);
		
		MonitoringContextProvider contextProvider = metricProvider.getMonitoringContextProvider();
		MetricFactory metricFactory = metricProvider.getMetricFactory();
		
		
		for(MetricConfiguration metricCfg : monitoringCfg.getStartupConfiguration().getMetrics()) {
		    MonitoringContext metricCxt = contextProvider.getNodeContext().getOrCreateContext(metricCfg.getContextPath());
		    String metricType = metricCfg.getType();
		    switch(metricType) {
		    case "SumAggregate":
		        metricFactory.sumAggregate(metricCxt);
		        LOGGER.info("Created startup metric {} {}", metricType, metricCxt);
		        break;
		    default:
		        LOGGER.error("Unknown metric type {}", metricType);
		        break;
		    }
		}
		
		List<ForwardRuleConfiguration> fwRuleCfgs = monitoringCfg.getRuleConfiguration().getForwardRules();
		DefaultForwardRuleProvider fwRuleProvider = new DefaultForwardRuleProvider(fwRuleCfgs);
		AttachedContextResolverProvider.getInstance().getResolver().setForwardRuleProvider(fwRuleProvider);
		
		moduleManager.startModules();
	}
	
	private static class DefaultForwardRuleProvider implements ForwardRuleProvider {
        private final List<ForwardRule> rules = new ArrayList<>();
        private final GlobalVariableResolver globalResolver = new GlobalVariableResolver();
        
        public DefaultForwardRuleProvider(List<ForwardRuleConfiguration> ruleCfgs) {
            init(ruleCfgs);
        }
        
        private void init(List<ForwardRuleConfiguration> ruleCfgs) {
            for(ForwardRuleConfiguration ruleCfg : ruleCfgs) {
                ForwardRule rule = new ForwardRule();
                rule.addVariableResolver(globalResolver);
                rule.setSourcePathPattern(ruleCfg.getSourcePattern());
                for(String[] targetTemplate : ruleCfg.getTargetTemplates()) {
                    rule.addTargetPathTemplate(targetTemplate);
                }
                
                rules.add(rule);
            }
        }
        
        @Override
        public List<ForwardRule> getForwardRules() {
            return rules;
        }
        
    }
	
	private static class GlobalVariableResolver implements VariableResolver {
	      
        @Override
        public String getVariable(String name) {
            return Constants.GLOBAL_VAR__NODE.equals(name) ? Util.getJBossNodeName() : null;
        }
        
    }
	
}
