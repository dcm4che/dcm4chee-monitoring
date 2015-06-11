package org.dcm4chee.archive.monitoring.impl.core;

import java.util.List;

import org.dcm4chee.archive.monitoring.impl.core.reservoir.AggregatedReservoirSnapshot;

public interface Meter extends Metric, Sampling<AggregatedReservoirSnapshot> {
    
    public static enum METER_CONFIGURATION {
        OPEN_RESOLUTION,
        ONE_SEC_RESOLUTION__5_SEC_HISTORY
    };

    void mark();
    
    void mark(long n);
    
    List<AggregatedReservoirSnapshot> getSnapshots(long start, long end, long resolution);
    
}
