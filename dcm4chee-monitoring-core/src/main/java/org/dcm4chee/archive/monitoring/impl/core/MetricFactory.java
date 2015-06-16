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

import org.dcm4chee.archive.monitoring.impl.core.Meter.TYPE;
import org.dcm4chee.archive.monitoring.impl.core.aggregate.Aggregate;
import org.dcm4chee.archive.monitoring.impl.core.aggregate.ForwardOnlyAggregate;
import org.dcm4chee.archive.monitoring.impl.core.aggregate.SimpleAggregate;
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
	
	public Aggregate simpleAggregate(MonitoringContext cxt) {
        final Aggregate metric = metricRegistry.getMetric(Aggregate.class, cxt);
        if (metric != null) {
            return metric;
        } else {
            ForwardingReservoir forwarding = addAttachedContexts(cxt, null);
            return createAggregateInt(forwarding, cxt, "SIMPLE");
        }
    }
	
	public Aggregate simpleAggregateWithForward(MonitoringContext cxt, MonitoringContext forwardCxt) {
        final Aggregate metric = metricRegistry.getMetric(Aggregate.class, cxt);
        if (metric != null) {
            return metric;
        } else {
            ForwardingReservoir forwarding = createForward(forwardCxt);
            forwarding = addAttachedContexts(cxt, forwarding);
            
            return createAggregateInt(forwarding, cxt, "SIMPLE");
        }
    }
	
	public Aggregate forwardAggregate(MonitoringContext cxt, MonitoringContext forwardCxt) {
	    final Aggregate metric = metricRegistry.getMetric(Aggregate.class, cxt);
        if (metric != null) {
            return metric;
        } else {
            ForwardingReservoir forwarding = createForward(forwardCxt);
            forwarding = addAttachedContexts(cxt, forwarding);
            return createAggregateInt(forwarding, cxt, "FORWARDING");
        }
	}
	
	public Aggregate sumAggregate(MonitoringContext monitoringContext) {
		final Aggregate metric = metricRegistry.getMetric(Aggregate.class, monitoringContext);
		if (metric != null) {
			return metric;
		} else {
		    ForwardingReservoir forwarding = addAttachedContexts(monitoringContext, null);
			return createAggregateInt(forwarding, monitoringContext, "SUM");
		}
	}
	
	public Aggregate sumAggregateWithForward(MonitoringContext cxt, MonitoringContext forwardCxt) {
        final Aggregate metric = metricRegistry.getMetric(Aggregate.class, cxt);
        if (metric != null) {
            return metric;
        } else {
            ForwardingReservoir forwarding = createForward(forwardCxt);
            forwarding = addAttachedContexts(cxt, forwarding);
            
            return createAggregateInt(forwarding, cxt, "SUM");
        }
    }
	
	public Timer timer(MonitoringContext monitoringContext) {
		return timer(monitoringContext, Timer.TYPE.DEFAULT);
	}
	
	public Timer timerOnlyForward(MonitoringContext cxt, MonitoringContext forwardCxt) {
	    final Timer metric = metricRegistry.getMetric(Timer.class, cxt);
        if (metric != null) {
            return metric;
        } else {
            ForwardingReservoir forwarding = createForward(forwardCxt);
            forwarding = addAttachedContexts(cxt, forwarding);
            
            Timer t = new ForwardOnlyTimer(cxt, forwarding, clock);
            metricRegistry.register(cxt, t);
            return t;
        }

	}
	
	public Timer timerWithForward(MonitoringContext cxt, Timer.TYPE type, MonitoringContext forwardCxt) {
	    final Timer metric = metricRegistry.getMetric(Timer.class, cxt);
        if (metric != null) {
            return metric;
        } else {
            ForwardingReservoir forwarding = createForward(forwardCxt);
            forwarding = addAttachedContexts(cxt, forwarding);
            
            return createTimerInt(forwarding, cxt, type);
        }
    }
	
	public Timer timer(MonitoringContext monitoringContext, Timer.TYPE type) {
		final Timer metric = metricRegistry.getMetric(Timer.class, monitoringContext);
		if (metric != null) {
			return metric;
		} else {
		    ForwardingReservoir forwarding = addAttachedContexts(monitoringContext, null);
		    return createTimerInt(forwarding, monitoringContext, type);
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
	
	public Meter meter(MonitoringContext monitoringContext, TYPE meterCfg) {
        final Meter meter = metricRegistry.getMetric(Meter.class, monitoringContext);
        if (meter != null) {
            return meter;
        } else {
            return createMeterInt(monitoringContext, meterCfg);
        }
    }
	
    private ForwardingReservoir createForward(MonitoringContext forwardCxt) {
        Aggregate forwardAggregate = metricRegistry.getAggregate(forwardCxt);

        ForwardingReservoir forwarding = null;
        if (forwardAggregate != null) {
            forwarding = new ForwardingReservoir();
            forwarding.addReservoir(forwardAggregate);
        }

        return forwarding;
    }

    private ForwardingReservoir addAttachedContexts(MonitoringContext cxt,
            ForwardingReservoir forwarding) {
        List<MonitoringContext> attachedContexts = cxt.getAttachedContexts();
        if (!attachedContexts.isEmpty()) {
            if (forwarding == null) {
                forwarding = new ForwardingReservoir();
            }

            for (MonitoringContext attachedContext : attachedContexts) {
                Aggregate attachedAggregate = metricRegistry
                        .getAggregate(attachedContext);
                forwarding.addReservoir(attachedAggregate);
            }
        }

        return forwarding;
    }
	
	private Aggregate createAggregateInt(Reservoir forwardReservoir, MonitoringContext context, String type) {
        Aggregate aggregate = null;
        if (!context.isEnabled()) {
            aggregate = NoSumAggregate.INSTANCE;
        } else {
            if("SUM".equals(type)) {
                aggregate = new SumAggregate(context, forwardReservoir,
                        reservoirFactory.createAggregateReservoirContainer());
            } else if("SIMPLE".equals(type)) {
                aggregate = new SimpleAggregate(context.getPath(), forwardReservoir,
                        reservoirFactory.createAggregateReservoirContainer());
            } else if("FORWARDING".equals(type)) {
                    aggregate = new ForwardOnlyAggregate(forwardReservoir);
            } else {
                throw new IllegalArgumentException("Unknown aggregate type: " + type);
            }
        }
        
        metricRegistry.register(context, aggregate);
        
        return aggregate;
	}
	
	private Timer createTimerInt(Reservoir forwardReservoir, MonitoringContext context, Timer.TYPE type) {
	    Timer timer = null;
	    if(!context.isEnabled()) {
	        timer = NoTimer.INSTANCE;
	    } else {
            if (forwardReservoir != null) {
                timer = new ForwardingTimerImpl(context,
                        reservoirFactory.createTimerReservoirContainer(type),
                        clock, forwardReservoir);
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
	
	private Meter createMeterInt(MonitoringContext context, Meter.TYPE type) {
	    Meter meter = null;
	    if(!context.isEnabled()) {
	        meter = NoMeter.INSTANCE;
	    } else {
	        meter = new MeterImpl(context, reservoirFactory.createMeterReservoirContainer(type), clock);
	    }

        metricRegistry.register(context, meter);
        return meter;
    }
	
}
