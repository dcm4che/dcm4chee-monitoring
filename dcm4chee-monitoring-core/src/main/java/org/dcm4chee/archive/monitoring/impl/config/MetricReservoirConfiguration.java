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

package org.dcm4chee.archive.monitoring.impl.config;

import org.dcm4chee.archive.monitoring.impl.core.reservoir.ReservoirBuilder.START_SPECIFICATION;

/**
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 *
 */
public class MetricReservoirConfiguration {
    public static enum RESERVOIR_TYPE {
        OPEN_RESOLUTION,
        ROUND_ROBIN
    }
    
    private String name;
    private RESERVOIR_TYPE type;
    private long resolutionStepSize;
    private long[] resolutions;
    private int[] maxRawValues;
    private int[] retentions;
    private START_SPECIFICATION start;
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RESERVOIR_TYPE getType() {
        return type;
    }
    
    public void setType(RESERVOIR_TYPE type) {
        this.type = type;
    }
    
    public long getResolutionStepSize() {
        return resolutionStepSize;
    }
    
    public void setResolutionStepSize(long resolutionStepSize) {
        this.resolutionStepSize = resolutionStepSize;
    }
    
    public long[] getResolutions() {
        return resolutions;
    }
    
    public void setResolutions(long... resolutions) {
        this.resolutions = resolutions;
    }
    
    public int[] getMaxRawValues() {
        return maxRawValues;
    }

    public void setMaxRawValues(int[] maxRawValues) {
        this.maxRawValues = maxRawValues;
    }

    public int[] getRetentions() {
        return retentions;
    }
    
    public void setRetentions(int... retentions) {
        this.retentions = retentions;
    }

    public START_SPECIFICATION getStart() {
        return start;
    }

    public void setStart(START_SPECIFICATION start) {
        this.start = start;
    }
    
}
