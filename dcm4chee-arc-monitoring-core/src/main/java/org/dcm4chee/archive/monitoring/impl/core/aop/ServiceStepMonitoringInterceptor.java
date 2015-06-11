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

package org.dcm4chee.archive.monitoring.impl.core.aop;

import java.util.Arrays;

import javax.inject.Inject;
import javax.interceptor.InvocationContext;

import org.dcm4chee.archive.monitoring.api.MONITORING_SCOPE;
import org.dcm4chee.archive.monitoring.api.MetricAttributesProvider;
import org.dcm4chee.archive.monitoring.api.Monitored;
import org.dcm4chee.archive.monitoring.api.aop.MonitoredScope;
import org.dcm4chee.archive.monitoring.api.aop.MonitoringInterceptor;
import org.dcm4chee.archive.monitoring.api.aop.MonitoringInterceptorType;
import org.dcm4chee.archive.monitoring.impl.core.ApplicationMonitoringRegistry;
import org.dcm4chee.archive.monitoring.impl.core.MetricProvider;
import org.dcm4chee.archive.monitoring.impl.core.Timer;
import org.dcm4chee.archive.monitoring.impl.core.context.MonitoringContext;
import org.dcm4chee.archive.monitoring.impl.core.context.MonitoringContextProvider;



/**
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 *
 */
@MonitoringInterceptorType(scope=MonitoredScope.SERVICE_STEP)
public class ServiceStepMonitoringInterceptor implements MonitoringInterceptor {
    private static final String[] UNDEFINED_SERVICE = new String[] { "UNDEFINED" };
    
	@Inject @ApplicationMonitoringRegistry
	private MetricProvider metricProvider;
	
	@Inject
    private MetricAttributesProviderManager attributesProviderManager;
	
	protected MonitoringContext getServiceMonitoringContextPath(MonitoringContextProvider cxtProvider, boolean instance, String[] serviceName, 
            InvocationContext context) {
        if (serviceName != null && !Arrays.equals(serviceName, UNDEFINED_SERVICE)) {
            return cxtProvider.getActiveContext(instance).getOrCreateContext(serviceName);
        } else {
            String className = context.getMethod().getDeclaringClass().getSimpleName();
            String methodName = context.getMethod().getName();
            return cxtProvider.getActiveContext(instance).getOrCreateContext(className, methodName);
        }
    }

	/**
	 * Indicates whether the method invocation should be monitored.
	 * Default behavior always returns true.
	 * This method can be overridden
	 *
	 * @param context Method invocation context
	 * @return true to enable Simon, false either
	 */
	protected boolean isMonitored(InvocationContext context) {
		return true;
	}

	@Override
	public Object monitor(InvocationContext context) throws Exception {
		if (isMonitored(context)) {
		    MonitoringContextProvider cxtProvider = metricProvider.getMonitoringContextProvider();
		    
		    Monitored monitorAnnotation = context.getMethod().getAnnotation(Monitored.class);
            MONITORING_SCOPE scope = monitorAnnotation.scope();
            String[] stepName = monitorAnnotation.name();
            
            Timer stepTimer, stepInstanceTimer = null;
            switch(scope) {
            case SERVICE:
                // open service instance monitoring context
                MonitoringContext stepCxt = getServiceMonitoringContextPath(cxtProvider, false, stepName, context);
                stepTimer = metricProvider.getMetricFactory().timer(stepCxt);
                break;
            case SERVICE_INSTANCE: 
                // open service instance monitoring context
                stepCxt = getServiceMonitoringContextPath(cxtProvider, false, stepName, context);
                // open service instance monitoring context
                MonitoringContext instanceCxt = getServiceMonitoringContextPath(cxtProvider, true, stepName, context);
                stepTimer = metricProvider.getMetricFactory().timer(stepCxt);
                stepInstanceTimer = metricProvider.getMetricFactory().timer(instanceCxt, Timer.TYPE.ONE_SHOT);
                
                MetricAttributesProvider metricAttributesProvider = attributesProviderManager.getAttributesProvider(stepName);
                if(metricAttributesProvider != null ) {
                    stepInstanceTimer.setAttributes(metricAttributesProvider.getMetricAttributes(context));
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown service scope " + scope);
            }
			
			Timer.Split stepTimerSplit = null;
            if(stepTimer != null) {
                stepTimerSplit = stepTimer.time();
            }
          
            Timer.Split stepInstanceTimerSplit = null;
            if(stepInstanceTimer != null) {
               stepInstanceTimerSplit = stepInstanceTimer.time();
            }
            
            try {
                return context.proceed();
            } finally {
                if(stepTimerSplit != null) {
                    stepTimerSplit.stop();
                }
                if(stepInstanceTimerSplit != null) {
                    stepInstanceTimerSplit.stop();
                }
            }
		} else {
			return context.proceed();
		}
	}
}

