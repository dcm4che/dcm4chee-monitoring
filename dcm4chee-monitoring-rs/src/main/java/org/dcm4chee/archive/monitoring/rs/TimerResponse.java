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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.dcm4chee.archive.monitoring.impl.core.reservoir.AggregatedReservoirSnapshot;
import org.dcm4chee.archive.monitoring.impl.util.UnitOfTime;


/**
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 *
 */
@JsonPropertyOrder({
	"timeUnit"
})
public class TimerResponse extends MetricResponse {
	private final UnitOfTime timeUnit;
	private long size;
	
	private TimerResponse(UnitOfTime timeUnit) {
		this.timeUnit = timeUnit;
	}
	
	public static TimerResponse create(AggregatedReservoirSnapshot snapshot, UnitOfTime timeUnit) {
		TimerResponse response = new TimerResponse(timeUnit);
		response.setPath(snapshot.getPath());
		response.setAttributes(snapshot.getAttributes());
		response.setStart(snapshot.getStart(), UnitOfTime.MILLISECONDS);
        response.setEnd(snapshot.getEnd(), UnitOfTime.MILLISECONDS);
        response.setSize(snapshot.size());
		response.setFirstUsageTimestamp(snapshot.getFirstUsageTimestamp(), UnitOfTime.MILLISECONDS);
		response.setLastUsageTimestamp(snapshot.getLastUsageTimestamp(), UnitOfTime.MILLISECONDS);
		response.setMin(snapshot.getMin(), UnitOfTime.NANOSECONDS);
		response.setMinTimestamp(snapshot.getMinTimestamp(), UnitOfTime.MILLISECONDS);
		response.setMax(snapshot.getMax(), UnitOfTime.NANOSECONDS);
		response.setMaxTimestamp(snapshot.getMaxTimestamp(), UnitOfTime.MILLISECONDS);
		response.setMean(snapshot.getMean(), UnitOfTime.NANOSECONDS);
		response.setStdDev(snapshot.getStdDev(), timeUnit.scaleFromNano());
		response.setMedian(snapshot.getMedian(), UnitOfTime.NANOSECONDS);
		response.setSum(snapshot.getSum(), UnitOfTime.NANOSECONDS);
		return response;
	}
	
	@XmlAttribute
	public String getTimeUnit() {
		return timeUnit.toString();
	}
	
	@XmlElement
	public long getSize() {
	    return size;
	}
	
	public void setSize(long size) {
	    this.size = size;
	}
	
	public void setMin(long min, UnitOfTime unit) {
        if(min != Long.MAX_VALUE) {
            this.min = convert(min, unit);
        }
    }
    
    public void setMax(long max, UnitOfTime unit) {
        if(max != Long.MIN_VALUE) {
            this.max = convert(max, unit);
        }
    }
    
    public void setMedian(double median, UnitOfTime unit) {
        if(!Double.isNaN(median)) {
            this.median = convert(median, unit);
        }
    }
    
    public void setSum(long sum, UnitOfTime unit) {
        if(sum != Long.MIN_VALUE) {
            this.sum = convert(sum, unit);
        }
    }
    
    public void setMean(double mean, UnitOfTime unit) {
        if(!Double.isNaN(mean)) {
            this.mean = convert(mean, unit);
        }
    }
	
	protected long convert(long d, UnitOfTime unit) {
		return this.timeUnit.convert(d, unit);
	}
	
	protected double convert(double d, UnitOfTime unit) {
		return this.timeUnit.convert(d, unit);
	}
	
}
