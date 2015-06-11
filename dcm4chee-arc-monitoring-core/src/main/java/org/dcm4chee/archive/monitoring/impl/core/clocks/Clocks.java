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

package org.dcm4chee.archive.monitoring.impl.core.clocks;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/**
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 *
 */
public class Clocks {
	private static final Clock DEFAULT = new UserTimeClock();

	/**
	 * The default clock to use.
	 *
	 * @return the default {@link Clock} instance
	 *
	 * @see Clock.UserTimeClock
	 */
	public static Clock defaultClock() {
		return DEFAULT;
	}

	protected static abstract class AbstractClock implements Clock {

        @Override
        public Clock getClock() {
            return this;
        }

        @Override
        public long getTime() {
            return System.currentTimeMillis();
        }
	    
	}
	
	/**
	 * A clock implementation which returns the current time in epoch
	 * nanoseconds.
	 */
	public static class UserTimeClock extends AbstractClock {
	    
		@Override
		public long getTick() {
			return System.nanoTime();
		}
		
	}

	/**
	 * A clock implementation which returns the current thread's CPU time.
	 */
	public static class CpuTimeClock extends AbstractClock {
		private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();

		@Override
		public long getTick() {
			return THREAD_MX_BEAN.getCurrentThreadCpuTime();
		}
		
	}
}
