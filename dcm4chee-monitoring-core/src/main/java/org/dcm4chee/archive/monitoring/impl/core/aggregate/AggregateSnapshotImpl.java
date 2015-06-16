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

package org.dcm4chee.archive.monitoring.impl.core.aggregate;

import java.util.Collections;
import java.util.Map;



/**
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 *
 */
public class AggregateSnapshotImpl implements AggregateSnapshot {
	private String path;
	private long[] values;
	
	private long size;
	
	private long min;
	private long max;
	private double mean;
	private double stdDev;
	private long maxTimestamp;
	private long minTimestamp;
	private long firstUsageTimestamp;
	private long lastUsageTimestamp;
	private long sum;
	
	private Map<String, Object> attributes = Collections.emptyMap();
	
	@Override
	public String getPath() {
		return path;
	}
	
	public void setPath(String path) {
		this.path = path;
	}
	
	@Override
	public long[] getValues(boolean copy) {
		return values;
	}
	
	public void setValues(long[] values) {
		this.values = values;
	}

	@Override
	public long getMax() {
		return max;
	}
	
	public void setMax(long max) {
		this.max = max;
	}

	@Override
	public long getMin() {
		return min;
	}
	
	public void setMin(long min) {
		this.min = min;
	}
	
	@Override
	public long size() {
		return size;
	}
	
	@Override
	public long getMaxTimestamp() {
		return maxTimestamp;
	}

	public void setMaxTimestamp(long maxTimestamp) {
		this.maxTimestamp = maxTimestamp;
	}

	@Override
	public long getMinTimestamp() {
		return minTimestamp;
	}

	public void setMinTimestamp(long minTimestamp) {
		this.minTimestamp = minTimestamp;
	}

	@Override
	public long getFirstUsageTimestamp() {
		return firstUsageTimestamp;
	}

	public void setFirstUsageTimestamp(long firstUsageTimestamp) {
		this.firstUsageTimestamp = firstUsageTimestamp;
	}

	@Override
	public long getLastUsageTimestamp() {
		return lastUsageTimestamp;
	}
	
	public void setLastUsageTimestamp(long lastUsageTimestamp) {
		this.lastUsageTimestamp = lastUsageTimestamp;
	}

	@Override
	public double getMean() {
		return mean;
	}
	
	public void setMean(double mean) {
		this.mean = mean;
	}

	@Override
	public double getStdDev() {
		return stdDev;
	}
	
	public void setStdDev(double stdDev) {
		this.stdDev = stdDev;
	}
	
    @Override
    public long getSum() {
        return sum;
    }

    public void setSum(long sum) {
        this.sum = sum;
    }

	public void setSize(long size) {
		this.size = size;
	}
	
	@Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }
    
    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

}
