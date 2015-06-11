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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.dcm4chee.archive.monitoring.impl.config.NodeConfiguration;
import org.dcm4chee.archive.monitoring.impl.core.clocks.Clock;
import org.dcm4chee.archive.monitoring.impl.core.context.MonitoringContextProvider;
import org.dcm4chee.archive.monitoring.impl.core.context.MonitoringContextTree;
import org.dcm4chee.archive.monitoring.impl.core.context.NodeEnabledProvider;
import org.dcm4chee.archive.monitoring.impl.core.reservoir.ReservoirBuilderFactory;

/**
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 *
 */
public class MetricProvider implements NodeEnabledProvider {
	private Clock clock;
	private ReservoirBuilderFactory reservoirFactory;
	private MetricFactory metricFactory;
	private MonitoringContextTree metricRegistry;
	private Map<PathContainer,NodeConfiguration> initialNodeConfigurationMap;
	private boolean globalEnabled;
	
	private static MetricProvider INSTANCE;
	
	@Produces @ApplicationMonitoringRegistry
	@ApplicationScoped
	public static MetricProvider getInstance() {
		if (INSTANCE == null) {
			throw new IllegalStateException("No metric provider registered");
		}

		return INSTANCE;
	}
	
	public static void setInstance(MetricProvider metricProvider) {
		INSTANCE = metricProvider;
	}
	
	public MetricProvider()
	{
		// NOOP
	}
	
	public void setClock(Clock clock) {
		this.clock = clock;
	}
	
	public void setReservoirFactory(ReservoirBuilderFactory reservoirFactory) {
		this.reservoirFactory = reservoirFactory;
	}
	
	public void setInitialNodeConfigurations(boolean globalEnabled, List<NodeConfiguration> initialNodeConfigurations) {
	    initialNodeConfigurationMap = new HashMap<>();
        for(NodeConfiguration cfg : initialNodeConfigurations) {
            initialNodeConfigurationMap.put(new PathContainer(cfg.getContextPath()), cfg);
        }
        this.globalEnabled = globalEnabled;
    }
	
	public ReservoirBuilderFactory getReservoirFactory() {
		return reservoirFactory;
	}
	
	public Clock getClock() {
	    return clock;
	}
	
	public void init() {
		metricRegistry = new MonitoringContextTree(clock, this, globalEnabled);
		metricFactory = new MetricFactory(metricRegistry, clock, reservoirFactory);
	}
	
	public void setGlobalEnable(boolean globalEnable) {
	    this.globalEnabled = globalEnable;
	    metricRegistry.setGlobalEnabled(globalEnable);
	}
	
	public boolean isGlobalEnabled() {
	    return globalEnabled;
	}
	
	public MetricFactory getMetricFactory() {
		return metricFactory;
	}
	
	public MetricRegistry getMetricRegistry() {
		return metricRegistry;
	}
	
	public MonitoringContextProvider getMonitoringContextProvider() {
		return metricRegistry.getMonitoringContextProvider();
	}

    @Override
    public Boolean isEnabled(String... path) {
        if(initialNodeConfigurationMap == null) {
            return null;
        }
        
        NodeConfiguration cfg = initialNodeConfigurationMap.get(new PathContainer(path));
        return (cfg != null) ? cfg.isEnabled() : null;
    }
    
    /**
     * Wrapper class to ensure correct equals()/hashCode() behavior for service-name-array
     * 
     * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
     */
    private static class PathContainer {
        private final String[] path;
        
        private PathContainer(String[] path) {
            this.path = path;
        }
        
        @Override
        public boolean equals(Object other) {
            if(this == other) {
                return true;
            }
            
            if(other == null || !(other instanceof PathContainer)) {
                return false;
            }
            
            return Arrays.equals(path, ((PathContainer)other).path);
        }
        
        @Override
        public int hashCode() {
            return Arrays.hashCode(path);
        }
    } 
	
}
