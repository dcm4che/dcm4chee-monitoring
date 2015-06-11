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

package org.dcm4chee.archive.monitoring.impl.core.context;


import java.util.Arrays;

import org.dcm4chee.archive.monitoring.impl.config.MetricReservoirConfiguration;
import org.dcm4chee.archive.monitoring.impl.config.MetricReservoirConfiguration.RESERVOIR_TYPE;
import org.dcm4chee.archive.monitoring.impl.config.MonitoringConfiguration;
import org.dcm4chee.archive.monitoring.impl.core.Counter;
import org.dcm4chee.archive.monitoring.impl.core.ManualClock;
import org.dcm4chee.archive.monitoring.impl.core.Metric;
import org.dcm4chee.archive.monitoring.impl.core.MetricFactory;
import org.dcm4chee.archive.monitoring.impl.core.MetricProvider;
import org.dcm4chee.archive.monitoring.impl.core.metric.NoCounter;
import org.dcm4chee.archive.monitoring.impl.core.registry.MetricFilters;
import org.dcm4chee.archive.monitoring.impl.core.reservoir.ReservoirBuilder.START_SPECIFICATION;
import org.dcm4chee.archive.monitoring.impl.util.UnitOfTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 *
 */
public class MonitoringContextTreeTest {
	private MetricProvider provider;
	private MonitoringContextProvider contextProvider;
	private MetricFactory metricFactory;
	private MonitoringContextTree metricRegistry;
	private ManualClock clock;
	
	
	@Before
	public void before() {
	    clock = new ManualClock(0, 500, UnitOfTime.MILLISECONDS);
		MonitoringConfiguration cfg = new MonitoringConfiguration();
		cfg.setClockProvider(clock);
		
		MetricReservoirConfiguration reservoirCfg = createDefaultMetricReservoirConfiguration();
		
		cfg.setReservoirConfigurations(Arrays.asList(reservoirCfg));
		provider = cfg.createMetricProvider();
		contextProvider = provider.getMonitoringContextProvider();
		metricFactory = provider.getMetricFactory();
		metricRegistry = (MonitoringContextTree)provider.getMetricRegistry();
	}
	
	private MetricReservoirConfiguration createDefaultMetricReservoirConfiguration() {
        MetricReservoirConfiguration reservoirCfg = new MetricReservoirConfiguration();
        reservoirCfg.setType(RESERVOIR_TYPE.ROUND_ROBIN);
        reservoirCfg.setName("DEFAULT");
        reservoirCfg.setResolutionStepSize(60l);
        reservoirCfg.setResolutions(new long[] { 60l, 60l * 2l });
        reservoirCfg.setRetentions( new int[] { 5, 10 });
        reservoirCfg.setValueReservoirs(new boolean[] { true, false});
        reservoirCfg.setStart(START_SPECIFICATION.CURRENT_MIN);
        return reservoirCfg;
    }
	
	@Test
	public void testConsumableMetricDisposal() {
		MonitoringContext cxt = contextProvider.getNodeContext().getOrCreateContext("service");
		Counter counter = metricFactory.counter(cxt, Counter.TYPE.DEFAULT);
		
		Metric registeredCounter = metricRegistry.getMetric(MetricFilters.ALL_FILTER, cxt);
		Assert.assertSame(counter, registeredCounter);
		
		cxt.dispose();
		
		Metric registeredCounterAfterDisposal = metricRegistry.getMetric(MetricFilters.ALL_FILTER, cxt);
		Assert.assertSame(registeredCounter, registeredCounterAfterDisposal);
		
		Metric consumedCounter = metricRegistry.consumeMetric(MetricFilters.ALL_FILTER, cxt);
		Assert.assertSame(registeredCounterAfterDisposal, consumedCounter);
		
		Metric nullCounterAfterConsumption = metricRegistry.getMetric(MetricFilters.ALL_FILTER, cxt);
		Assert.assertNull(nullCounterAfterConsumption);
		
		// remember the undefined node!
		Assert.assertEquals(1, contextProvider.getNodeContext().getChildren(false).size());
	}
	
