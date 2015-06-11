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

package org.dcm4chee.archive.monitoring.impl.util;


/**
 * Extended implementation of {@link java.util.concurrent.TimeUnit}. Adds support
 * for conversion of time durations of type double.
 * 
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 */
public enum UnitOfTime {
    NANOSECONDS {
        public long toNanos(long d)   { return d; }
        public long toMicros(long d)  { return d/(C1/C0); }
        public long toMillis(long d)  { return d/(C2/C0); }
        public long toSeconds(long d) { return d/(C3/C0); }
        public long toMinutes(long d) { return d/(C4/C0); }
        public long toHours(long d)   { return d/(C5/C0); }
        public long toDays(long d)    { return d/(C6/C0); }
        public long convert(long d, UnitOfTime u) { return u.toNanos(d); }
        
        public double toNanos(double d)   { return d; }
        public double toMicros(double d)  { return d/(C1/C0); }
        public double toMillis(double d)  { return d/(C2/C0); }
        public double toSeconds(double d) { return d/(C3/C0); }
        public double toMinutes(double d) { return d/(C4/C0); }
        public double toHours(double d)   { return d/(C5/C0); }
        public double toDays(double d)    { return d/(C6/C0); }
        public double convert(double d, UnitOfTime u) { return u.toNanos(d); }
        public double scaleFromNano() { return C0; }
    },
    MICROSECONDS {
        public long toNanos(long d)   { return x(d, C1/C0, MAX/(C1/C0)); }
        public long toMicros(long d)  { return d; }
        public long toMillis(long d)  { return d/(C2/C1); }
        public long toSeconds(long d) { return d/(C3/C1); }
        public long toMinutes(long d) { return d/(C4/C1); }
        public long toHours(long d)   { return d/(C5/C1); }
        public long toDays(long d)    { return d/(C6/C1); }
        public long convert(long d, UnitOfTime u) { return u.toMicros(d); }
        
        public double toNanos(double d)   { return x(d, C1/C0, MAX/(C1/C0)); }
        public double toMicros(double d)  { return d; }
        public double toMillis(double d)  { return d/(C2/C1); }
        public double toSeconds(double d) { return d/(C3/C1); }
        public double toMinutes(double d) { return d/(C4/C1); }
        public double toHours(double d)   { return d/(C5/C1); }
        public double toDays(double d)    { return d/(C6/C1); }
        public double convert(double d, UnitOfTime u) { return u.toMicros(d); }
        public double scaleFromNano() { return 1.0/C1; }
    },
    MILLISECONDS {
        public long toNanos(long d)   { return x(d, C2/C0, MAX/(C2/C0)); }
        public long toMicros(long d)  { return x(d, C2/C1, MAX/(C2/C1)); }
        public long toMillis(long d)  { return d; }
        public long toSeconds(long d) { return d/(C3/C2); }
        public long toMinutes(long d) { return d/(C4/C2); }
        public long toHours(long d)   { return d/(C5/C2); }
        public long toDays(long d)    { return d/(C6/C2); }
        public long convert(long d, UnitOfTime u) { return u.toMillis(d); }
        
        public double toNanos(double d)   { return x(d, C2/C0, MAX/(C2/C0)); }
        public double toMicros(double d)  { return x(d, C2/C1, MAX/(C2/C1)); }
        public double toMillis(double d)  { return d; }
        public double toSeconds(double d) { return d/(C3/C2); }
        public double toMinutes(double d) { return d/(C4/C2); }
        public double toHours(double d)   { return d/(C5/C2); }
        public double toDays(double d)    { return d/(C6/C2); }
        public double convert(double d, UnitOfTime u) { return u.toMillis(d); }
        public double scaleFromNano() { return 1.0/C2; }
    },
    SECONDS {
        public long toNanos(long d)   { return x(d, C3/C0, MAX/(C3/C0)); }
        public long toMicros(long d)  { return x(d, C3/C1, MAX/(C3/C1)); }
        public long toMillis(long d)  { return x(d, C3/C2, MAX/(C3/C2)); }
        public long toSeconds(long d) { return d; }
        public long toMinutes(long d) { return d/(C4/C3); }
        public long toHours(long d)   { return d/(C5/C3); }
        public long toDays(long d)    { return d/(C6/C3); }
        public long convert(long d, UnitOfTime u) { return u.toSeconds(d); }
        
        public double toNanos(double d)   { return x(d, C3/C0, MAX/(C3/C0)); }
        public double toMicros(double d)  { return x(d, C3/C1, MAX/(C3/C1)); }
        public double toMillis(double d)  { return x(d, C3/C2, MAX/(C3/C2)); }
        public double toSeconds(double d) { return d; }
        public double toMinutes(double d) { return d/(C4/C3); }
        public double toHours(double d)   { return d/(C5/C3); }
        public double toDays(double d)    { return d/(C6/C3); }
        public double convert(double d, UnitOfTime u) { return u.toSeconds(d); }
        public double scaleFromNano() { return 1.0/C3; }
    },
    MINUTES {
        public long toNanos(long d)   { return x(d, C4/C0, MAX/(C4/C0)); }
        public long toMicros(long d)  { return x(d, C4/C1, MAX/(C4/C1)); }
        public long toMillis(long d)  { return x(d, C4/C2, MAX/(C4/C2)); }
        public long toSeconds(long d) { return x(d, C4/C3, MAX/(C4/C3)); }
        public long toMinutes(long d) { return d; }
        public long toHours(long d)   { return d/(C5/C4); }
        public long toDays(long d)    { return d/(C6/C4); }
        public long convert(long d, UnitOfTime u) { return u.toMinutes(d); }
        
        public double toNanos(double d)   { return x(d, C4/C0, MAX/(C4/C0)); }
        public double toMicros(double d)  { return x(d, C4/C1, MAX/(C4/C1)); }
        public double toMillis(double d)  { return x(d, C4/C2, MAX/(C4/C2)); }
        public double toSeconds(double d) { return x(d, C4/C3, MAX/(C4/C3)); }
        public double toMinutes(double d) { return d; }
        public double toHours(double d)   { return d/(C5/C4); }
        public double toDays(double d)    { return d/(C6/C4); }
        public double convert(double d, UnitOfTime u) { return u.toMinutes(d); }
        public double scaleFromNano() { return 1.0/C4; }
    },
    HOURS {
        public long toNanos(long d)   { return x(d, C5/C0, MAX/(C5/C0)); }
        public long toMicros(long d)  { return x(d, C5/C1, MAX/(C5/C1)); }
        public long toMillis(long d)  { return x(d, C5/C2, MAX/(C5/C2)); }
        public long toSeconds(long d) { return x(d, C5/C3, MAX/(C5/C3)); }
        public long toMinutes(long d) { return x(d, C5/C4, MAX/(C5/C4)); }
        public long toHours(long d)   { return d; }
        public long toDays(long d)    { return d/(C6/C5); }
        public long convert(long d, UnitOfTime u) { return u.toHours(d); }
        
        public double toNanos(double d)   { return x(d, C5/C0, MAX/(C5/C0)); }
        public double toMicros(double d)  { return x(d, C5/C1, MAX/(C5/C1)); }
        public double toMillis(double d)  { return x(d, C5/C2, MAX/(C5/C2)); }
        public double toSeconds(double d) { return x(d, C5/C3, MAX/(C5/C3)); }
        public double toMinutes(double d) { return x(d, C5/C4, MAX/(C5/C4)); }
        public double toHours(double d)   { return d; }
        public double toDays(double d)    { return d/(C6/C5); }
        public double convert(double d, UnitOfTime u) { return u.toHours(d); }
        public double scaleFromNano() { return 1.0/C5; }
    },
    DAYS {
        public long toNanos(long d)   { return x(d, C6/C0, MAX/(C6/C0)); }
        public long toMicros(long d)  { return x(d, C6/C1, MAX/(C6/C1)); }
        public long toMillis(long d)  { return x(d, C6/C2, MAX/(C6/C2)); }
        public long toSeconds(long d) { return x(d, C6/C3, MAX/(C6/C3)); }
        public long toMinutes(long d) { return x(d, C6/C4, MAX/(C6/C4)); }
        public long toHours(long d)   { return x(d, C6/C5, MAX/(C6/C5)); }
        public long toDays(long d)    { return d; }
        public long convert(long d, UnitOfTime u) { return u.toDays(d); }
        
        public double toNanos(double d)   { return x(d, C6/C0, MAX/(C6/C0)); }
        public double toMicros(double d)  { return x(d, C6/C1, MAX/(C6/C1)); }
        public double toMillis(double d)  { return x(d, C6/C2, MAX/(C6/C2)); }
        public double toSeconds(double d) { return x(d, C6/C3, MAX/(C6/C3)); }
        public double toMinutes(double d) { return x(d, C6/C4, MAX/(C6/C4)); }
        public double toHours(double d)   { return x(d, C6/C5, MAX/(C6/C5)); }
        public double toDays(double d)    { return d; }
        public double convert(double d, UnitOfTime u) { return u.toDays(d); }
        public double scaleFromNano() { return 1.0/C6; }
    };

