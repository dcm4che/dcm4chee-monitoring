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

import java.util.Date;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.dcm4chee.archive.monitoring.impl.core.ApplicationMonitoringRegistry;
import org.dcm4chee.archive.monitoring.impl.core.Counter;
import org.dcm4chee.archive.monitoring.impl.core.Metric;
import org.dcm4chee.archive.monitoring.impl.core.MetricProvider;
import org.dcm4chee.archive.monitoring.impl.core.MetricRegistry;
import org.dcm4chee.archive.monitoring.impl.core.Timer;
import org.dcm4chee.archive.monitoring.impl.core.aggregate.Aggregate;
import org.dcm4chee.archive.monitoring.impl.core.clocks.Clock;
import org.dcm4chee.archive.monitoring.impl.core.context.AllMatchMonitoringContextFilter;
import org.dcm4chee.archive.monitoring.impl.core.context.MonitoringContext;
import org.dcm4chee.archive.monitoring.impl.core.context.MonitoringContextFilter;
import org.dcm4chee.archive.monitoring.impl.core.context.PrefixMonitoringContextFilter;
import org.dcm4chee.archive.monitoring.impl.core.registry.MetricFilter;
import org.dcm4chee.archive.monitoring.impl.core.registry.MetricFilters;
import org.dcm4chee.archive.monitoring.impl.core.reservoir.AggregatedReservoirSnapshot;
import org.dcm4chee.archive.monitoring.impl.util.UnitOfTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of RESTFUL service to query monitoring metrics.
 * </br>
 * </br>
 * The service allows to filter the queried metrics by the following dimensions:
 * <ul>
 *   <li>monitoring context</li>
 *   <li>metric type</li>
 *   <li>time resolution</li>
 *   <li>time interval</li>
 * </ul>
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 *
 */
@ApplicationScoped
@Path("/monitoring-rs")
public class MonitoringRS {
	private static final Logger LOGGER = LoggerFactory.getLogger(MonitoringRS.class);
	private static final String COUNTER_TYPE_STRING = "counter";
	private static final String TIMER_TYPE_STRING = "timer";

	@Inject @ApplicationMonitoringRegistry
	private MetricProvider metricProvider;
	@Inject
	private XmlStreamingOutputProvider xmlOutputProvider;
	@Inject
	private JsonStreamingOutputProvider jsonOutputProvider;
	
	public MonitoringRS() {
		//NOOP
	}
	
	@GET
    @Path("/metric")
    @Produces({"application/xml"})
    public Response getMetricsXml(@QueryParam("pattern") String pattern, 
            @QueryParam("type") String type,
            @QueryParam("start") String startTime, @QueryParam("end") String stopTime, 
            @QueryParam("resolution") String resolution, 
            @QueryParam("time") String timeFormat, 
            @QueryParam("consume") boolean consume) {
        
        UnitOfTime timeUnit = createTimeUnit(timeFormat);
        TimeSpec timeSpec = createTimeSpec(startTime, stopTime, resolution);
        MetricResponses metricResponse = getMetricsInt(pattern, type, timeSpec, timeUnit, consume);
        
        Response res = Response.ok().entity(xmlOutputProvider.entity(metricResponse)).build();
        return res;

    }
	
	@GET
	@Path("/metric")
	@Produces({"application/json"})
	public Response getMetricsJson(@QueryParam("pattern") String pattern, 
			@QueryParam("type") String type,
			@QueryParam("start") String startTime, @QueryParam("end") String stopTime, 
			@QueryParam("resolution") String resolution, 
			@QueryParam("time") String timeFormat, 
			@QueryParam("consume") boolean consume) {
		
		UnitOfTime timeUnit = createTimeUnit(timeFormat);
		TimeSpec timeSpec = createTimeSpec(startTime, stopTime, resolution);
		MetricResponses metricResponse = getMetricsInt(pattern, type, timeSpec, timeUnit, consume);
		
		Response res = Response.ok().entity(jsonOutputProvider.entity(metricResponse)).build();
		return res;

	}
	
