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

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.dcm4chee.archive.monitoring.impl.util.UnitOfTime;


/**
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 *
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder={
		"path", "start", "end", "attributes", "firstUsageTimestamp", "lastUsageTimestamp", 
		"minTimestamp", "min", "maxTimestamp", "max", "median", "mean", "stdDev", "sum"
})

@JsonPropertyOrder({
		"path", "start", "end", "attributes", "firstUsageTimestamp", "lastUsageTimestamp", 
		"minTimestamp", "min", "maxTimestamp", "max", "median", "mean", "stdDev", "sum"
})
public class MetricResponse {
	private String path;
	
	private Date start;
	private Date end;
	
	private final DateFormat dateFormat;
	private final DecimalFormat decimalFormat;
	
	protected Long max;
	protected Long min;
	protected Double median;
	protected Double mean;
	protected Long sum;
	private Double stdDev;
	
	private Date maxTimestamp;
	private Date minTimestamp;
	private Date firstUsageTimestamp;
	private Date lastUsageTimestamp;
	
	private Map<String, Object> attributes = Collections.emptyMap();
	
	public MetricResponse() {
		this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		this.decimalFormat = new DecimalFormat("0.##");
	}
	
	@XmlElement
	public String getMedian() {
		return (median != null) ? decimalFormat.format(median) : null;
	}

	@XmlElement
	public String getMax() {
		return (max != null) ? Long.toString(max) : null;
	}

	@XmlElement
	public String getMean() {
		return (mean != null) ? decimalFormat.format(this.mean) : null;
	}

	@XmlElement
	public String getMin() {
		return (min != null ) ? Long.toString(min) : null;
	}
	
	@XmlElement
	public String getStdDev() {
		return (stdDev != null) ? decimalFormat.format(stdDev) : null;
	}
	
	@XmlElement
    public String getSum() {
        return (sum != null) ? Long.toString(sum) : null;
    }
	
	public void setStdDev(double stdDev, double scalingFactor) {
	    if(!Double.isNaN(stdDev)) {
	        this.stdDev = stdDev * scalingFactor;
	    }
	}

	@XmlElement
	public String getPath() {
		return path;
	}
	
	public void setPath(String path) {
		this.path = path;
	}

	@XmlElement
	public Map<String, Object> getAttributes() {
		return attributes;
	}
	
	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}
	
	@XmlElement
	public String getMaxTimestamp() {
		return (maxTimestamp != null) ? dateFormat.format(maxTimestamp) : null;
	}
	
	public void setMaxTimestamp(long maxTimestamp, UnitOfTime unit) {
	    if(maxTimestamp != Long.MIN_VALUE) {
	        long maxTimestampMillis = UnitOfTime.MILLISECONDS.convert(maxTimestamp, unit);
	        this.maxTimestamp = new Date(maxTimestampMillis);
	    }
	}

	@XmlElement
	public String getMinTimestamp() {
		return (minTimestamp != null) ? dateFormat.format(minTimestamp) : null;
	}
	
	public void setMinTimestamp(long minTimestamp, UnitOfTime unit) {
	    if(minTimestamp != Long.MIN_VALUE) {
	        long minTimestampMillis = UnitOfTime.MILLISECONDS.convert(minTimestamp, unit);
	        this.minTimestamp = new Date(minTimestampMillis);
	    }
	}

	@XmlElement
	public String getFirstUsageTimestamp() {
		return (firstUsageTimestamp != null) ? dateFormat.format(firstUsageTimestamp) : null;
	}
	
	public void setFirstUsageTimestamp(long firstUsageTimestamp, UnitOfTime unit) {
	    if(firstUsageTimestamp != Long.MIN_VALUE) {
	        long firstUsageTimestampMillis = UnitOfTime.MILLISECONDS.convert(firstUsageTimestamp, unit);
	        this.firstUsageTimestamp = new Date(firstUsageTimestampMillis);
	    }
	}

	@XmlElement
	public String getLastUsageTimestamp() {
		return (lastUsageTimestamp != null) ? dateFormat.format(lastUsageTimestamp) : null;
	}

	public void setLastUsageTimestamp(long lastUsageTimestamp, UnitOfTime unit) {
	    if(lastUsageTimestamp != Long.MIN_VALUE) {
	        long lastUsageTimestampMillis = UnitOfTime.MILLISECONDS.convert(lastUsageTimestamp, unit);
	        this.lastUsageTimestamp = new Date(lastUsageTimestampMillis);
	    }
	}
	
	@XmlElement
	public String getStart() {
		return (start != null) ? dateFormat.format(start) : null;
	}
	
	public void setStart(long start, UnitOfTime unit) {
	    if(start != Long.MIN_VALUE) {
	        long startMillis = UnitOfTime.MILLISECONDS.convert(start, unit);
	        this.start = new Date(startMillis);
	    }
	}
	
	@XmlElement
	public String getEnd() {
		return (end != null) ? dateFormat.format(end) : null;
	}
	
	public void setEnd(long end, UnitOfTime unit) {
	    if(end != Long.MIN_VALUE) {
	        long endMillis = UnitOfTime.MILLISECONDS.convert(end, unit);
	        this.end = new Date(endMillis);
	    }
	}
	
}