    // Handy constants for conversion methods
    static final long C0 = 1L;
    static final long C1 = C0 * 1000L;
    static final long C2 = C1 * 1000L;
    static final long C3 = C2 * 1000L;
    static final long C4 = C3 * 60L;
    static final long C5 = C4 * 60L;
    static final long C6 = C5 * 24L;

    static final long MAX = Long.MAX_VALUE;

    /**
     * Scale d by m, checking for overflow.
     * This has a short name to make above code more readable.
     */
    static long x(long d, long m, long over) {
        if (d >  over) return Long.MAX_VALUE;
        if (d < -over) return Long.MIN_VALUE;
        return d * m;
    }
    
    /**
     * Scale d by m, checking for overflow.
     * This has a short name to make above code more readable.
     */
    static double x(double d, double m, double over) {
        if (d >  over) return Double.MAX_VALUE;
        if (d < -over) return Double.MIN_VALUE;
        return d * m;
    }

    // To maintain full signature compatibility with 1.5, and to improve the
    // clarity of the generated javadoc (see 6287639: Abstract methods in
    // enum classes should not be listed as abstract), method convert
    // etc. are not declared abstract but otherwise act as abstract methods.

    /**
     * Convert the given time duration in the given unit to this
     * unit.  Conversions from finer to coarser granularities
     * truncate, so lose precision. For example converting
     * <tt>999</tt> milliseconds to seconds results in
     * <tt>0</tt>. Conversions from coarser to finer granularities
     * with arguments that would numerically overflow saturate to
     * <tt>Long.MIN_VALUE</tt> if negative or <tt>Long.MAX_VALUE</tt>
     * if positive.
     *
     * <p>For example, to convert 10 minutes to milliseconds, use:
     * <tt>TimeUnit.MILLISECONDS.convert(10L, TimeUnit.MINUTES)</tt>
     *
     * @param sourceDuration the time duration in the given <tt>sourceUnit</tt>
     * @param sourceUnit the unit of the <tt>sourceDuration</tt> argument
     * @return the converted duration in this unit,
     * or <tt>Long.MIN_VALUE</tt> if conversion would negatively
     * overflow, or <tt>Long.MAX_VALUE</tt> if it would positively overflow.
     */
    public long convert(long sourceDuration, UnitOfTime sourceUnit) {
        throw new AbstractMethodError();
    }
    
