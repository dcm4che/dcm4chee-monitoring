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

import static java.lang.String.format;

import java.util.Arrays;

/**
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 *
 */
public class MonitoringContextProvider {
	private static final ThreadLocal<MonitoringContext> activeInstanceContext = new ThreadLocal<MonitoringContext>();
	private static final ThreadLocal<MonitoringContext> activeStaticContext = new ThreadLocal<MonitoringContext>();
	
	private final NonDisposableMonitoringContextNode rootContext;
	private final NonDisposableMonitoringContextNode nodeContext;
	private final NonDisposableMonitoringContextNode undefined;
	
	public MonitoringContextProvider(MonitoringContextTree tree) {
	    rootContext = tree.getRoot();
	    nodeContext = tree.getNodeNode();
	    undefined = tree.getUndefinedNode();
	}
	
	public MonitoringContext createActiveInstanceContext(MonitoringContext context) {
		MonitoringContext mContext = getActiveInstanceContext();
		if (mContext != undefined) {
			throw new IllegalStateException(format("Active monitoring context already exists: %s", Arrays.toString(mContext.getPath())));
		}
		
		activeInstanceContext.set(context);
		return context;
	}
	
	public MonitoringContext createActiveStaticContext(MonitoringContext context) {
        MonitoringContext mContext = getActiveStaticContext();
        if (mContext != undefined) {
            throw new IllegalStateException(format("Active monitoring context already exists: %s", Arrays.toString(mContext.getPath())));
        }
        
        activeStaticContext.set(context);
        return context;
    }
	
	public MonitoringContext createActiveInstanceContext(String... path) {
		return createActiveInstanceContext(rootContext.getOrCreateContext(path));
	}
	
	public MonitoringContext getActiveContext(boolean instance) {
        MonitoringContext mContext = instance ? activeInstanceContext.get() : activeStaticContext.get();
        return (mContext != null) ? mContext : undefined;
    }
	
	public MonitoringContext getActiveStaticContext() {
        MonitoringContext mContext = activeStaticContext.get();
        return (mContext != null) ? mContext : undefined;
    }
	
	public MonitoringContext getActiveInstanceContext() {
		MonitoringContext mContext = activeInstanceContext.get();
		return (mContext != null) ? mContext : undefined;
	}
	
	public void disposeActiveInstanceContext() {
		MonitoringContext activeInstanceContext = MonitoringContextProvider.activeInstanceContext.get();
		if(activeInstanceContext != null) {
			activeInstanceContext.dispose();
		}
		MonitoringContextProvider.activeInstanceContext.remove();
	}
	
	public void freeActiveStaticContext() {
        MonitoringContextProvider.activeStaticContext.remove();
    }
	
	public void disposeAndFreeAllActiveContext() {
       disposeActiveInstanceContext();
       freeActiveStaticContext();
    }
	
	public MonitoringContext getNodeContext() {
		return nodeContext;
	}
	
	public MonitoringContext getRootContext() {
		return rootContext;
	}
	
}