	@Test
	public void testHierarchicalConsumableMetricDisposal() {
		MonitoringContext cxt = contextProvider.getNodeContext().getOrCreateContext("service");
		Counter counter = metricFactory.counter(cxt, Counter.TYPE.DEFAULT);
		MonitoringContext subCxt = contextProvider.getNodeContext().getOrCreateContext("service", "sub");
		Counter subCounter = metricFactory.counter(subCxt, Counter.TYPE.DEFAULT);
		
		Metric registeredCounter = metricRegistry.getMetric(MetricFilters.ALL_FILTER, cxt);
		Assert.assertSame(counter, registeredCounter);
		Metric registeredSubCounter = metricRegistry.getMetric(MetricFilters.ALL_FILTER, subCxt);
		Assert.assertSame(subCounter, registeredSubCounter);
		
		cxt.dispose();
		
		Metric registeredCounterAfterDisposal = metricRegistry.getMetric(MetricFilters.ALL_FILTER, cxt);
		Assert.assertSame(registeredCounter, registeredCounterAfterDisposal);
		Metric registeredSubCounterAfterDisposal = metricRegistry.getMetric(MetricFilters.ALL_FILTER, subCxt);
		Assert.assertSame(registeredSubCounter, registeredSubCounterAfterDisposal);
		
		Metric consumedCounter = metricRegistry.consumeMetric(MetricFilters.ALL_FILTER, cxt);
		Assert.assertSame(registeredCounterAfterDisposal, consumedCounter);
		Metric consumedSubCounter = metricRegistry.consumeMetric(MetricFilters.ALL_FILTER, subCxt);
		Assert.assertSame(registeredSubCounterAfterDisposal, consumedSubCounter);
		
		Metric nullCounterAfterConsumption = metricRegistry.getMetric(MetricFilters.ALL_FILTER, cxt);
		Assert.assertNull(nullCounterAfterConsumption);
		Metric nullSubCounterAfterConsumption = metricRegistry.getMetric(MetricFilters.ALL_FILTER, cxt);
		Assert.assertNull(nullSubCounterAfterConsumption);
		
		// remember the undefined node!
		Assert.assertEquals(1, contextProvider.getNodeContext().getChildren(false).size());
	}
	
	@Test
    public void testHierarchicalConsumableMetricDisposal2() {
        MonitoringContext cxt = contextProvider.getNodeContext().getOrCreateContext("service");
        Counter counter = metricFactory.counter(cxt, Counter.TYPE.DEFAULT);
        MonitoringContext subCxt = contextProvider.getNodeContext().getOrCreateContext("service", "intermediate", "sub");
        Counter subCounter = metricFactory.counter(subCxt, Counter.TYPE.DEFAULT);
        
        Metric registeredCounter = metricRegistry.getMetric(MetricFilters.ALL_FILTER, cxt);
        Assert.assertSame(counter, registeredCounter);
        Metric registeredSubCounter = metricRegistry.getMetric(MetricFilters.ALL_FILTER, subCxt);
        Assert.assertSame(subCounter, registeredSubCounter);
        
        cxt.dispose();
        
        Metric registeredCounterAfterDisposal = metricRegistry.getMetric(MetricFilters.ALL_FILTER, cxt);
        Assert.assertSame(registeredCounter, registeredCounterAfterDisposal);
        Metric registeredSubCounterAfterDisposal = metricRegistry.getMetric(MetricFilters.ALL_FILTER, subCxt);
        Assert.assertSame(registeredSubCounter, registeredSubCounterAfterDisposal);
        
        Metric consumedSubCounter = metricRegistry.consumeMetric(MetricFilters.ALL_FILTER, subCxt);
        Assert.assertSame(registeredSubCounterAfterDisposal, consumedSubCounter);
        Metric consumedCounter = metricRegistry.consumeMetric(MetricFilters.ALL_FILTER, cxt);
        Assert.assertSame(registeredCounterAfterDisposal, consumedCounter);
        
        Metric nullCounterAfterConsumption = metricRegistry.getMetric(MetricFilters.ALL_FILTER, cxt);
        Assert.assertNull(nullCounterAfterConsumption);
        Metric nullSubCounterAfterConsumption = metricRegistry.getMetric(MetricFilters.ALL_FILTER, cxt);
        Assert.assertNull(nullSubCounterAfterConsumption);
        
        // remember the undefined node!
        Assert.assertEquals(1, contextProvider.getNodeContext().getChildren(false).size());
    }
	
