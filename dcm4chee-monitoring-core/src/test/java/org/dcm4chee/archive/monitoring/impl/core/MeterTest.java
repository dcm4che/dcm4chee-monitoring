package org.dcm4chee.archive.monitoring.impl.core;


import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.dcm4chee.archive.monitoring.impl.config.Configuration;
import org.dcm4chee.archive.monitoring.impl.config.MetricReservoirConfiguration;
import org.dcm4chee.archive.monitoring.impl.config.MetricReservoirConfiguration.RESERVOIR_TYPE;
import org.dcm4chee.archive.monitoring.impl.config.MonitoringBuilder;
import org.dcm4chee.archive.monitoring.impl.core.Meter.TYPE;
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

public class MeterTest {
    private MetricProvider provider;
    private MonitoringContextProvider contextProvider;
    private MetricFactory metricFactory;
    
    private ManualClock clock;
    
    @Before
    public void before() {
        clock = new ManualClock(0, 100, UnitOfTime.MILLISECONDS);
        
        MetricReservoirConfiguration reservoirCfg = createDefaultMetricReservoirConfiguration();
        MetricReservoirConfiguration openReservoirCfg = createOpenMetricReservoirConfiguration();
        
        initProvider(clock, reservoirCfg, openReservoirCfg );
    }
    
    private MetricReservoirConfiguration createDefaultMetricReservoirConfiguration() {
        MetricReservoirConfiguration reservoirCfg = new MetricReservoirConfiguration();
        reservoirCfg.setType(RESERVOIR_TYPE.ROUND_ROBIN);
        reservoirCfg.setName("ONE_SEC_RESOLUTION__5_SEC_HISTORY");
        reservoirCfg.setResolutionStepSize(1000);
        reservoirCfg.setResolutions(new long[] { 1000 });
        reservoirCfg.setRetentions( new int[] { 5});
        reservoirCfg.setValueReservoirs(new boolean[] { false});
        reservoirCfg.setStart(START_SPECIFICATION.CURRENT_SEC);
        return reservoirCfg;
    }
    
    private MetricReservoirConfiguration createOpenMetricReservoirConfiguration() {
        MetricReservoirConfiguration reservoirCfg = new MetricReservoirConfiguration();
        reservoirCfg.setType(RESERVOIR_TYPE.OPEN_RESOLUTION);
        reservoirCfg.setName("OPEN_RESOLUTION");
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
    public void testOpenResolutionMeter() {
        MonitoringContext serviceCxt = contextProvider.createActiveContext("test", "service1");
        Meter meter = metricFactory.meter(serviceCxt, TYPE.OPEN_RESOLUTION);
        meter.mark();
        
        clock.tick(2);
        
        meter.mark(99);
        
        AggregatedReservoirSnapshot snapshot = meter.getSnapshot();
        
        Assert.assertEquals(0, snapshot.getStart());
        Assert.assertEquals(200, snapshot.getEnd());
        Assert.assertEquals(100, snapshot.getSum());
        
        // 100 marks for 200 msecs => mean rate: 500 marks / 1sec
        Assert.assertEquals(500.0, snapshot.getMeanRate(1, UnitOfTime.SECONDS), 0.0);
        // 100 marks for 200 msecs => mean rate: 5marks / 10ms
        Assert.assertEquals(5.0, snapshot.getMeanRate(10, UnitOfTime.MILLISECONDS), 0.0);
    }
    
    @Test
    public void test1SecResolutionMeter() {
        MonitoringContext serviceCxt = contextProvider.createActiveContext("test", "service1");
        Meter meter = metricFactory.meter(serviceCxt, TYPE.ONE_SEC_RESOLUTION__5_SEC_HISTORY);
        meter.mark(5);
        
        clock.tick(1);
        
        meter.mark(10);
        
        clock.tick(1);
        
        AggregatedReservoirSnapshot snapshot = meter.getSnapshot();
        
        Assert.assertEquals(0, snapshot.getStart());
        Assert.assertEquals(1000, snapshot.getEnd());
        Assert.assertEquals(15, snapshot.getSum());
        
        Assert.assertEquals(15.0, snapshot.getMeanRate(1, UnitOfTime.SECONDS), 0.0);
        Assert.assertEquals(30.0, snapshot.getMeanRate(2, UnitOfTime.SECONDS), 0.0);
        Assert.assertEquals(1.5, snapshot.getMeanRate(100, UnitOfTime.MILLISECONDS), 0.0);
    }
    
    @Test
    public void test1SecResolutionMeter2() {
        MonitoringContext serviceCxt = contextProvider.createActiveContext("test", "service1");
        Meter meter = metricFactory.meter(serviceCxt, TYPE.ONE_SEC_RESOLUTION__5_SEC_HISTORY);
        meter.mark(5);
        
        clock.tick(1);
        
        meter.mark(10);
        
        clock.tick(1);
        
        List<AggregatedReservoirSnapshot> snapshots = meter.getSnapshots(0, 1000, 1000);
        
        
        Assert.assertEquals(1, snapshots.size());
        
        AggregatedReservoirSnapshot snapshot = snapshots.iterator().next();
        Assert.assertEquals(0, snapshot.getStart());
        Assert.assertEquals(1000, snapshot.getEnd());
        Assert.assertEquals(15, snapshot.getSum());
        
        Assert.assertEquals(15.0, snapshot.getMeanRate(1, UnitOfTime.SECONDS), 0.0);
        Assert.assertEquals(30.0, snapshot.getMeanRate(2, UnitOfTime.SECONDS), 0.0);
        Assert.assertEquals(1.5, snapshot.getMeanRate(100, UnitOfTime.MILLISECONDS), 0.0);
    }
    
    @Test
    public void test1SecResolutionMeter3() {
        MonitoringContext serviceCxt = contextProvider.createActiveContext("test", "service1");
        Meter meter = metricFactory.meter(serviceCxt, TYPE.ONE_SEC_RESOLUTION__5_SEC_HISTORY);
        
        //time: 0sec
        meter.mark(5);
        
        //time : 0.1sec
        clock.tick(1);
        
        meter.mark(10);
        
        // time: 0.2sec
        clock.tick(1);
        
        meter.mark(10);
        
        // time: 1.2sec
        clock.tick(10);
        meter.mark(50);
        
        List<AggregatedReservoirSnapshot> snapshots = meter.getSnapshots(0, 1000, 1000);
        Assert.assertEquals(1, snapshots.size());
        AggregatedReservoirSnapshot snapshot = snapshots.iterator().next();
        Assert.assertEquals(0, snapshot.getStart());
        Assert.assertEquals(1000, snapshot.getEnd());
        Assert.assertEquals(25, snapshot.getSum());
        
        snapshots = meter.getSnapshots(0, 2000, 1000);
        Assert.assertEquals(2, snapshots.size());
        Iterator<AggregatedReservoirSnapshot> it = snapshots.iterator();
        snapshot = it.next();
        Assert.assertEquals(0, snapshot.getStart());
        Assert.assertEquals(1000, snapshot.getEnd());
        Assert.assertEquals(25, snapshot.getSum());
        snapshot = it.next();
        Assert.assertEquals(1001, snapshot.getStart());
        Assert.assertEquals(2000, snapshot.getEnd());
        Assert.assertEquals(50, snapshot.getSum());
    }
    
}
