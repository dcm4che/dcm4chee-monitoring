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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.dcm4chee.archive.monitoring.impl.core.Util;

/**
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 *
 */
public class NonDisposableMonitoringContextNode extends MonitoringContextNode {
    private static final String UNDEFINED_NODE_NAME = "undefined";
    
    private final boolean undefined;
	private NonDisposableMonitoringContextNode undefinedNode;
	private NonDisposableMonitoringContextNode nodeNode;
	
	
	public NonDisposableMonitoringContextNode(MonitoringContextTree tree, boolean enabled) {
		super(tree, enabled);
		this.undefined = false;
	}
	
	private NonDisposableMonitoringContextNode(MonitoringContextTree tree, MonitoringContextNode parent, 
	        boolean enabled, boolean inheritedFromParent, boolean undefined, String... path) {
		super(tree, parent, enabled, inheritedFromParent, path);
		this.undefined = undefined;
	}
	
	@Override
	public void dispose(boolean force) {
		if (!children.isEmpty()) {
			List<MonitoringContextNode> childrenCopy = new ArrayList<>(children);
			for (MonitoringContextNode child : childrenCopy) {
				child.dispose(force);
			}
		}
	}
	
	public NonDisposableMonitoringContextNode getUndefinedNode(boolean enabled, boolean inheritedFromParent) {
		if (undefinedNode == null) {
		    // construct path of undefined node
		    String[] parentPath = getPath();
		    String[] undefinedNodePath = new String[parentPath.length + 1];
		    System.arraycopy(parentPath, 0, undefinedNodePath, 0, parentPath.length);
		    undefinedNodePath[ undefinedNodePath.length - 1] = UNDEFINED_NODE_NAME;
		    
			undefinedNode = new NonDisposableMonitoringContextNode(tree, this, 
			        enabled, inheritedFromParent, true, undefinedNodePath);
			if(children == Collections.<MonitoringContextNode>emptyList()) {
				children = new ArrayList<>();
			}
			
			children.add(undefinedNode);
		}
		
		return undefinedNode;
	}
	
	public NonDisposableMonitoringContextNode getNodeNode(boolean enabled, boolean inheritedFromParent) {
		if (nodeNode == null) {
		    String nodeName = Util.getJBossNodeName();
			nodeNode = new NonDisposableMonitoringContextNode(tree, this, 
			        enabled, inheritedFromParent, false, nodeName);
			if(children == Collections.<MonitoringContextNode>emptyList()) {
				children = new ArrayList<>();
			}
			
			children.add(nodeNode);
		}
		
		return nodeNode;
	}
	
    @Override
    public boolean isUndefined() {
        return undefined;
    }
	
}
