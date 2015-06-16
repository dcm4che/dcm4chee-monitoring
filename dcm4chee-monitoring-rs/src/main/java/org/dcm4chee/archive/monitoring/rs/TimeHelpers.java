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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dcm4chee.archive.monitoring.impl.core.Util;
import org.dcm4chee.archive.monitoring.impl.core.clocks.Clock;
import org.dcm4chee.archive.monitoring.impl.util.UnitOfTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 *
 */
public class TimeHelpers {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimeHelpers.class);
    
    private final static DateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    // some digits followed by some chars
    private static final Pattern resolutionStringPattern = Pattern.compile("([\\d]+)([a-zA-Z]+)");
    
    private static final Pattern currentMinutePlusOffsetPattern = Pattern.compile("minNow(([+-])?([\\d]+))?");
    
    

    private TimeHelpers() {
        // NOOP
    }

    public static Date parseDate(String dateString) {
        try {
            return dateParser.parse(dateString);
        } catch (Exception e) {
            return null;
        }
    }
    
    public static Date parseNaturalDateDescription(Clock clock, String dateDescription) {
        Matcher m = currentMinutePlusOffsetPattern.matcher(dateDescription);
        if(m.matches()) {
            long currentMinute = Util.getTimeInMinuteResolution(clock.getTime());
            
            if(m.groupCount() > 1 && m.group(1) != null) {
                boolean addOffset = m.group(2) == null || "+".equals(m.group(2));
                long offset = Long.parseLong(m.group(3));
                
                if(addOffset) {
                    long time = currentMinute + UnitOfTime.MINUTES.toMillis(offset);
                    return new Date(time);                
                } else {
                    long time = currentMinute - UnitOfTime.MINUTES.toMillis(offset);
                    return new Date(time);       
                } 
            }
            
            return new Date(currentMinute);
        }
        return null;
    }
    
    public static long parseResolutionString(String resolutionString) {
        try {
            Matcher m = resolutionStringPattern.matcher(resolutionString);
            if (m.matches()) {
                long res = Long.parseLong(m.group(1));
                String timeUnitString = m.group(2);
                UnitOfTime timeUnit = parseTimeUnit(timeUnitString);
                return (timeUnit != null) ? timeUnit.toMillis(res) : Long.MIN_VALUE;
            }
        } catch (Exception e) {
            LOGGER.error("Error while parsing time resolution string", e);
        }

        return Long.MIN_VALUE;
    }
    
    public static UnitOfTime parseTimeUnit(String timeUnit) {
        try {
            switch (timeUnit.toUpperCase()) {
            case "MS":
                return UnitOfTime.MILLISECONDS;
            case "NS":
                return UnitOfTime.NANOSECONDS;
            case "S":
                return UnitOfTime.SECONDS;
            case "SEC":
                return UnitOfTime.SECONDS;
            case "M":
                return UnitOfTime.MINUTES;
            case "H":
                return UnitOfTime.HOURS;
            case "D":
                return UnitOfTime.DAYS;
            default:
                throw new IllegalArgumentException("Unknown time unit: " + timeUnit);
            }
        } catch (Exception e) {
            LOGGER.error("Error while parsing time unit string", e);
        }

        return null;
    }
       
}
