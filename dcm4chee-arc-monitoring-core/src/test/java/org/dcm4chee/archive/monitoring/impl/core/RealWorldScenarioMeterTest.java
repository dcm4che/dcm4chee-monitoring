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
import java.util.List;
import java.util.TimerTask;

import org.dcm4chee.archive.monitoring.impl.config.MetricReservoirConfiguration;
import org.dcm4chee.archive.monitoring.impl.config.MonitoringConfiguration;
import org.dcm4chee.archive.monitoring.impl.core.Meter.METER_CONFIGURATION;
import org.dcm4chee.archive.monitoring.impl.core.clocks.Clock;
import org.dcm4chee.archive.monitoring.impl.core.clocks.ClockProvider;
import org.dcm4chee.archive.monitoring.impl.core.clocks.Clocks;
import org.dcm4chee.archive.monitoring.impl.core.context.MonitoringContext;
import org.dcm4chee.archive.monitoring.impl.core.context.MonitoringContextProvider;
import org.dcm4chee.archive.monitoring.impl.core.reservoir.AggregatedReservoirSnapshot;
import org.junit.After;
import org.junit.Before;

public class RealWorldScenarioMeterTest {
    private MetricProvider provider;
    private MonitoringContextProvider contextProvider;
    private MetricFactory metricFactory;
    
    private Clock clock;
    
    @Before
    public void before() {
        clock = Clocks.defaultClock();
        
        MetricReservoirConfiguration reservoirCfg = new MetricReservoirConfiguration();
        reservoirCfg.setName("");
        reservoirCfg.setResolutionStepSize(60);
        reservoirCfg.setResolutions(new long[] { 60, 60 * 2 });
        reservoirCfg.setRetentions( new int[] { 5, 10 });
        
        initProvider(reservoirCfg, clock);
    }
    
    private void initProvider(MetricReservoirConfiguration config, ClockProvider clock) {
        MonitoringConfiguration cfg = new MonitoringConfiguration();
        cfg.setClockProvider(clock);
        cfg.setReservoirConfigurations(Arrays.asList(config));
        
        provider = cfg.createMetricProvider();
        
        contextProvider = provider.getMonitoringContextProvider();
        metricFactory = provider.getMetricFactory();
    }
    
    @After
    public void after() {
        contextProvider.disposeActiveInstanceContext();
    }
    
//    @Test
    public void test1SecResolutionMeter() {
        MonitoringContext serviceCxt = contextProvider.createActiveInstanceContext("test", "service1");
        final Meter meter = metricFactory.meter(serviceCxt, METER_CONFIGURATION.ONE_SEC_RESOLUTION__5_SEC_HISTORY);
        
        TimerTask task = new TimerTask() {
            
            @Override
            public void run() {
//                long start = System.nanoTime();
                meter.mark();
//                long duration = System.nanoTime() - start;
//                System.out.println(duration);
            }
        };
        
        TimerTask queryTask = new TimerTask() {
            
            @Override
            public void run() {
                long now = Util.getTimeInSecondResolution(System.currentTimeMillis());
                List<AggregatedReservoirSnapshot> snapshots = meter.getSnapshots(now - 999, now - 1, 1000);
//                Date now = new Date();
                System.out.println(snapshots.size());
                AggregatedReservoirSnapshot snapshot = snapshots.iterator().next();
                System.out.println("[Now: " + now + "] [" + snapshot.getStart() + " - " + snapshot.getEnd()+ "] Rate: " + snapshot.getSum() + " mean: " + snapshot.getMean());
            }
        };
        
        java.util.Timer timer = new java.util.Timer();
        timer.scheduleAtFixedRate(task, 1, 10);
        timer.scheduleAtFixedRate(queryTask, 1005, 1000);
        
        
        
        try {
            Thread.sleep(25000);
        } catch (InterruptedException e) {
        }
        timer.cancel();
        
        
        
    }
}
