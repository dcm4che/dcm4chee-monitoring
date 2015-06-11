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

import java.util.List;

import org.dcm4chee.archive.monitoring.impl.core.Meter.METER_CONFIGURATION;
import org.dcm4chee.archive.monitoring.impl.core.aggregate.Aggregate;
import org.dcm4chee.archive.monitoring.impl.core.aggregate.SumAggregate;
import org.dcm4chee.archive.monitoring.impl.core.clocks.Clock;
import org.dcm4chee.archive.monitoring.impl.core.context.MonitoringContext;
import org.dcm4chee.archive.monitoring.impl.core.context.MonitoringContextTree;
import org.dcm4chee.archive.monitoring.impl.core.metric.NoCounter;
import org.dcm4chee.archive.monitoring.impl.core.metric.NoMeter;
import org.dcm4chee.archive.monitoring.impl.core.metric.NoSumAggregate;
import org.dcm4chee.archive.monitoring.impl.core.metric.NoTimer;
import org.dcm4chee.archive.monitoring.impl.core.reservoir.ForwardingReservoir;
import org.dcm4chee.archive.monitoring.impl.core.reservoir.Reservoir;
import org.dcm4chee.archive.monitoring.impl.core.reservoir.ReservoirBuilderFactory;



/**
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 *
 */
public class MetricFactory {
	private final MonitoringContextTree metricRegistry;
	private final Clock clock;
	private final ReservoirBuilderFactory reservoirFactory;
	
	public MetricFactory(MonitoringContextTree metricRegistry, Clock clock, ReservoirBuilderFactory reservoirFactory) {
		this.metricRegistry = metricRegistry;
		this.clock = clock;
		this.reservoirFactory = reservoirFactory;
	}
	
	public <T extends MonitoredObject> T register(MonitoringContext context, T monitoredObject) throws IllegalArgumentException {
		return null;
	}
	
	public Aggregate sumAggregate(MonitoringContext monitoringContext) {
		final Aggregate metric = metricRegistry.getMetric(Aggregate.class, monitoringContext);
		if (metric != null) {
			return metric;
		} else {
			Aggregate parentAggregate = metricRegistry.getAggregate(monitoringContext.getParentContext());
			return createAggregateInt(parentAggregate, monitoringContext);
		}
	}
	
	public Timer timer(MonitoringContext monitoringContext) {
		return timer(monitoringContext, Timer.TYPE.DEFAULT);
	}
	
	public Timer timer(MonitoringContext monitoringContext, Timer.TYPE type) {
		final Timer metric = metricRegistry.getMetric(Timer.class, monitoringContext);
		if (metric != null) {
			return metric;
		} else {
			Aggregate parentAggregate = metricRegistry.getAggregate(monitoringContext.getParentContext());
			
			List<MonitoringContext> attachedContexts = monitoringContext.getAttachedContexts();
			if (!attachedContexts.isEmpty()) {
				ForwardingReservoir forwarding = new ForwardingReservoir();
				forwarding.addReservoir(parentAggregate);
				
				for (MonitoringContext attachedContext : attachedContexts) {
					Aggregate attachedAggregate = metricRegistry.getAggregate(attachedContext);
					forwarding.addReservoir(attachedAggregate);
				}
				return createTimerInt(forwarding, monitoringContext, type);
			} else {
				return createTimerInt(parentAggregate, monitoringContext, type);
			}
		}
	}
	
	public Counter counter(MonitoringContext monitoringContext, Counter.TYPE type) {
        final Counter counter = metricRegistry.getMetric(Counter.class, monitoringContext);
        if (counter != null) {
            return counter;
        } else {
            Counter parentTimer = metricRegistry.getParentMetric(Counter.class, monitoringContext);
            return createCounterInt(parentTimer, monitoringContext, type);
        }
    }
	
	public Meter meter(MonitoringContext monitoringContext, METER_CONFIGURATION meterCfg) {
        final Meter meter = metricRegistry.getMetric(Meter.class, monitoringContext);
        if (meter != null) {
            return meter;
        } else {
            return createMeterInt(monitoringContext, meterCfg);
        }
    }
	
	private Aggregate createAggregateInt(Aggregate parentAggregate, MonitoringContext context) {
        Aggregate aggregate = null;
        if (!context.isEnabled()) {
            aggregate = NoSumAggregate.INSTANCE;
        } else {
            aggregate = new SumAggregate(context, parentAggregate,
                    reservoirFactory.createAggregateReservoirContainer());
        }
        
        metricRegistry.register(context, aggregate);
        
        return aggregate;
	}
	
	private Timer createTimerInt(Reservoir reservoir, MonitoringContext context, Timer.TYPE type) {
	    Timer timer = null;
	    if(!context.isEnabled()) {
	        timer = NoTimer.INSTANCE;
	    } else {
            if (reservoir != null) {
                timer = new AggregatableTimerImpl(context,
                        reservoirFactory.createTimerReservoirContainer(type),
                        clock, reservoir);
            } else {
                timer = new TimerImpl(context,
                        reservoirFactory.createTimerReservoirContainer(type),
                        clock);
            }
	        
	    }
	  
		metricRegistry.register(context, timer);
		
		return timer;
	}
	
	private Counter createCounterInt(Counter parentCounter, MonitoringContext context, Counter.TYPE type) {
        Counter counter = null;
        if(!context.isEnabled()) {
            counter = NoCounter.INSTANCE;
        } else {
            if (parentCounter != null) {
                counter = new HierarchicalCounter(context, parentCounter,
                        reservoirFactory.createCounterReservoirContainer(type),
                        clock);
            } else {
                counter = new CounterImpl(context,
                        reservoirFactory.createCounterReservoirContainer(type),
                        clock);
            }
        }
        
        metricRegistry.register(context, counter);
        
        return counter;
    }
	
	private Meter createMeterInt(MonitoringContext context, METER_CONFIGURATION meterCfg) {
	    Meter meter = null;
	    if(!context.isEnabled()) {
	        meter = NoMeter.INSTANCE;
	    } else {
	        meter = new MeterImpl(context, reservoirFactory.createMeterReservoirContainer(meterCfg), clock);
	    }

        metricRegistry.register(context, meter);
        return meter;
    }
	
}
