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

import org.dcm4chee.archive.monitoring.impl.core.clocks.Clock;
import org.dcm4chee.archive.monitoring.impl.core.context.MonitoringContext;
import org.dcm4chee.archive.monitoring.impl.core.reservoir.AggregatedReservoir;
import org.dcm4chee.archive.monitoring.impl.core.reservoir.Reservoir;


/**
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 *
 */
public class ForwardingTimerImpl extends TimerImpl {
	private final Reservoir forwardReservoir;
	
	public ForwardingTimerImpl(MonitoringContext context, AggregatedReservoir aggregatedReservoir, Clock clock, Reservoir forwardReservoir) {
		super(context, aggregatedReservoir, clock);
		this.forwardReservoir = forwardReservoir;
	}
	
	protected static class AggregatableTimerSplitImpl extends TimerImpl.TimerSplitImpl {
		private final MonitoringContext context;
		private final Reservoir forwardReservoir;
		
		protected AggregatableTimerSplitImpl(MonitoringContext context, Reservoir forwardReservoir, TimerImpl timer, Clock clock)
		{
			super(timer, clock);
			this.context = context;
			this.forwardReservoir = forwardReservoir;
		}
		
		@Override
		public long stop() {
			long duration = super.stop();
			forwardReservoir.update(context, now, duration);
			return duration;
		}

	}
	
	@Override
	protected Timer.Split newSplit() {
		return new AggregatableTimerSplitImpl(context, forwardReservoir, this, clock);
	}

}
