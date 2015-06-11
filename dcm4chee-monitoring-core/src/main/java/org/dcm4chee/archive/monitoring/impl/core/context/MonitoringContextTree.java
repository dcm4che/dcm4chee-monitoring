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


package org.dcm4chee.archive.monitoring.impl.core.context;

import java.util.TimerTask;

import org.dcm4chee.archive.monitoring.impl.core.Metric;
import org.dcm4chee.archive.monitoring.impl.core.MetricRegistry;
import org.dcm4chee.archive.monitoring.impl.core.aggregate.Aggregate;
import org.dcm4chee.archive.monitoring.impl.core.clocks.Clock;
import org.dcm4chee.archive.monitoring.impl.core.context.MonitoringContextNode.DISPOSAL_CONTEXT;
import org.dcm4chee.archive.monitoring.impl.core.registry.MetricFilter;
import org.dcm4chee.archive.monitoring.impl.util.UnitOfTime;

public class MonitoringContextTree implements MetricRegistry {
    private static final long DEFAULT_CONSUMED_METRIC_TIMEOUT = 5000;
    
    private final Clock clock;
    private final NonDisposableMonitoringContextNode rootContext;
    private final MonitoringContextProvider monitoringContextProvider;
    
    private long consumedMetricTimeout = DEFAULT_CONSUMED_METRIC_TIMEOUT;
    private java.util.Timer metricCleanupDaemon;
    private TimerTask metricCleanupTask;
    
    private NodeEnabledProvider enabledProvider;
    private boolean globalEnabled;
    
    public MonitoringContextTree(Clock clock) {
        this(clock, null, true);
    }
    
    public MonitoringContextTree(Clock clock, NodeEnabledProvider enabledProvider, boolean enabled) {
        this.clock = clock;
        
        Boolean _rootEnabled = (enabledProvider != null) ? enabledProvider.isEnabled(new String[0]) : null;
        boolean isRootEnabled = (_rootEnabled != null) ? _rootEnabled : true; 
        this.rootContext = new NonDisposableMonitoringContextNode(this, isRootEnabled);
        
        this.enabledProvider = enabledProvider;
        this.globalEnabled = enabled;
        this.monitoringContextProvider = new MonitoringContextProvider(this);
    }
    
    public boolean isGlobalEnabled() {
        return globalEnabled;
    }
    
    public void setGlobalEnabled(boolean globalEnabled) {
        this.globalEnabled = globalEnabled;
        this.rootContext.setGlobalEnabled(globalEnabled);
    }
    
    public NonDisposableMonitoringContextNode getNodeNode() {
        return rootContext.getNodeNode(globalEnabled, true);
    }
    
    public NonDisposableMonitoringContextNode getUndefinedNode() {
        return getNodeNode().getUndefinedNode(globalEnabled, true);
    }
    
    public Boolean isEnabled(String[] path) {
        return enabledProvider != null ? enabledProvider.isEnabled(path) : null;
    }
    
    public Clock getClock() {
        return clock;
    }
    
    public NonDisposableMonitoringContextNode getRoot() {
        return rootContext;
    }
    
    @Override
    public void startMetricCleanupDaemon(long daemonRunPeriod, UnitOfTime timeUnit) {
        initMetricCleanupDaemon(timeUnit.toMillis(daemonRunPeriod));
    }
    
    @Override
    public void setConsumedMetricTimeout(long consumedMetricTimeout, UnitOfTime timeUnit) {
        this.consumedMetricTimeout = timeUnit.toMillis(consumedMetricTimeout);
    }
    
    private void initMetricCleanupDaemon(final long daemonRunPeriodMillis) {
        if (metricCleanupTask != null) {
            metricCleanupTask.cancel();
            metricCleanupTask = null;
        }

        if (daemonRunPeriodMillis > 0) {
            metricCleanupTask = new TimerTask() {
                @Override
                public void run() {
                    traverseAndDisposeOutdated();
                }
            };
            
            if(metricCleanupDaemon == null) {
                metricCleanupDaemon = new java.util.Timer("MonitoringCleanupDaemon");
            }
            metricCleanupDaemon.schedule(metricCleanupTask, daemonRunPeriodMillis, daemonRunPeriodMillis);
        }
    }
    
