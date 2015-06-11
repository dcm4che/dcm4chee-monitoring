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

package org.dcm4chee.archive.monitoring.rs;



import java.util.Arrays;

import javax.inject.Inject;

import org.dcm4chee.archive.monitoring.impl.config.MetricReservoirConfiguration;
import org.dcm4chee.archive.monitoring.impl.config.MonitoringConfiguration;
import org.dcm4chee.archive.monitoring.impl.config.MetricReservoirConfiguration.RESERVOIR_TYPE;
import org.dcm4chee.archive.monitoring.impl.core.ApplicationMonitoringRegistry;
import org.dcm4chee.archive.monitoring.impl.core.Counter;
import org.dcm4chee.archive.monitoring.impl.core.MetricProvider;
import org.dcm4chee.archive.monitoring.impl.core.Timer;
import org.dcm4chee.archive.monitoring.impl.core.Util;
import org.dcm4chee.archive.monitoring.impl.core.context.MonitoringContext;
import org.dcm4chee.archive.monitoring.impl.core.reservoir.ReservoirBuilder.START_SPECIFICATION;
import org.dcm4chee.archive.monitoring.impl.util.UnitOfTime;
import org.dcm4chee.archive.monitoring.rs.MonitoringRS.TimeSpec;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 *
 */
@RunWith(Arquillian.class)
public class MonitoringRSTest {
	private static ManualClock clock;
	
	@Inject
	private MonitoringRS monitoringRS;
	
	@Inject @ApplicationMonitoringRegistry
	private MetricProvider metricProvider;
	
