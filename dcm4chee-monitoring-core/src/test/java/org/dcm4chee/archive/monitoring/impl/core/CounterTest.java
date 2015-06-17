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
import java.util.Iterator;
import java.util.List;

import org.dcm4chee.archive.monitoring.impl.config.Configuration;
import org.dcm4chee.archive.monitoring.impl.config.MetricReservoirConfiguration;
import org.dcm4chee.archive.monitoring.impl.config.MonitoringBuilder;
import org.dcm4chee.archive.monitoring.impl.config.MetricReservoirConfiguration.RESERVOIR_TYPE;
import org.dcm4chee.archive.monitoring.impl.core.clocks.ClockProvider;
import org.dcm4chee.archive.monitoring.impl.core.context.MonitoringContext;
import org.dcm4chee.archive.monitoring.impl.core.context.MonitoringContextProvider;
import org.dcm4chee.archive.monitoring.impl.core.reservoir.AggregatedReservoirSnapshot;
import org.dcm4chee.archive.monitoring.impl.core.reservoir.ReservoirBuilder.START_SPECIFICATION;
import org.dcm4chee.archive.monitoring.impl.util.UnitOfTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 *
 */
public class CounterTest {
	private MetricProvider provider;
	private MonitoringContextProvider contextProvider;
	private MetricFactory metricFactory;
	
	@Before
    public void before() {
	    ManualClock clock = new ManualClock(0, 1, UnitOfTime.SECONDS);
	    MetricReservoirConfiguration defReservoirCfg = createDefaultMetricReservoirConfiguration();
	    MetricReservoirConfiguration oneShotReservoirCfg = createOneShotMetricReservoirConfiguration();
        initProvider(clock, defReservoirCfg, oneShotReservoirCfg);
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
	
	private MetricReservoirConfiguration createOneShotMetricReservoirConfiguration() {
        MetricReservoirConfiguration reservoirCfg = new MetricReservoirConfiguration();
        reservoirCfg.setType(RESERVOIR_TYPE.OPEN_RESOLUTION);
        reservoirCfg.setName("ONE_SHOT");
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
	public void testOneShotCounterIncrease() {
		MonitoringContext serviceCxt = contextProvider.createActiveContext("test", "service1");
		Counter serviceCounter = metricFactory.counter(serviceCxt, Counter.TYPE.ONE_SHOT);
		serviceCounter.inc();
		serviceCounter.inc();
		
		AggregatedReservoirSnapshot snapshot = serviceCounter.getSnapshot();
		Assert.assertEquals(1, snapshot.getMin());
		Assert.assertEquals(2, snapshot.getMax());
		Assert.assertEquals(1.5, snapshot.getMean(), 0.0);
		Assert.assertEquals(0.7, snapshot.getStdDev(), 0.01);
	}
	
	@Test
    public void testDefaultCounterIncrease() {
        MonitoringContext serviceCxt = contextProvider.createActiveContext("test", "service1");
        Counter serviceCounter = metricFactory.counter(serviceCxt, Counter.TYPE.DEFAULT);
        serviceCounter.inc();
        serviceCounter.inc();
        
        AggregatedReservoirSnapshot snapshot = serviceCounter.getSnapshot();
        Assert.assertEquals(1, snapshot.getMin());
        Assert.assertEquals(2, snapshot.getMax());
        Assert.assertEquals(1.5, snapshot.getMean(), 0.0);
        Assert.assertEquals(0.7, snapshot.getStdDev(), 0.01);
    }
	
	@Test
    public void testDefaultCounterSnapshot() {
        MonitoringContext serviceCxt = contextProvider.createActiveContext("test", "service1");
        Counter serviceCounter = metricFactory.counter(serviceCxt, Counter.TYPE.DEFAULT);
        serviceCounter.inc();
        serviceCounter.inc();
        
        List<AggregatedReservoirSnapshot> snapshots = serviceCounter.getSnapshots(0, 60, 60);
        Assert.assertEquals(1, snapshots.size());
        Iterator<AggregatedReservoirSnapshot> it = snapshots.iterator();
        
        AggregatedReservoirSnapshot snapshot = it.next();
        Assert.assertEquals(1, snapshot.getMin());
        Assert.assertEquals(2, snapshot.getMax());
        Assert.assertEquals(1.5, snapshot.getMean(), 0.0);
        Assert.assertEquals(0.7, snapshot.getStdDev(), 0.01);
        
        snapshots = serviceCounter.getSnapshots(0, 120, 120);
        Assert.assertEquals(1, snapshots.size());
        it = snapshots.iterator();
        
        snapshot = it.next();
        Assert.assertEquals(1, snapshot.getMin());
        Assert.assertEquals(2, snapshot.getMax());
        Assert.assertEquals(1.5, snapshot.getMean(), 0.0);
        Assert.assertEquals(0.7, snapshot.getStdDev(), 0.01);
    }
	
}
