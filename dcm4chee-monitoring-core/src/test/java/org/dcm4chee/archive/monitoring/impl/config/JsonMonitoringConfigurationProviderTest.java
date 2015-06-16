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

package org.dcm4chee.archive.monitoring.impl.config;


import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.dcm4chee.archive.monitoring.impl.config.MetricReservoirConfiguration.RESERVOIR_TYPE;
import org.dcm4chee.archive.monitoring.impl.config.json.JsonMonitoringConfigurationProvider;
import org.dcm4chee.archive.monitoring.impl.core.reservoir.ReservoirBuilder.START_SPECIFICATION;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 *
 */
public class JsonMonitoringConfigurationProviderTest {

    @Test
    public void testConfigFileParsing() {
        JsonMonitoringConfigurationProvider jsonProvider = new JsonMonitoringConfigurationProvider();
        Path cfgFilePath = Paths.get("src", "test", "resources", "test_monitoring_cfg1.json");
        Configuration cfg = jsonProvider.createConfiguration(cfgFilePath);
        Assert.assertNotNull(cfg);
        
        StartupConfiguration startupCfg = cfg.getStartupConfiguration();
        Assert.assertNotNull(startupCfg);
        
        List<MetricConfiguration> metrics = startupCfg.getMetrics();
        Assert.assertNotNull(metrics);
        Assert.assertEquals(2, metrics.size());
        
        MetricConfiguration metricCfg1 = metrics.iterator().next();
        Assert.assertArrayEquals(new String[] {"db", "connection"}, metricCfg1.getContextPath());
        Assert.assertEquals("SumAggregate", metricCfg1.getType());
        
        MetricRegistryConfiguration registryCfg = cfg.getRegistryConfiguration();
        Assert.assertNotNull(registryCfg);
        Assert.assertEquals(5000, registryCfg.getConsumedMetricTimeout());
        Assert.assertEquals(6000, registryCfg.getMetricCleanupDaemonRunPeriod());
    }
    
    @Test
    public void testConfigFileParsingFromClasspath() {
        JsonMonitoringConfigurationProvider jsonProvider = new JsonMonitoringConfigurationProvider();
        Configuration cfg = jsonProvider.createConfigurationFromClasspath(this.getClass().getClassLoader(), "test_monitoring_cfg1.json");
        Assert.assertNotNull(cfg);
    }
    
    private void createJsonConfiguration() {
        List<MetricConfiguration> metrics = new ArrayList<>(); 
        
        MetricConfiguration metricCfg1 = new MetricConfiguration();
        metricCfg1.setContextPath(new String[] {"db", "connection"});
        metricCfg1.setType("SumAggregate");
        metrics.add(metricCfg1);
        
        MetricConfiguration metricCfg2 = new MetricConfiguration();
        metricCfg2.setContextPath(new String[] {"db", "connection"});
        metricCfg2.setType("SumAggregate");
        metrics.add(metricCfg2);
        
        StartupConfiguration startupCfg = new StartupConfiguration();
        startupCfg.setMetrics(metrics);
        
        MetricRegistryConfiguration regCfg = new MetricRegistryConfiguration();
        regCfg.setConsumedMetricTimeout(5000);
        regCfg.setMetricCleanupDaemonRunPeriod(6000);
        
        List<ForwardRuleConfiguration> fwRules = new ArrayList<>();
        
        ForwardRuleConfiguration fwRule1 = new ForwardRuleConfiguration();
        fwRule1.setSourcePattern(new String[] {"**","connection", "*", "statement","*"} );
        List<String[]> targetTemplates = new ArrayList<>();
        targetTemplates.add(new String[] {"$node", "db", "connection"});
        fwRule1.setTargetTemplates(targetTemplates);
        fwRules.add(fwRule1);
        
        RuleConfiguration ruleCfg = new RuleConfiguration();
        ruleCfg.setForwardRules(fwRules);
        
        MetricReservoirConfiguration metricResCfg1 = new MetricReservoirConfiguration();
        metricResCfg1.setName("default");
        metricResCfg1.setResolutions(60,120);
        metricResCfg1.setRetentions(2,2);
        metricResCfg1.setType(RESERVOIR_TYPE.ROUND_ROBIN);
        metricResCfg1.setStart(START_SPECIFICATION.CURRENT_MIN);
        
        MetricReservoirConfiguration metricResCfg2 = new MetricReservoirConfiguration();
        metricResCfg2.setName("oneshot");
        metricResCfg2.setResolutions(60,120);
        metricResCfg2.setType(RESERVOIR_TYPE.OPEN_RESOLUTION);
        
        NodeConfiguration nodeCfg1 = new NodeConfiguration();
        nodeCfg1.setEnabled(true);
        nodeCfg1.setContextPath(new String[0]);
        
        ModuleConfiguration mCfg = new ModuleConfiguration();
        Map<String,String> params = new HashMap<>();
        params.put("datasources", "java:/jndi, test");
        params.put("flag", "true");
        mCfg.setParameters(params);
        mCfg.setParameters(params);
        
        
        Configuration cfg = new Configuration();
        cfg.setStartupConfiguration(startupCfg);
        cfg.setRegistryConfiguration(regCfg);
        cfg.setRuleConfiguration(ruleCfg);
        cfg.setMetricReservoirConfigurations(Arrays.asList(metricResCfg1, metricResCfg2));
        cfg.setNodeConfigurations(Arrays.asList(nodeCfg1));
        
        cfg.setModuleConfigurations(Arrays.asList(mCfg));
        
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        File cfgFile = new File("test_cfg.json");
        System.out.println(cfgFile.getAbsolutePath());
        try {
            writer.writeValue(cfgFile, cfg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
//    @Test
    public void testCreateCfg() {
        createJsonConfiguration();
    }
}
