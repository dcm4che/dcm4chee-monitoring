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

package org.dcm4chee.archive.monitoring.impl.core.reservoir;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dcm4chee.archive.monitoring.impl.config.MetricReservoirConfiguration;
import org.dcm4chee.archive.monitoring.impl.config.MetricReservoirConfiguration.RESERVOIR_TYPE;
import org.dcm4chee.archive.monitoring.impl.core.Counter;
import org.dcm4chee.archive.monitoring.impl.core.Meter.METER_CONFIGURATION;
import org.dcm4chee.archive.monitoring.impl.core.Timer;
import org.dcm4chee.archive.monitoring.impl.core.Util;
import org.dcm4chee.archive.monitoring.impl.core.clocks.Clock;
import org.dcm4chee.archive.monitoring.impl.core.reservoir.ReservoirBuilder.START_SPECIFICATION;

/**
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 *
 */
public class ReservoirBuilderFactory {
	private final Map<String,ReservoirBuilder> reservoirBuilders = new HashMap<>();
	private final Clock clock;
	
	public ReservoirBuilderFactory(List<MetricReservoirConfiguration> reservoirConfigurations, Clock clock) {
		this.clock = clock;
		init(reservoirConfigurations);
	}
	
	private void init(List<MetricReservoirConfiguration> reservoirConfigurations) {
	    for(MetricReservoirConfiguration reservoirCfg : reservoirConfigurations) {
	        String name = reservoirCfg.getName();
	        RESERVOIR_TYPE type = reservoirCfg.getType();
	        
	        ReservoirBuilder builder = null;
	        switch(type) {
            case OPEN_RESOLUTION:
                builder = new OneValueReservoirBuilderImpl()
                    .clock(clock);
                break;
            case ROUND_ROBIN:
                builder = new DefaultReservoirBuilderImpl()
                    .clock(clock)
                    .resolutionStepSize(reservoirCfg.getResolutionStepSize())
                    .resolutions(reservoirCfg.getResolutions())
                    .valueReservoirs(reservoirCfg.getValueReservoirs())
                    .retentions(reservoirCfg.getRetentions())
                    .start(reservoirCfg.getStart());
                break;
            default:
                break;
	        
	        }
	        
	        reservoirBuilders.put(name, builder);
	    }
	}
	
	public AggregatedReservoir createTimerReservoirContainer(Timer.TYPE type) {
	    ReservoirBuilder builder = null;
	    switch (type) {
			case ONE_SHOT:
			    builder = reservoirBuilders.get(type.toString());
			    break;
			case DEFAULT:
			    builder = reservoirBuilders.get(type.toString());
			    break;
			default:
				throw new IllegalArgumentException("Unknown timer type " + type);
		}
	    
	    if(builder != null ) {
	        return builder.build();
	    } else {
	        throw new IllegalArgumentException("No reservoir builder registered for type " + type);
	    }
	   
	}
	
	public AggregatedReservoir createCounterReservoirContainer(Counter.TYPE type) {
	    ReservoirBuilder builder = null;
        switch (type) {
            case ONE_SHOT:
                builder = reservoirBuilders.get(type.toString());
                break;
            case DEFAULT:
                builder = reservoirBuilders.get(type.toString());
                break;
            default:
                throw new IllegalArgumentException("Unknown timer type " + type);
        }
        
        if(builder != null ) {
            return builder.build();
        } else {
            throw new IllegalArgumentException("No reservoir builder registered for type " + type);
        }
    }
	
	public AggregatedReservoir createMeterReservoirContainer(METER_CONFIGURATION meterCfg) {
	    ReservoirBuilder builder = null;
        switch (meterCfg) {
            case OPEN_RESOLUTION:
                builder = reservoirBuilders.get(meterCfg.toString());
                break;
            case ONE_SEC_RESOLUTION__5_SEC_HISTORY:
                builder = reservoirBuilders.get(meterCfg.toString());
                break;
            default:
                throw new IllegalArgumentException("Unknown meter configuration " + meterCfg);
        }
        
        if(builder != null ) {
            return builder.build();
        } else {
            throw new IllegalArgumentException("No reservoir builder registered for type " + meterCfg);
        }
    }
	
	public AggregatedReservoir createAggregateReservoirContainer() {
	    ReservoirBuilder builder = null;
        builder = reservoirBuilders.get("DEFAULT");
                
        if(builder != null ) {
            return builder.build();
        } else {
            throw new IllegalArgumentException("No reservoir builder registered for type ");
        }
	}
	
	private static class OneValueReservoirBuilderImpl implements ReservoirBuilder {
	    private Clock clock;
       
        @Override
        public ReservoirBuilder clock(Clock clock) {
            this.clock  = clock;
            return this;
        }

        @Override
        public ReservoirBuilder resolutionStepSize(long stepSize) {
            return this;
        }

        @Override
        public ReservoirBuilder resolutions(long[] resolutions) {
            return this;
        }

        @Override
        public ReservoirBuilder valueReservoirs(boolean[] valueReservoirs) {
            return this;
        }

        @Override
        public ReservoirBuilder retentions(int[] retentions) {
            return this;
        }

        @Override
        public AggregatedReservoir build() {
            return new OneValueReservoir(clock);
        }

        @Override
        public ReservoirBuilder start(START_SPECIFICATION start) {
            return this;
        }
	    
	}
	
	private static class DefaultReservoirBuilderImpl implements ReservoirBuilder {
        private long reservoirResolutionStepSize;
        private long[] reservoirResolutions;
        private boolean[] valueReservoirs;
        private int[] reservoirRetentions;
        private Clock clock;
        private START_SPECIFICATION start;
        
        public DefaultReservoirBuilderImpl clock(Clock clock) {
            this.clock = clock;
            return this;
        }
        
        public AggregatedReservoir build() {
            RoundRobinReservoir.Builder reservoirBuilder  = new RoundRobinReservoir.Builder().clock(clock)
                    .start(createStartTimeMillis(start, clock))
                    .step(reservoirResolutionStepSize);
        
            for(int i = 0; i < reservoirResolutions.length; i++) {
                reservoirBuilder.addArchive(reservoirResolutions[i], reservoirRetentions[i], valueReservoirs[i]);
            }
            
            return reservoirBuilder.build();
        }

        @Override
        public ReservoirBuilder resolutionStepSize(long stepSize) {
            this.reservoirResolutionStepSize = stepSize;
            return this;
        }

        @Override
        public ReservoirBuilder resolutions(long[] resolutions) {
            this.reservoirResolutions = resolutions;
            return this;
        }

        @Override
        public ReservoirBuilder retentions(int[] retentions) {
            this.reservoirRetentions = retentions;
            return this;
        }

        @Override
        public ReservoirBuilder valueReservoirs(boolean[] valueReservoirs) {
            this.valueReservoirs = valueReservoirs;
            return this;
        }

        @Override
        public ReservoirBuilder start(START_SPECIFICATION start) {
            this.start = start;
            return this;
        }
    }	
	
	private static long createStartTimeMillis(START_SPECIFICATION start, Clock clock) {
	    switch(start) {
        case CURRENT_MIN:
            return Util.getTimeInMinuteResolution(clock.getTime());
        case CURRENT_SEC:
            return Util.getTimeInSecondResolution(clock.getTime());
        default:
            return 0;
	    }
	}
	
}
