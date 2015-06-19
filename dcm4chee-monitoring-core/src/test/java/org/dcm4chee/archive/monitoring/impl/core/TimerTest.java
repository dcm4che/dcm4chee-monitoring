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

package org.dcm4chee.archive.monitoring.impl.core;

import java.util.Arrays;
import java.util.Random;

import org.dcm4chee.archive.monitoring.impl.config.Configuration;
import org.dcm4chee.archive.monitoring.impl.config.MetricReservoirConfiguration;
import org.dcm4chee.archive.monitoring.impl.config.MetricReservoirConfiguration.RESERVOIR_TYPE;
import org.dcm4chee.archive.monitoring.impl.config.MonitoringBuilder;
import org.dcm4chee.archive.monitoring.impl.core.aggregate.Aggregate;
import org.dcm4chee.archive.monitoring.impl.core.aggregate.AggregateSnapshot;
import org.dcm4chee.archive.monitoring.impl.core.clocks.Clock;
import org.dcm4chee.archive.monitoring.impl.core.clocks.ClockProvider;
import org.dcm4chee.archive.monitoring.impl.core.clocks.Clocks;
import org.dcm4chee.archive.monitoring.impl.core.context.MonitoringContext;
import org.dcm4chee.archive.monitoring.impl.core.context.MonitoringContextProvider;
import org.dcm4chee.archive.monitoring.impl.core.reservoir.AggregatedReservoir;
import org.dcm4chee.archive.monitoring.impl.core.reservoir.AggregatedReservoirSnapshot;
import org.dcm4chee.archive.monitoring.impl.core.reservoir.ReservoirBuilder.START_SPECIFICATION;
import org.dcm4chee.archive.monitoring.impl.core.reservoir.RoundRobinReservoir;
import org.dcm4chee.archive.monitoring.impl.util.UnitOfTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 *
 */
public class TimerTest {
	private MetricProvider provider;
	private MonitoringContextProvider contextProvider;
	private MetricFactory metricFactory;
	
	@Before
	public void before() {
	    MetricReservoirConfiguration reservoirCfg = createDefaultMetricReservoirConfiguration();
		initProvider(Clocks.defaultClock(), reservoirCfg);
	}
	
	private MetricReservoirConfiguration createDefaultMetricReservoirConfiguration() {
	    MetricReservoirConfiguration reservoirCfg = new MetricReservoirConfiguration();
	    reservoirCfg.setType(RESERVOIR_TYPE.ROUND_ROBIN);
        reservoirCfg.setName("DEFAULT");
        // 60 sec in millis
        reservoirCfg.setResolutionStepSize(60l * 1000l);
        reservoirCfg.setResolutions(new long[] { 60l * 1000l, 60l * 1000l * 2l });
        reservoirCfg.setRetentions( new int[] { 5, 10 });
        reservoirCfg.setMaxRawValues(new int[] { 10000, 0});
        reservoirCfg.setStart(START_SPECIFICATION.CURRENT_MIN);
        return reservoirCfg;
	}
	
	private void initProvider(ClockProvider clock, MetricReservoirConfiguration... config) {
		Configuration cfg = new Configuration();
		cfg.setClockProvider(clock);
		cfg.setMetricReservoirConfigurations(Arrays.asList(config));
		
		provider = new MonitoringBuilder(cfg).createMetricProvider();
		
		contextProvider = provider.getMonitoringContextProvider();
		metricFactory = provider.getMetricFactory();
	}
	
	@After
	public void after() {
		contextProvider.disposeActiveContext();
	}
	
	@Test
	public void testTimer() {
	    
		MonitoringContext serviceCxt = contextProvider.createActiveContext("test", "service1");
		ManualClock clock = new ManualClock(0, 50, UnitOfTime.MILLISECONDS);
		TestReservoirBuilder reservoirBuilder = new TestReservoirBuilder(clock, 0, 1000, 1000);
		Timer serviceTimer = new TimerImpl(serviceCxt, reservoirBuilder.build(), clock);
		Timer.Split split = serviceTimer.time();
		try {
		    clock.tick();
		} finally {
			split.stop();
		}
		
		AggregatedReservoirSnapshot snapshot = serviceTimer.getSnapshot();
		Assert.assertEquals(50000000, snapshot.getMax());
	}
	
