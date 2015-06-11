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

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.dcm4chee.archive.monitoring.impl.core.clocks.Clock;
import org.dcm4chee.archive.monitoring.impl.core.context.MonitoringContext;
import org.dcm4chee.archive.monitoring.impl.core.reservoir.AggregatedReservoir;
import org.dcm4chee.archive.monitoring.impl.core.reservoir.AggregatedReservoirSnapshot;
import org.dcm4chee.archive.monitoring.impl.util.LongAdder;


/**
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 *
 */
public class CounterImpl extends AbstractMetric implements Counter {
    private final MonitoringContext context;
    private final Clock clock;

    private final CounterImplState state;
    private final AtomicReference<CounterImplState> stateRef;

    public CounterImpl(MonitoringContext context, AggregatedReservoir reservoir,
            Clock clock) {
        this.context = context;
        this.clock = clock;
        this.state = new CounterImplState(reservoir);
        this.stateRef = new AtomicReference<>(state);
    }
	
    protected class CounterImplState {
        protected final AggregatedReservoir reservoir;
        private final LongAdder count;

        protected CounterImplState(AggregatedReservoir reservoir) {
            this.reservoir = reservoir;
            count = new LongAdder();
        }

        protected void update(long now, long n) {
            count.add(n);
            reservoir.update(context, now, count.sum());
        }
    }

    /**
     * Increment the counter by one.
     */
    public void inc() {
        inc(1);
    }

    /**
     * Increment the counter by {@code n}.
     *
     * @param n
     *            the amount by which the counter will be increased
     */
    public void inc(long n) {
        updateCount(n);
    }

    /**
     * Decrement the counter by one.
     */
    public void dec() {
        dec(1);
    }

    /**
     * Decrement the counter by {@code n}.
     *
     * @param n
     *            the amount by which the counter will be decreased
     */
    public void dec(long n) {
        updateCount(-n);
    }

    private void updateCount(long n) {
        CounterImplState state = lockState();
        try {
            long now = clock.getTime();
            state.update(now, n);
        } finally {
            unlockState(state);
        }
    }

    private CounterImplState lockState() {
        /*
         * Mark the state as locked by replacing the referenced state with NULL
         */
        while (!stateRef.compareAndSet(state, null)) {
            ;
        }
        return state;
    }

    private void unlockState(CounterImplState updatedState) {
        /*
         * Mark the state as unlocked by replacing NULL with the updated state
         */
        if (!stateRef.compareAndSet(null, updatedState)) {
            throw new IllegalArgumentException(
                    "Invalid synchronization state: Seems like trying to unlock without locking before");
        }
    }

    @Override
    public AggregatedReservoirSnapshot getSnapshot() {
        CounterImplState state = lockState();
        try {
            AggregatedReservoirSnapshot reservoirSnapshot = state.reservoir.getCurrentSnapshot();
            reservoirSnapshot.setPath(Util.createPath(context.getPath()));
            reservoirSnapshot.setAttributes(getAttributes(true));
            return reservoirSnapshot;
        } finally {
            unlockState(state);
        }
    }

    @Override
    public List<AggregatedReservoirSnapshot> getSnapshots(long start, long end,
            long resolution) {
        CounterImplState state = lockState();
        try {
            List<AggregatedReservoirSnapshot> reservoirSnapshots = state.reservoir.getSnapshots(start, end, resolution);

            // augment snapshots with path & attributes
            if (!reservoirSnapshots.isEmpty()) {
                String path = Util.createPath(context.getPath());
                Map<String, Object> attrs = getAttributes(true);
                for (AggregatedReservoirSnapshot reservoirSnapshot : reservoirSnapshots) {
                    reservoirSnapshot.setPath(path);
                    reservoirSnapshot.setAttributes(attrs);
                }
            }

            return reservoirSnapshots;
        } finally {
            unlockState(state);
        }
    }

}
