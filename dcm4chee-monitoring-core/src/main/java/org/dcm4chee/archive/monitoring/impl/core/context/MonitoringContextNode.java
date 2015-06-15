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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.dcm4chee.archive.monitoring.impl.core.Metric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 *
 */
public class MonitoringContextNode implements MonitoringContext {
	private static final Logger LOGGER = LoggerFactory.getLogger(MonitoringContextNode.class);
	
	private final String[] path;
	protected final MonitoringContextTree tree;
	private MonitoringContextNode parent;
	protected List<MonitoringContextNode> children = Collections.emptyList();
	private List<MonitoringContext> attachedContexts = Collections.emptyList();
	
	private boolean disposed;
	
	private boolean metricEnabled;
	private MetricContainer<? extends Metric> metricContainer;
	
	private boolean inheritedFromParent;
	private boolean enabled;
	
	public static enum DISPOSAL_CONTEXT {
	    EXTERNAL, CLEANUP_CHECK, CONSUME
	}
	
	public MonitoringContextNode(MonitoringContextTree tree, boolean enabled) {
		this(tree, null, enabled, false, new String[0]);
	}
	
	protected MonitoringContextNode(MonitoringContextTree tree, MonitoringContextNode parent, 
	        boolean enabled, boolean inheritedFromParent, String... path) {
        this.tree = tree;
        this.parent = parent;
        this.enabled = enabled;
        this.inheritedFromParent = inheritedFromParent;
        this.path = path;
        LOGGER.debug("Creating monitoring context: {}", this);
        init();
    }
	
	private void init() {
		List<MonitoringContext> attachableContexts = AttachedContextResolverProvider.getInstance().getResolver().getAttachableContexts(this);
		for (MonitoringContext attachableCxt : attachableContexts) {
			LOGGER.debug("Attaching monitoring context: {} -> {} ", this, attachableContexts);
			attachContext(attachableCxt);
		}
	}
	
	private MonitoringContextNode createChildNode(String[] childPathSegments, int level) {
		String[] childPath;
		if(path == null) {
			childPath = new String[1];
			childPath[0] = childPathSegments[level];
		} else {
			childPath = new String[path.length + 1];
			System.arraycopy(path, 0, childPath, 0, path.length);
			childPath[path.length] = childPathSegments[level];
		}
		
		boolean[] enabledAndInherited = determineIfEnabled(childPath);
		MonitoringContextNode child = new MonitoringContextNode(tree, this, enabledAndInherited[0], enabledAndInherited[1], childPath);
		if(children == Collections.<MonitoringContextNode>emptyList()) {
			children = new ArrayList<>();
		}
		
		children.add(child);
		if(level + 1 < childPathSegments.length) {
			return child.createChildNode(childPathSegments, level + 1);
		}
		
		return child;
	}
	
	private boolean[] determineIfEnabled(String[] childPath) {
	    Boolean enabled = tree.isEnabled(childPath);
	    boolean inheritedFromParent = false;
	    if(enabled == null) {
	        enabled = this.enabled;
	        inheritedFromParent = true;
	    }
	    
	    return new boolean[] {enabled, inheritedFromParent};
	}
	
	public boolean isEnabled() {
	    return tree.isGlobalEnabled() && enabled;
	}
	
	@Override
	public MonitoringContext getOrCreateInstanceContext(Object obj, String... path) {
		String[] fullPath = new String[path.length + 1];
		System.arraycopy(path, 0, fullPath, 0, path.length);
		fullPath[fullPath.length - 1] = Integer.toString(System.identityHashCode(obj));
		return getOrCreateContext(fullPath);
	}
	
	@Override
	public MonitoringContextNode getContext(String... path) {
		return getChild(path, 0, false);
	}
	
	@Override
	public MonitoringContextNode getOrCreateContext(String... path) {
		return getChild(path, 0, true);
	}
	
	@Override
	public MonitoringContextNode getOrCreateContext(String[] path, String... suffix) {
	    String[] completePath = new String[ path.length + suffix.length];
        System.arraycopy(path, 0, completePath, 0, path.length);
        System.arraycopy(suffix, 0, completePath, path.length, suffix.length);
        return getChild(completePath, 0, true);
    }
	