	@Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class)
                .addClass(MonitoringRS.class)
                .addClass(XmlStreamingOutputProvider.class)
                .addClass(JsonStreamingOutputProvider.class)
                .addClass(MetricProvider.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }
	
	@BeforeClass
	public static void before() {
		clock = new ManualClock(1l, 1l, UnitOfTime.NANOSECONDS);
		
		MonitoringConfiguration cfg = new MonitoringConfiguration();
		cfg.setClockProvider(clock);
		
		MetricReservoirConfiguration reservoirCfg = createDefaultMetricReservoirConfiguration();
        
		cfg.setReservoirConfigurations(Arrays.asList(reservoirCfg));
		cfg.createMetricProvider();
	}
	
	private static MetricReservoirConfiguration createDefaultMetricReservoirConfiguration() {
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
	
	@After
	public void after() {
		// dispose ALL contexts
		metricProvider.getMonitoringContextProvider().getNodeContext().dispose(true);
		clock.reset();
	}
	
	@Test
	public void testMonitoringRS() {
		MonitoringContext cxt = metricProvider.getMonitoringContextProvider().getNodeContext().getOrCreateContext("service", "counter");
		
		count(cxt, 2);
		
		MetricResponses metricList = monitoringRS.getMetricsInt(Util.createPath(cxt.getPath()), null, TimeSpec.ALL, null, true);
		Assert.assertEquals(1, metricList.getCounterResponses().size());
		Assert.assertEquals("2", metricList.getCounterResponses().iterator().next().getCount());
	}
	
	@Test
	public void testMonitoringRS2() {
		MonitoringContext cxt = metricProvider.getMonitoringContextProvider().getNodeContext().getOrCreateContext("service", "counter");
		
		count(cxt, 2);
		
		MetricResponses resp = monitoringRS.getMetricsInt(Util.createPath(cxt.getPath()), null, TimeSpec.ALL, null, true);
		Assert.assertEquals(1, resp.getCounterResponses().size());
		Assert.assertEquals("2", resp.getCounterResponses().iterator().next().getCount());
	}
	
	@Test
	public void testMonitoringRSWithMultipleMetrics() {
		MonitoringContext cxt = metricProvider.getMonitoringContextProvider().getNodeContext().getOrCreateContext("service");
		
		count(cxt.getOrCreateContext("counter"), 2);
		time(cxt.getOrCreateContext("timer"), 1);
		time(cxt.getOrCreateContext("timer"), 3);
		
		MetricResponses metricList = monitoringRS.getMetricsInt(Util.createPath(cxt.getPath()), null, TimeSpec.ALL, 
				org.dcm4chee.archive.monitoring.impl.util.UnitOfTime.MILLISECONDS, true);
		Assert.assertEquals(1, metricList.getCounterResponses().size());
		Assert.assertEquals("2", metricList.getCounterResponses().iterator().next().getCount());
		Assert.assertEquals(1, metricList.getTimerResponses().size());
//		Assert.assertArrayEquals( new long[] {1, 3}, metricList.getTimerResponses().iterator().next().getValues());
	}
	
	@Test
	public void testMonitoringRSWithMultipleMetricsAtMultipleTimes() {
		MonitoringContext cxt = metricProvider.getMonitoringContextProvider().getNodeContext().getOrCreateContext("service");
		
		count(cxt.getOrCreateContext("counter"), 2);
		time(cxt.getOrCreateContext("timer"), 1);
		time(cxt.getOrCreateContext("timer"), 3);
		
		MetricResponses metricList = monitoringRS.getMetricsInt(Util.createPath(cxt.getPath()), null, TimeSpec.ALL, 
				org.dcm4chee.archive.monitoring.impl.util.UnitOfTime.MILLISECONDS, true);
		Assert.assertEquals(1, metricList.getCounterResponses().size());
		Assert.assertEquals("2", metricList.getCounterResponses().iterator().next().getCount());
		Assert.assertEquals(1, metricList.getTimerResponses().size());
//		Assert.assertArrayEquals( new long[] {1, 3}, metricList.getTimerResponses().iterator().next().getValues());
		
		time(cxt.getOrCreateContext("timer"), 5);
		count(cxt.getOrCreateContext("counter"), 3);
		
		metricList = monitoringRS.getMetricsInt(Util.createPath(cxt.getPath()), null, TimeSpec.ALL, 
				org.dcm4chee.archive.monitoring.impl.util.UnitOfTime.MILLISECONDS, true);
		Assert.assertEquals(1, metricList.getCounterResponses().size());
		Assert.assertEquals("5", metricList.getCounterResponses().iterator().next().getCount());
		Assert.assertEquals(1, metricList.getTimerResponses().size());
//		Assert.assertArrayEquals( new long[] {1, 3, 5}, metricList.getTimerResponses().iterator().next().getValues());
	}
	
	@Test
	public void testMonitoringRSWithCounterMetricTypeFilter() {
		MonitoringContext cxt = metricProvider.getMonitoringContextProvider().getNodeContext().getOrCreateContext("service");
		
		count(cxt.getOrCreateContext("counter"), 2);
		time(cxt.getOrCreateContext("timer"), 1);
		time(cxt.getOrCreateContext("timer"), 3);
		
		MetricResponses metricList = monitoringRS.getMetricsInt(Util.createPath(cxt.getPath()), "counter", TimeSpec.ALL, 
				org.dcm4chee.archive.monitoring.impl.util.UnitOfTime.MILLISECONDS, true);
		Assert.assertEquals(1, metricList.getCounterResponses().size());
		Assert.assertEquals("2", metricList.getCounterResponses().iterator().next().getCount());
		Assert.assertEquals(0, metricList.getTimerResponses().size());
	}
	
	@Test
	public void testMonitoringRSWithTimerMetricTypeFilter() {
		MonitoringContext cxt = metricProvider.getMonitoringContextProvider().getNodeContext().getOrCreateContext("service");
		
		count(cxt.getOrCreateContext("counter"), 2);
		time(cxt.getOrCreateContext("timer"), 1);
		time(cxt.getOrCreateContext("timer"), 3);
		
		MetricResponses metricList = monitoringRS.getMetricsInt(Util.createPath(cxt.getPath()), "timer", TimeSpec.ALL, 
				org.dcm4chee.archive.monitoring.impl.util.UnitOfTime.MILLISECONDS, true);
		Assert.assertEquals(0, metricList.getCounterResponses().size());
		Assert.assertEquals(1, metricList.getTimerResponses().size());
//		Assert.assertArrayEquals( new long[] {1, 3}, metricList.getTimerResponses().iterator().next().getValues());
	}
	
	@Test
	public void testMonitoringRSWithInvalidMetricTypeFilter() {
		MonitoringContext cxt = metricProvider.getMonitoringContextProvider().getNodeContext().getOrCreateContext("service");
		
		count(cxt.getOrCreateContext("counter"), 2);
		time(cxt.getOrCreateContext("timer"), 1);
		time(cxt.getOrCreateContext("timer"), 3);
		
		MetricResponses metricList = monitoringRS.getMetricsInt(Util.createPath(cxt.getPath()), "fantasticMetric", TimeSpec.ALL, 
				org.dcm4chee.archive.monitoring.impl.util.UnitOfTime.MILLISECONDS, true);
		// return ALL metrics for invalid type filter
		Assert.assertEquals(1, metricList.getCounterResponses().size());
		Assert.assertEquals(1, metricList.getTimerResponses().size());
	}
	
	@Test
	public void testMonitoringRSWithTimeSpec() {
		MonitoringContext cxt = metricProvider.getMonitoringContextProvider().getNodeContext().getOrCreateContext("service");
		
		time(cxt.getOrCreateContext("timer"), 1);
		time(cxt.getOrCreateContext("timer"), 3);
		
		TimeSpec timeSpec = TimeSpec.createFromTimeMillis(1, 10, 60);
		MetricResponses metricList = monitoringRS.getMetricsInt(Util.createPath(cxt.getPath()), null, timeSpec, 
				org.dcm4chee.archive.monitoring.impl.util.UnitOfTime.NANOSECONDS, true);
		
		Assert.assertEquals(1, metricList.getTimerResponses().size());
		TimerResponse snapshot = metricList.getTimerResponses().iterator().next();
		Assert.assertEquals("3", snapshot.getMax());
		Assert.assertEquals("1", snapshot.getMin());
//		Assert.assertEquals(2, snapshot.size());
	}
	
	private void time(MonitoringContext timerCxt, long ticks) {
		Timer timer = metricProvider.getMetricFactory().timer(timerCxt);
		Timer.Split split = timer.time();
		try {
			clock.tick(ticks);
		} finally {
			split.stop();
		}
	}
	
	private void count(MonitoringContext counterCxt, long count) {
		Counter counter = metricProvider.getMetricFactory().counter(counterCxt, Counter.TYPE.DEFAULT);
		counter.inc(count);
	}
	
}
