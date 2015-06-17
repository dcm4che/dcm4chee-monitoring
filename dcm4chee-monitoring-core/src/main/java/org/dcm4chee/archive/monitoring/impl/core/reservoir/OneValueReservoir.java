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

package org.dcm4chee.archive.monitoring.impl.core.reservoir;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.dcm4chee.archive.monitoring.impl.core.clocks.Clock;
import org.dcm4chee.archive.monitoring.impl.core.context.MonitoringContext;

/**
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 *
 */
public class OneValueReservoir extends AbstractAggregatedReservoir {
    private final long start;
    private final Clock clock;
    
	private long value = Long.MIN_VALUE;
	
	private int size;
	private long sum = Long.MIN_VALUE;
    private double m2;
    private double mean = Double.NaN;
    
    public OneValueReservoir(Clock clock) {
        this.start = clock.getTime();
        this.clock = clock;
    }
	
	@Override
	public void update(MonitoringContext context, long now, long value) {		
		updateUsages(now);
		
		size++;
		
        if (sum == Long.MIN_VALUE) {
            sum = value;
        } else {
            sum += value;
        }
        
        // set mean to default value for calculation
        if(Double.isNaN(mean)) {
            mean = 0.0;
        }
        
        /*
         * Calculation of mean and variance based on D. Knuths Online Variance algorithm
         * see: http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance (section Online algorithm)
         */
        double delta = value - mean;
        mean = mean + delta / size;
        m2 += delta * (value - mean);
		
		updateMinMax(value);
		
		this.value = value;
	}
	
	private double getVariance() {
        if(size < 2) {
            return Double.NaN;
        }
        
        return m2 / (size - 1);
    }
    
    private double getStdDev() {
        return Math.sqrt(getVariance());
    }

    @Override
    public AggregatedReservoirSnapshot getCurrentSnapshot() {
        AggregatedReservoirSnapshotImpl snapshot = new AggregatedReservoirSnapshotImpl();
        snapshot.setStart(start);
        snapshot.setEnd(clock.getTime());
        snapshot.setLastValue(value);
        snapshot.setSum(sum);
        snapshot.setFirstUsageTimestamp(firstUsage);
        snapshot.setLastUsageTimestamp(lastUsage);
        snapshot.setMinTimestamp(minTimestamp);
        snapshot.setMaxTimestamp(maxTimestamp);
        snapshot.setMax(max);
        snapshot.setMin(min);
        snapshot.setSize(size);
        snapshot.setMean(mean);
        snapshot.setStdDev(getStdDev());
        return snapshot;
    }

    @Override
    public List<AggregatedReservoirSnapshot> getSnapshots(long start, long end, long resolution) {
        return Collections.emptyList();
    }
    
    @Override
    public List<AggregatedReservoirSnapshot> getSnapshots() {
        return Arrays.asList(getCurrentSnapshot());
    }

}
