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
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.dcm4chee.archive.monitoring.api.MetricAttributesProvider;
import org.dcm4chee.archive.monitoring.api.Binding;
import org.dcm4chee.archive.monitoring.api.ServiceBindings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 *
 */
@ApplicationScoped
public class MetricAttributesProviderManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricAttributesProviderManager.class);
    private final Map<ServiceName,MetricAttributesProvider> service2ProviderMap = new HashMap<>();
    
    @Inject
    private Instance<MetricAttributesProvider> attributeProviders;
    
    @PostConstruct
    public void init() {
        for(MetricAttributesProvider attributeProvider : attributeProviders) {
            Class<? extends MetricAttributesProvider> providerClass = attributeProvider.getClass();
            ServiceBindings serviceBindings = providerClass.getAnnotation(ServiceBindings.class);
            if(serviceBindings == null) {
                LOGGER.error("MetricAttributesProvider implementation {} has no @ServiceBindings annotation -> Skip", providerClass.getName());
            } else {
                for(Binding binding : serviceBindings.value()) {
                    service2ProviderMap.put(new ServiceName(binding.value()), attributeProvider);
                }
               
            }
        }
    }
    
    public MetricAttributesProvider getAttributesProvider(String... serviceName) {
        return service2ProviderMap.get(new ServiceName(serviceName));
    }
    
    /**
     * Wrapper class to ensure correct equals()/hashCode() behavior for service-name-array
     * 
     * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
     */
    private static class ServiceName {
        private final String[] serviceName;
        
        private ServiceName(String[] serviceName) {
            this.serviceName = serviceName;
        }
        
        @Override
        public boolean equals(Object other) {
            if(this == other) {
                return true;
            }
            
            if(other == null || !(other instanceof ServiceName)) {
                return false;
            }
            
            return Arrays.equals(serviceName, ((ServiceName)other).serviceName);
        }
        
        @Override
        public int hashCode() {
            return Arrays.hashCode(serviceName);
        }
    } 
    
}