	@Test
	public void testConsumedMetricCleanupDaemon() {
	    metricRegistry.startMetricCleanupDaemon(10, UnitOfTime.MILLISECONDS);
	    metricRegistry.setConsumedMetricTimeout(1001, UnitOfTime.MILLISECONDS);

	    MonitoringContext cxt = contextProvider.getNodeContext().getOrCreateContext("service");
        metricFactory.counter(cxt, Counter.TYPE.DEFAULT);
        
        Metric registeredCounter = metricRegistry.getMetric(MetricFilters.ALL_FILTER, cxt);
        Assert.assertNotNull(registeredCounter);
        
        // dispose metric
        // until the metric is (1.) not consumed OR (2.) no timeout happened it still exists
        cxt.dispose();
        
        registeredCounter = metricRegistry.getMetric(MetricFilters.ALL_FILTER, cxt);
        Assert.assertNotNull(registeredCounter);
        
        //time: 500ms
        clock.tick(1);
        
        sleep(15);
        
        registeredCounter = metricRegistry.getMetric(MetricFilters.ALL_FILTER, cxt);
        Assert.assertNotNull(registeredCounter);
        
        //time: 1000ms
        clock.tick(1);
        
        sleep(15);
        
        registeredCounter = metricRegistry.getMetric(MetricFilters.ALL_FILTER, cxt);
        Assert.assertNotNull(registeredCounter);
        
        //time: 1500ms
        clock.tick(1);
        
        // METRIC TIMEOUT HAPPENS INBETWEEN -> metric cleaned up by daemon
        
        sleep(15);
        
        registeredCounter = metricRegistry.getMetric(MetricFilters.ALL_FILTER, cxt);
        // metric is gone 
        Assert.assertNull(registeredCounter);
	}
	
	@Test
	public void testMetricEnableUpdate() {
	    MonitoringContext level1 = contextProvider.getNodeContext().getOrCreateContext("level1");
        Counter l1Counter = metricFactory.counter(level1, Counter.TYPE.DEFAULT);
        
        MonitoringContext level2 = contextProvider.getNodeContext().getOrCreateContext("level1", "level2");
        Counter l2Counter = metricFactory.counter(level2, Counter.TYPE.DEFAULT);
        
        Assert.assertFalse(l1Counter instanceof NoCounter);
        Assert.assertFalse(l2Counter instanceof NoCounter);
        
        level1.setEnabled(false);
        
        l1Counter = metricFactory.counter(level1, Counter.TYPE.DEFAULT);
        l2Counter = metricFactory.counter(level2, Counter.TYPE.DEFAULT);
        Assert.assertTrue(l1Counter instanceof NoCounter);
        Assert.assertTrue(l2Counter instanceof NoCounter);
	}
	
	@Test
    public void testGlobalDisable() {
        MonitoringContext level1 = contextProvider.getNodeContext().getOrCreateContext("level1");
        Counter l1Counter = metricFactory.counter(level1, Counter.TYPE.DEFAULT);
        
        MonitoringContext level2 = contextProvider.getNodeContext().getOrCreateContext("level1", "level2");
        Counter l2Counter = metricFactory.counter(level2, Counter.TYPE.DEFAULT);
        
        level1.setEnabled(true);
        
        Assert.assertFalse(l1Counter instanceof NoCounter);
        Assert.assertFalse(l2Counter instanceof NoCounter);
        
        provider.setGlobalEnable(false);
        
        l1Counter = metricFactory.counter(level1, Counter.TYPE.DEFAULT);
        l2Counter = metricFactory.counter(level2, Counter.TYPE.DEFAULT);
        Assert.assertTrue(l1Counter instanceof NoCounter);
        Assert.assertTrue(l2Counter instanceof NoCounter);
    }
	
	@Test
    public void testGlobalDisableThenEnable() {
        MonitoringContext level1 = contextProvider.getNodeContext().getOrCreateContext("level1");
        Counter l1Counter = metricFactory.counter(level1, Counter.TYPE.DEFAULT);
        
        MonitoringContext level2 = contextProvider.getNodeContext().getOrCreateContext("level1", "level2");
        Counter l2Counter = metricFactory.counter(level2, Counter.TYPE.DEFAULT);
        
        level1.setEnabled(true);
        
        Assert.assertFalse(l1Counter instanceof NoCounter);
        Assert.assertFalse(l2Counter instanceof NoCounter);
        
        provider.setGlobalEnable(false);
        
        l1Counter = metricFactory.counter(level1, Counter.TYPE.DEFAULT);
        l2Counter = metricFactory.counter(level2, Counter.TYPE.DEFAULT);
        Assert.assertTrue(l1Counter instanceof NoCounter);
        Assert.assertTrue(l2Counter instanceof NoCounter);
        
        provider.setGlobalEnable(true);
        
        l1Counter = metricFactory.counter(level1, Counter.TYPE.DEFAULT);
        l2Counter = metricFactory.counter(level2, Counter.TYPE.DEFAULT);
        Assert.assertFalse(l1Counter instanceof NoCounter);
        Assert.assertFalse(l2Counter instanceof NoCounter);
    }
	
	private static void sleep(long timeMillis) {
	    try {
            Thread.sleep(timeMillis);
        } catch (InterruptedException e) {
        }
	}
	
}