    public double convert(double sourceDuration, UnitOfTime sourceUnit) {
        throw new AbstractMethodError();
    }
    
    /**
     * Returns the scaling factor to convert from nanoseconds to this time unit
     * @return
     */
    public double scaleFromNano() {
        throw new AbstractMethodError();
    }

    /**
     * Equivalent to <tt>NANOSECONDS.convert(duration, this)</tt>.
     * @param duration the duration
     * @return the converted duration,
     * or <tt>Long.MIN_VALUE</tt> if conversion would negatively
     * overflow, or <tt>Long.MAX_VALUE</tt> if it would positively overflow.
     * @see #convert
     */
    public long toNanos(long duration) {
        throw new AbstractMethodError();
    }
    
    public double toNanos(double duration) {
        throw new AbstractMethodError();
    }

    /**
     * Equivalent to <tt>MICROSECONDS.convert(duration, this)</tt>.
     * @param duration the duration
     * @return the converted duration,
     * or <tt>Long.MIN_VALUE</tt> if conversion would negatively
     * overflow, or <tt>Long.MAX_VALUE</tt> if it would positively overflow.
     * @see #convert
     */
    public long toMicros(long duration) {
        throw new AbstractMethodError();
    }
    
    public double toMicros(double duration) {
        throw new AbstractMethodError();
    }

    /**
     * Equivalent to <tt>MILLISECONDS.convert(duration, this)</tt>.
     * @param duration the duration
     * @return the converted duration,
     * or <tt>Long.MIN_VALUE</tt> if conversion would negatively
     * overflow, or <tt>Long.MAX_VALUE</tt> if it would positively overflow.
     * @see #convert
     */
    public long toMillis(long duration) {
        throw new AbstractMethodError();
    }
    
    public double toMillis(double duration) {
        throw new AbstractMethodError();
    }

    /**
     * Equivalent to <tt>SECONDS.convert(duration, this)</tt>.
     * @param duration the duration
     * @return the converted duration,
     * or <tt>Long.MIN_VALUE</tt> if conversion would negatively
     * overflow, or <tt>Long.MAX_VALUE</tt> if it would positively overflow.
     * @see #convert
     */
    public long toSeconds(long duration) {
        throw new AbstractMethodError();
    }
    
    public double toSeconds(double duration) {
        throw new AbstractMethodError();
    }

    /**
     * Equivalent to <tt>MINUTES.convert(duration, this)</tt>.
     * @param duration the duration
     * @return the converted duration,
     * or <tt>Long.MIN_VALUE</tt> if conversion would negatively
     * overflow, or <tt>Long.MAX_VALUE</tt> if it would positively overflow.
     * @see #convert
     * @since 1.6
     */
    public long toMinutes(long duration) {
        throw new AbstractMethodError();
    }
    
    public double toMinutes(double duration) {
        throw new AbstractMethodError();
    }

    /**
     * Equivalent to <tt>HOURS.convert(duration, this)</tt>.
     * @param duration the duration
     * @return the converted duration,
     * or <tt>Long.MIN_VALUE</tt> if conversion would negatively
     * overflow, or <tt>Long.MAX_VALUE</tt> if it would positively overflow.
     * @see #convert
     * @since 1.6
     */
    public long toHours(long duration) {
        throw new AbstractMethodError();
    }
    
    public double toHours(double duration) {
        throw new AbstractMethodError();
    }

    /**
     * Equivalent to <tt>DAYS.convert(duration, this)</tt>.
     * @param duration the duration
     * @return the converted duration
     * @see #convert
     * @since 1.6
     */
    public long toDays(long duration) {
        throw new AbstractMethodError();
    }
    
    public double toDays(double duration) {
        throw new AbstractMethodError();
    }

}