	public MetricResponses getMetricsInt(String pattern, String type, TimeSpec timeSpec, 
			UnitOfTime timeUnit, boolean consume) {
		MonitoringContextFilter cxtFilter = createContextFilter(pattern);
		MetricFilter metricFilter = createMetricFilter(type);
		return buildMetricResponse(cxtFilter, metricFilter, timeSpec, timeUnit, consume);
	}
	
	private MonitoringContextFilter createContextFilter(String pattern) {
		if (pattern != null && !pattern.isEmpty()) {
			return new PrefixMonitoringContextFilter(pattern);
		}
		return AllMatchMonitoringContextFilter.INSTANCE;
	}
	
	private MetricFilter createMetricFilter(String type) {
		if (type == null) {
			return MetricFilters.ALL_FILTER;
		} else if (TIMER_TYPE_STRING.equals(type.toLowerCase())) {
			return MetricFilters.TIMER_FILTER;
		} else if (COUNTER_TYPE_STRING.equals(type.toLowerCase())) {
			return MetricFilters.COUNTER_FILTER;
		} else {
			LOGGER.warn("Unknown metrics type {}", type);
			return MetricFilters.ALL_FILTER;
		}
	}
	
	private TimeSpec createTimeSpec(String startString, String endString, String resolutionString) {
	    TimeSpec timeSpec = null;
        if (startString != null && endString != null && resolutionString != null) {
            timeSpec = TimeSpec.createFromString(startString, endString, resolutionString, metricProvider.getClock());
        }
        return (timeSpec != null) ? timeSpec : TimeSpec.ALL;
	}
	
	private UnitOfTime createTimeUnit(String timeFormat) {
	    UnitOfTime timeUnit = null;
	    if(timeFormat != null ) {
	        timeUnit = TimeHelpers.parseTimeUnit(timeFormat);
	    }
		return timeUnit != null ? timeUnit : UnitOfTime.MILLISECONDS;
	}
	
	private MetricResponses buildMetricResponse(MonitoringContextFilter cxtFilter, MetricFilter metricFilter, 
			TimeSpec timeSpec, UnitOfTime timeUnit, boolean consume) {
		MetricResponses metricResponse = new MetricResponses();
		
		MetricRegistry registry = metricProvider.getMetricRegistry();
		MonitoringContext rootContext = metricProvider.getMonitoringContextProvider().getRootContext();
		traverseAndCollectMetrics(registry, rootContext, cxtFilter, metricFilter, timeSpec, timeUnit, consume, metricResponse);
		
		return metricResponse;
	}
	
