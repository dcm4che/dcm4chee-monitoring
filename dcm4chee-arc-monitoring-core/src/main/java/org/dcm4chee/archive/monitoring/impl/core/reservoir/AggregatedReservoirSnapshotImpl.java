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

import static java.lang.Math.floor;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.dcm4chee.archive.monitoring.impl.util.UnitOfTime;



/**
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 *
 */
public class AggregatedReservoirSnapshotImpl implements
        AggregatedReservoirSnapshot {
    private long start;
    private long end;
    private long size;

    private long[] values;
    private long lastValue;
    private long sum;
    private double mean;
    private double stdDev;
    private long max;
    private long maxTimestamp;
    private long min;
    private long minTimestamp;
    private long firstUsageTimestamp;
    private long lastUsageTimestamp;

    private String path;
    private Map<String, Object> attributes = Collections.emptyMap();
    private MEAN_RATE_CALC_METHOD meanRateCalcMethod = MEAN_RATE_CALC_METHOD.UNDEFINED;

    public static enum MEAN_RATE_CALC_METHOD {
        RELATIVE, ABSOLUTE, UNDEFINED
    }

    public AggregatedReservoirSnapshotImpl() {
        // NOOP
    }

    @Override
    public void setMeanRateCalculatiuonMethod(MEAN_RATE_CALC_METHOD meanRateCalcMethod) {
        this.meanRateCalcMethod = meanRateCalcMethod;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    @Override
    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
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

    public void setSize(long size) {
        this.size = size;
    }

    @Override
    public double getMedian() {
        return getValue(0.5);
    }
    
    /**
     * Returns the value at the given quantile.
     *
     * @param quantile
     *            a given quantile, in {@code [0..1]}
     * @return the value in the distribution at {@code quantile}
     */
    @Override
    public double getValue(double quantile) {
        if (quantile < 0.0 || quantile > 1.0) {
            throw new IllegalArgumentException(quantile + " is not in [0..1]");
        }

        if (values == null || values.length == 0) {
            return Double.NaN;
        }

        final double pos = quantile * (values.length + 1);

        if (pos < 1) {
            return values[0];
        }

        if (pos >= values.length) {
            return values[values.length - 1];
        }

        final double lower = values[(int) pos - 1];
        final double upper = values[(int) pos];
        return lower + (pos - floor(pos)) * (upper - lower);
    }

    @Override
    public long[] getValues(boolean copy) {
        return (values != null) ? (copy ? Arrays.copyOf(values, values.length) : values) : null;
    }
    
    public void setValues(long[] values) {
        this.values = values;
    }
    
    @Override
    public long getLastValue() {
        return lastValue;
    }
    
    public void setLastValue(long lastValue) {
        this.lastValue = lastValue;
    }
    
    @Override
    public long getSum() {
        return sum;
    }
    
    public void setSum(long sum) {
        this.sum = sum;
    }

    @Override
    public double getMeanRate(long resolution, UnitOfTime timeUnit) {
        switch (meanRateCalcMethod) {
        case ABSOLUTE:
            return getMeanRateAbs(resolution, timeUnit);
        case RELATIVE:
            return getMeanRateRel(resolution, timeUnit);
        case UNDEFINED:
            return Double.NaN;
        default:
            throw new IllegalArgumentException("Unknwon mean rate calculation method " + meanRateCalcMethod);
        }
    }
    
    private double getMeanRateAbs(long resolution, UnitOfTime timeUnit) {
        if (sum == 0 || sum == Long.MIN_VALUE) {
            return 0.0;
        } else {
            final double elapsed = (end - start);
            return sum / elapsed * timeUnit.toMillis(resolution);
        }
    }
    
    private double getMeanRateRel(long resolution, UnitOfTime timeUnit) {
        if (sum == 0 || sum == Long.MIN_VALUE) {
            return 0.0;
        } else {
            double elapsed = end - start;
            if(start != 0) {
                elapsed++;
            }
            return sum * (timeUnit.toMillis(resolution) / elapsed);
        }
    }

}