	@Test
	public void testTimerMultiThreaded() {
	    final ManualClock clock = new ManualClock(0, 50, UnitOfTime.MILLISECONDS);
	    
		MonitoringContext serviceCxt = contextProvider.createActiveContext("test", "service1");
		TestReservoirBuilder reservoirBuilder = new TestReservoirBuilder(clock, 0, 1000, 1000);
		final Timer serviceTimer = new TimerImpl(serviceCxt, reservoirBuilder.build(), clock);
		
		Runnable runnable = new Runnable() {
			public void run() {
				Timer.Split split = serviceTimer.time();
				try {
					clock.tick();
				} finally {
					split.stop();
				}
				
				AggregatedReservoirSnapshot snapshot = serviceTimer.getSnapshot();
				Assert.assertEquals(50000000, snapshot.getMax());
			}
		};
		
		new Thread(runnable).start();
		
		Timer.Split split = serviceTimer.time();
		try {
		    clock.tick();
		} finally {
			split.stop();
		}
		
		AggregatedReservoirSnapshot snapshot = serviceTimer.getSnapshot();
		Assert.assertEquals(50000000, snapshot.getMax());
		
	}
	
	@Test
	public void testIfSubContextTimerIsPropagated() {
		initProvider(Clocks.defaultClock(), createDefaultMetricReservoirConfiguration());
		
		MonitoringContext serviceCxt = contextProvider.createActiveContext("test", "service1");
		metricFactory.sumAggregate(serviceCxt);
		

		MonitoringContext subServiceCxt = contextProvider.getActiveContext().getOrCreateContext("subsystem1");
		subServiceCxt.attachContext(serviceCxt);
		
		Timer subServiceTimer = metricFactory.timer(subServiceCxt);
		Timer.Split subServiceTimerCxt = subServiceTimer.time();
		try {
			randomDurationMethod(2, 5);
		} finally {
			subServiceTimerCxt.stop();
		}
		
		Assert.assertEquals(1, subServiceTimer.getSnapshot().getValues(false).length);
		
		Aggregate serviceAggregate = metricFactory.sumAggregate(serviceCxt);
		Assert.assertTrue(((AggregateSnapshot)serviceAggregate.getSnapshot()).getMax()>0);
	}
	
	@Test
	public void testIfNestedSubContextTimerIsPropagated() {
		initProvider(Clocks.defaultClock(), createDefaultMetricReservoirConfiguration());
		
		MonitoringContext rootCxt = contextProvider.createActiveContext("test");
		metricFactory.sumAggregate(rootCxt);
		
		MonitoringContext serviceCxt = contextProvider.getActiveContext().getOrCreateContext("service1");
		serviceCxt.attachContext(rootCxt);
		metricFactory.sumAggregate(serviceCxt);
		
		MonitoringContext subServiceCxt1 = serviceCxt.getOrCreateContext("subsystem1");
		subServiceCxt1.attachContext(serviceCxt);
		Timer subServiceTimer1 = metricFactory.timer(subServiceCxt1);
		Timer.Split subServiceTimerSplit1 = subServiceTimer1.time();
		try {
			randomDurationMethod(2, 5);
		} finally {
			subServiceTimerSplit1.stop();
		}
		
		MonitoringContext subServiceCxt2 = serviceCxt.getOrCreateContext("subsystem2");
		subServiceCxt2.attachContext(serviceCxt);
		Timer subServiceTimer2 = metricFactory.timer(subServiceCxt2);
		Timer.Split subServiceTimerSplit2 = subServiceTimer2.time();
		try {
			randomDurationMethod(2, 5);
		} finally {
			subServiceTimerSplit2.stop();
		}
		
		Assert.assertEquals(1, subServiceTimer1.getSnapshot().getValues(false).length);
		Assert.assertEquals(1, subServiceTimer2.getSnapshot().getValues(false).length);
		
		Aggregate serviceAggregate = metricFactory.sumAggregate(serviceCxt);
		Assert.assertTrue(((AggregateSnapshot)serviceAggregate.getSnapshot()).getMax()>0);
		
		Aggregate rootAggregate = metricFactory.sumAggregate(rootCxt);
		Assert.assertTrue(((AggregateSnapshot)rootAggregate.getSnapshot()).getMax()>0);
	}
	