	private void traverseAndCollectMetrics(MetricRegistry registry, MonitoringContext cxt, 
			MonitoringContextFilter cxtFilter, MetricFilter metricFilter, 
			TimeSpec timeSpec, UnitOfTime timeUnit, boolean consume, MetricResponses metricResponse) {
		boolean cxtMatch = true;
		MonitoringContextFilter.FilterResult filterResult = cxtFilter.matches(cxt);
		if (!filterResult.matches()) {
			if (filterResult.ignoreChildContexts()) {
				return;
			} else {
				cxtMatch = false;
			}
		}
		
		if (cxtMatch) {
			Metric metric = null;
			if ( consume ) {
				metric = registry.consumeMetric(metricFilter, cxt);
			} else {
				metric = registry.getMetric(metricFilter, cxt);
			}
			
			if (metric != null) {
				if (metric instanceof Timer) {
					Timer timer = (Timer)metric;
					if(timeSpec == TimeSpec.ALL) {
//						AggregatedReservoirSnapshot snapshot = timer.getSnapshot();
//						if( snapshot != null) {
//						    TimerResponse timerResponse = TimerResponse.create(snapshot, timeUnit);
//	                        metricResponse.addTimer(timerResponse);
//						}
						List<AggregatedReservoirSnapshot> snapshots = timer.getSnapshots();
						for (AggregatedReservoirSnapshot snapshot : snapshots) {
							TimerResponse timerResponse = TimerResponse.create(snapshot, timeUnit);
							metricResponse.addTimer(timerResponse);
						}
					} else {
						List<AggregatedReservoirSnapshot> snapshots = timer.getSnapshots(timeSpec.getStart(), timeSpec.getEnd(), timeSpec.getResolution());
						for (AggregatedReservoirSnapshot snapshot : snapshots) {
							TimerResponse timerResponse = TimerResponse.create(snapshot, timeUnit);
							metricResponse.addTimer(timerResponse);
						}
					}
				} else if (metric instanceof Counter) {
					Counter counter = (Counter)metric;
					if(timeSpec == TimeSpec.ALL) {
//						AggregatedReservoirSnapshot snapshot = counter.getSnapshot();
//						if(snapshot != null) {
//						    CounterResponse counterResponse = CounterResponse.create(snapshot);
//	                        metricResponse.addCounter(counterResponse);
//						}
						List<AggregatedReservoirSnapshot> snapshots = counter.getSnapshots();
						for (AggregatedReservoirSnapshot snapshot : snapshots) {
							CounterResponse counterResponse = CounterResponse.create(snapshot);
							metricResponse.addCounter(counterResponse);
						}
						
					} else {
						List<AggregatedReservoirSnapshot> snapshots = counter.getSnapshots(timeSpec.getStart(), timeSpec.getEnd(), timeSpec.getResolution());
						for (AggregatedReservoirSnapshot snapshot : snapshots) {
							CounterResponse counterResponse = CounterResponse.create(snapshot);
							metricResponse.addCounter(counterResponse);
						}
					}
				} else if (metric instanceof Aggregate) {
					Aggregate aggregate = (Aggregate)metric;
					if(timeSpec == TimeSpec.ALL) {
//						AggregateSnapshot snapshot = aggregate.getSnapshot();
//						if(snapshot != null) {
//						    AggregateTimerResponse aggregateResponse = AggregateTimerResponse.create(snapshot, timeUnit);
//	                        metricResponse.addAggregate(aggregateResponse);
//						}
						List<AggregatedReservoirSnapshot> snapshots = aggregate.getSnapshots();
						for (AggregatedReservoirSnapshot snapshot : snapshots) {
							AggregateTimerResponse aggregateResponse = AggregateTimerResponse.create(snapshot, timeUnit);
							metricResponse.addAggregate(aggregateResponse);
						}
					} else {
						List<AggregatedReservoirSnapshot> snapshots = aggregate.getSnapshots(timeSpec.getStart(), timeSpec.getEnd(), timeSpec.getResolution());
						for (AggregatedReservoirSnapshot snapshot : snapshots) {
							AggregateTimerResponse aggregateResponse = AggregateTimerResponse.create(snapshot, timeUnit);
							metricResponse.addAggregate(aggregateResponse);
						}
					}

				}
			}
		}
		
		for (MonitoringContext child : cxt.getChildren(true)) {
			traverseAndCollectMetrics(registry, child, cxtFilter, metricFilter, timeSpec, timeUnit, consume, metricResponse);
		}
	}
	
	public static class TimeSpec {
		private final long start;
		private final long end;
		private final long resolution;
		
		protected final static TimeSpec ALL = new TimeSpec(-1, -1, -1);
		
		public static TimeSpec createFromTimeMillis(long start, long end, long resolution) {
		    return new TimeSpec(start, end, resolution);
		}
		
        public static TimeSpec createFromString(String startDateString,
                String endDateString, String resolutionString, Clock clock) {
            Date startDate = TimeHelpers.parseDate(startDateString);
            if(startDate == null ){
                startDate = TimeHelpers.parseNaturalDateDescription(clock, startDateString);
            }
            
            Date endDate = TimeHelpers.parseDate(endDateString);
            if(endDate == null ){
                endDate = TimeHelpers.parseNaturalDateDescription(clock, endDateString);
            }
            
            long resolution = TimeHelpers.parseResolutionString(resolutionString);
            if (startDate != null && endDate != null && resolution != Long.MIN_VALUE) {
                return new TimeSpec(startDate.getTime(), endDate.getTime(), resolution);
            }

            return TimeSpec.ALL;
        }
		
		private TimeSpec(long start, long end, long resolution) {
			this.start = start;
			this.end = end;
			this.resolution = resolution;
		}

		protected long getStart() {
			return start;
		}

		protected long getEnd() {
			return end;
		}

		protected long getResolution() {
			return resolution;
		}

		public static TimeSpec getAll() {
			return ALL;
		}
		
	}
}