	private MonitoringContextNode getChild(String[] path, int level, boolean create) {
		for (MonitoringContextNode child : children) {
			if (child.path[child.path.length - 1].equals(path[level])) {
				if (level + 1 < path.length) {
					return child.getChild(path, level + 1, create);
				} else {
					return child;
				}
			}
		}

		return create ? createChildNode(path, level) : null;
	}

	@Override
	public String[] getPath() {
		return path;
	}
	
	@Override
	public MonitoringContextNode getParentContext() {
		return parent;
	}

	@Override
	public List<MonitoringContext> getAttachedContexts() {
		return attachedContexts;
	}

	@Override
	public void attachContext(MonitoringContext context) {
		if (this.attachedContexts == Collections.<MonitoringContext>emptyList()) {
			this.attachedContexts = new ArrayList<>();
		}
		this.attachedContexts.add(context);
	}
	
	@Override
	public List<MonitoringContext> getChildren(boolean copy) {
		return copy ? new ArrayList<MonitoringContext>(children) : Collections.<MonitoringContext>unmodifiableList(children);
	}

	@Override
	public void dispose() {
		dispose(false);
	}
	
	@Override
    public void dispose(boolean force) {
	    checkedDispose(DISPOSAL_CONTEXT.EXTERNAL, force);
	}
	
    public void checkedDispose(DISPOSAL_CONTEXT disposalCxt, boolean force) {
        if(DISPOSAL_CONTEXT.EXTERNAL.equals(disposalCxt) || DISPOSAL_CONTEXT.CLEANUP_CHECK.equals(disposalCxt)) {
            propagateToChildren(disposalCxt, force);
        }
        
        if (metricContainer != null) {
            long now = tree.getClock().getTime();

            if (DISPOSAL_CONTEXT.CONSUME.equals(disposalCxt)) {
                metricContainer.markConsumed(now);
            } else if (DISPOSAL_CONTEXT.EXTERNAL.equals(disposalCxt)) {
                metricContainer.markExternallyDisposed(now);
            }

            if (!force && !metricContainer.checkDispose(now)) {
                return;
            }

            metricContainer = null;
        }

        disposed = true;

        if (children.isEmpty()) {
            if( parent != null) {
                parent.informParentOnChildDisposal(this);
                parent = null;
            }
        }
    }
    
    private void propagateToChildren(DISPOSAL_CONTEXT disposalCxt, boolean force) {
        if (!children.isEmpty()) {
          List<MonitoringContext> childrenCopy = getChildren(true);
          for (MonitoringContext child : childrenCopy) {
              MonitoringContextNode childCxt = MonitoringContextTree.dirtyCast(child);
              childCxt.checkedDispose(disposalCxt, force);
          }
      }
    }

    protected void informParentOnChildDisposal(MonitoringContextNode child) {
        children.remove(child);
        if (disposed && children.isEmpty()) {
            parent.informParentOnChildDisposal(this);
            parent = null;
        }
    }

    public MetricContainer<? extends Metric> getMetricContainer() {
        return metricContainer;
    }

    public void setMetricContainer(MetricContainer<? extends Metric> container) {
        this.metricContainer = container;
        this.metricEnabled = isMetricEnabled();
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.inheritedFromParent = false;
        updateMetricEnabled(isMetricEnabled());
        propagateUpdateEnabled(enabled);
    }
    
    private boolean isMetricEnabled() {
        return tree.isGlobalEnabled() && enabled;
    }
    
    public void setGlobalEnabled(boolean globalEnabled) {
        updateMetricEnabled(isMetricEnabled());
        for (MonitoringContext child : getChildren(false)) {
            child.setGlobalEnabled(globalEnabled);
        }
    }
    
    private void propagateUpdateEnabled(boolean enabled) {
        if (inheritedFromParent) {
            this.enabled = enabled;
            updateMetricEnabled(isMetricEnabled());
        }

        for (MonitoringContext child : getChildren(false)) {
            MonitoringContextNode childCxt = MonitoringContextTree.dirtyCast(child);
            childCxt.propagateUpdateEnabled(enabled);
        }
    }
    
    private void updateMetricEnabled(boolean metricEnabled) {
        if(metricEnabled != this.metricEnabled) {
            metricContainer = null;
            this.metricEnabled = metricEnabled;
        }
    }
    
    @Override
    public boolean isUndefined() {
        return false;
    }

    @Override
    public String toString() {
        return "MonitoringContext(" + Arrays.toString(path) + ")";
    }
    
}