    private void traverseAndDisposeOutdated() {
        for(MonitoringContext child : monitoringContextProvider.getRootContext().getChildren(true)) {
            traverseAndDisposeOutdated(child);
        }
        
    }
    
    private void traverseAndDisposeOutdated(MonitoringContext cxt) {
        for(MonitoringContext child : cxt.getChildren(true)) {
            MonitoringContextNode cxtChildNode = dirtyCast(child);
            MetricContainer<?> container = cxtChildNode.getMetricContainer();
            if(container != null ) {
                cxtChildNode.checkedDispose(DISPOSAL_CONTEXT.CLEANUP_CHECK, false);
            }
            traverseAndDisposeOutdated(child);
        }
        
    }
    
    public MonitoringContextProvider getMonitoringContextProvider() {
        return monitoringContextProvider;
    }
    
    @Override
    public Aggregate getAggregate(MonitoringContext cxt) {
        if (cxt == null) {
            return null;
        }
        
        Aggregate aggregate = getMetric(Aggregate.class, cxt);
        if(aggregate != null) {
            return aggregate;
        }
        
        return getAggregate(cxt.getParentContext());
    }
    
    @Override
    public <T extends Metric> T getParentMetric(Class<T> metricType, MonitoringContext cxt) {
        MonitoringContext parentContext = cxt.getParentContext();
        if(parentContext == null) {
            return null;
        }
        
        T parentMetric = getMetric(metricType, parentContext);
        if(parentMetric != null) {
            return parentMetric;
        }
        
        return getParentMetric(metricType, parentContext);
    }
    
    @Override
    public <T extends Metric> T getMetric(Class<T> metricType, MonitoringContext cxt) {
        MonitoringContextNode cxtNode = dirtyCast(cxt);
        MetricContainer<? extends Metric> metricContainer = cxtNode.getMetricContainer();
        if (metricContainer != null) {
            Metric metric = metricContainer.getMetric();
            if (metric != null && metricType.isInstance(metric)) {
                @SuppressWarnings("unchecked")
                T t = (T) metric;
                return t;
            }
        }
        return null;
    }
    
    @Override
    public Metric getMetric(MetricFilter filter, MonitoringContext cxt) {
        MonitoringContextNode cxtNode = dirtyCast(cxt);
        MetricContainer<? extends Metric> metricContainer = cxtNode.getMetricContainer();
        if (metricContainer != null) {
            Metric metric = metricContainer.getMetric();
            if (metric != null && filter.matches(cxtNode, metric)) {
                return metric;
            }
        }
        return null;
    }
    
    public Metric consumeMetric(MetricFilter filter, MonitoringContext cxt) {
        MonitoringContextNode cxtNode = dirtyCast(cxt);
        MetricContainer<? extends Metric> metricContainer = cxtNode.getMetricContainer();
        if (metricContainer != null) {
            Metric metric = metricContainer.getMetric();
            if (metric != null && filter.matches(cxt, metric)) {
                cxtNode.checkedDispose(MonitoringContextNode.DISPOSAL_CONTEXT.CONSUME, false);
                return metric;
            }
        }
        return null;
    }

    @Override
    public <T extends Metric> T register(MonitoringContext cxt, T metric) {
        MonitoringContextNode cxtNode = dirtyCast(cxt);
        MetricContainer<T> container = new MetricContainer<T>(metric, consumedMetricTimeout);
        cxtNode.setMetricContainer(container);
        return metric;
    }
    
    public static MonitoringContextNode dirtyCast(MonitoringContext cxt) {
        return (MonitoringContextNode)cxt;
    }

}