	@Test
	public void testIfAggregatorsAreFilled() {
		initProvider(Clocks.defaultClock(), createDefaultMetricReservoirConfiguration());
		
		MonitoringContext subsystemsContext = contextProvider.getNodeContext().getOrCreateContext("subsystems");
		metricFactory.sumAggregate(subsystemsContext);
		
		MonitoringContext rootCxt = contextProvider.createActiveContext("test");
		metricFactory.sumAggregate(rootCxt);
		
		MonitoringContext serviceCxt = contextProvider.getActiveContext().getOrCreateContext("service1");
		serviceCxt.attachContext(rootCxt);
		metricFactory.sumAggregate(serviceCxt);
		
		MonitoringContext subsystemCxt1 = serviceCxt.getOrCreateContext("subsystem1");
		subsystemCxt1.attachContext(subsystemsContext);
		subsystemCxt1.attachContext(serviceCxt);
	
		Timer subsystemTimer1 = metricFactory.timer(subsystemCxt1);
		try (Timer.Split subsystemTimerSplit1 = subsystemTimer1.time() ) {
			randomDurationMethod(2, 5);
		}

		MonitoringContext subsystemCxt2 = serviceCxt.getOrCreateContext("subsystem2");
		subsystemCxt1.attachContext(subsystemsContext);
		subsystemCxt2.attachContext(serviceCxt);
		
		Timer subsystemTimer2 = metricFactory.timer(subsystemCxt2);
		try (Timer.Split subsystemTimerSplit2 = subsystemTimer2.time()) {
			randomDurationMethod(2, 5);
		}
		
		Assert.assertEquals(1, subsystemTimer1.getSnapshot().getValues(false).length);
		Assert.assertEquals(1, subsystemTimer2.getSnapshot().getValues(false).length);
		
		Aggregate serviceAggregate = metricFactory.sumAggregate(serviceCxt);
		Assert.assertTrue(((AggregateSnapshot)serviceAggregate.getSnapshot()).getMax()>0);
		
		Aggregate rootAggregate = metricFactory.sumAggregate(rootCxt);
		Assert.assertTrue(((AggregateSnapshot)rootAggregate.getSnapshot()).getMax()>0);
		
		Aggregate subsystemsAggregate = metricFactory.sumAggregate(subsystemsContext);
		Assert.assertTrue(((AggregateSnapshot)subsystemsAggregate.getSnapshot()).getMax()>0);
	}
	
	/*
	 * Method that takes random amount of time to execute
	 */
	private void randomDurationMethod(int minDuration, int maxDuration) {
		Random random = new Random();
		int waitTime = random.nextInt(maxDuration - minDuration + 1) + minDuration;
		try {
			Thread.sleep(waitTime);
		} catch (InterruptedException e) {
			// swallow
		}
	}
	
	private static class TestReservoirBuilder {
        private final Clock clock;
        private long start;
        private long step;
        private long resolution;
        
        private TestReservoirBuilder(Clock clock, long start, long step, long resolution) {
            this.clock = clock;
            this.start = start;
            this.resolution = resolution;
            this.step = step;
        }
        
        public AggregatedReservoir build() {
            return new RoundRobinReservoir.Builder()
            .clock(clock).start(start).step(step)
            .addArchive(resolution, 1, 10000).build();
        }
        
    }
	
}
