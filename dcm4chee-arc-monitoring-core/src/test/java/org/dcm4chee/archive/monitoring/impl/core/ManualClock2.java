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

package org.dcm4chee.archive.monitoring.impl.core;

import org.dcm4chee.archive.monitoring.impl.core.clocks.Clocks.UserTimeClock;

/**
 * A clock implementation that only lets the time progress on explicit request.
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 *
 */
public class ManualClock2 extends UserTimeClock {
	private long currentTick;
	private final long tickSize;
	
	private long currentTock;
    private final long tockSize;

	public static class Builder {
	    private long startTick;
	    private long tickSize;
	    
	    private long startTock;
        private long tockSize;
	    
	    public Builder tick(long startTick, long tickSize) {
	        this.startTick = startTick;
	        this.tickSize = tickSize;
	        return this;
	    }
	    
	    public Builder tock(long startTock, long tockSize) {
	        this.startTock = startTock;
	        this.tockSize = tockSize;
	        return this;
	    }
	    
	    public ManualClock2 build() {
	        return new ManualClock2(this);
	    }
	    
	}
	
	private ManualClock2(Builder builder) {
		currentTick = builder.startTick;
		tickSize = builder.tickSize;
		
		currentTock = builder.startTock;
        tockSize = builder.tockSize;
	}
	
	@Override
	public long getTick() {
		return currentTick;
	}
	
	@Override
    public long getTime() {
	    return currentTock;
    }
	
	/**
	 * Progresses the time by one tick as specified by tick-size.
	 */
	public void tick() {
		tick(1);
	}
	
	/**
     * Progresses the time by one tock as specified by tock-size.
     */
    public long tock() {
        return tock(1);
    }
	
	/**
	 * Progresses the time by N ticks where the size of a tick is specified by tick-size.
	 * @param ticks 
	 */
	public void tick(int ticks) {
		if (ticks <= 0) {
			throw new IllegalArgumentException("Number of time ticks must be greater 0");
		}
		
		currentTick += (tickSize * ticks);
	}
	
	/**
     * Progresses the time by N tocks where the size of a tock is specified by tock-size.
     * @param ticks 
     */
    public long tock(int tocks) {
        currentTock += (tockSize * tocks);
        return currentTock;
    }

}

