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

import java.util.Iterator;
import java.util.List;

import org.dcm4chee.archive.monitoring.impl.core.ManualClock2;
import org.dcm4chee.archive.monitoring.impl.core.reservoir.RoundRobinReservoir.ArchiveContainer;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 *
 */
public class RoundRobinReservoirTest {

	@Test
	public void testReservoirWithPrimaryResolutionQuery() {
		ManualClock2 clock = new ManualClock2.Builder().tick(0, 1).tock(0, 1).build();
		RoundRobinReservoir reservoir = new RoundRobinReservoir.Builder()
		        .clock(clock).start(0).step(5)
		        .addArchive(5, 5, false)
		        .addArchive(10, 5, false).build();
		
		reservoir.update(null, clock.tock(), 1);
		reservoir.update(null, clock.tock(), 2);
		reservoir.update(null, clock.tock(), 3);
		reservoir.update(null, clock.tock(), 4);
		reservoir.update(null, clock.tock(), 5);
		reservoir.update(null, clock.tock(), 6);
		reservoir.update(null, clock.tock(), 7);
		reservoir.update(null, clock.tock(), 8);
		reservoir.update(null, clock.tock(), 9);
		reservoir.update(null, clock.tock(), 10);
		reservoir.update(null, clock.tock(), 11);
		reservoir.update(null, clock.tock(), 12);
		reservoir.update(null, clock.tock(), 13);
		
		long queryResolution = 5;
		List<AggregatedReservoirSnapshot> snapshots = reservoir.getSnapshots(0, 4, queryResolution);
		Assert.assertEquals(1, snapshots.size());
		AggregatedReservoirSnapshot snapshot = snapshots.iterator().next();
		Assert.assertEquals(1, snapshot.getMin());
		Assert.assertEquals(5, snapshot.getMax());
	}
	
	@Test
	public void testReservoirWithNotPrimaryResolutionQuery() {
	    ManualClock2 clock = new ManualClock2.Builder().tick(0, 1).tock(0, 1).build();
		RoundRobinReservoir reservoir = new RoundRobinReservoir.Builder()
		        .clock(clock).start(0).step(5)
                .addArchive(5, 5, false)
                .addArchive(10, 5, false).build();
		
		reservoir.update(null, clock.tock(), 1);
		reservoir.update(null, clock.tock(), 2);
		reservoir.update(null, clock.tock(), 3);
		reservoir.update(null, clock.tock(), 4);
		reservoir.update(null, clock.tock(), 5);
		reservoir.update(null, clock.tock(), 6);
		reservoir.update(null, clock.tock(), 7);
		reservoir.update(null, clock.tock(), 8);
		reservoir.update(null, clock.tock(), 9);
		reservoir.update(null, clock.tock(), 10);
		reservoir.update(null, clock.tock(), 11);
		reservoir.update(null, clock.tock(), 12);
		reservoir.update(null, clock.tock(), 13);
		
		long queryResolution = 10;
		List<AggregatedReservoirSnapshot> snapshots = reservoir.getSnapshots(0, 4, queryResolution);
		Assert.assertEquals(1, snapshots.size());
		AggregatedReservoirSnapshot snapshot = snapshots.iterator().next();
		Assert.assertEquals(1, snapshot.getMin());
		Assert.assertEquals(10, snapshot.getMax());
	}
	
	@Test
	public void testReservoirWithPrimaryResolutionQuerySpanningMultipleArchives() {
		ManualClock2 clock = new ManualClock2.Builder().tick(0, 1).tock(0, 1).build();
		RoundRobinReservoir reservoir = new RoundRobinReservoir.Builder()
		        .clock(clock).start(0).step(5)
                .addArchive(5, 5, false)
                .addArchive(10, 5, false).build();
		
		reservoir.update(null, clock.tock(), 1);
		reservoir.update(null, clock.tock(), 2);
		reservoir.update(null, clock.tock(), 3);
		reservoir.update(null, clock.tock(), 4);
		reservoir.update(null, clock.tock(), 5);
		reservoir.update(null, clock.tock(), 6);
		reservoir.update(null, clock.tock(), 7);
		reservoir.update(null, clock.tock(), 8);
		reservoir.update(null, clock.tock(), 9);
		reservoir.update(null, clock.tock(), 10);
		reservoir.update(null, clock.tock(), 11);
		reservoir.update(null, clock.tock(), 12);
		reservoir.update(null, clock.tock(), 13);
		
		long queryResolution = 5;
		List<AggregatedReservoirSnapshot> snapshots = reservoir.getSnapshots(1, 12, queryResolution);
		Assert.assertEquals(3, snapshots.size());
		
		Iterator<AggregatedReservoirSnapshot> it = snapshots.iterator();
		
		AggregatedReservoirSnapshot resolution5Reservoir1 = it.next();
		Assert.assertEquals(1, resolution5Reservoir1 .getMin());
		Assert.assertEquals(5, resolution5Reservoir1 .getMax());
		
		AggregatedReservoirSnapshot resolution5Reservoir2 = it.next();
		Assert.assertEquals(6, resolution5Reservoir2 .getMin());
		Assert.assertEquals(10, resolution5Reservoir2 .getMax());
		
		AggregatedReservoirSnapshot resolution5Reservoir3 = it.next();
		Assert.assertEquals(11, resolution5Reservoir3 .getMin());
		Assert.assertEquals(13, resolution5Reservoir3 .getMax());
	}
	
	@Test
	public void testReservoirWithNotPrimaryResolutionQuerySpanningMultipleArchives() {
	    ManualClock2 clock = new ManualClock2.Builder().tick(0, 1).tock(0, 1).build();
		RoundRobinReservoir reservoir = new RoundRobinReservoir.Builder()
		        .clock(clock).start(0).step(5)
                .addArchive(5, 5, false)
                .addArchive(10, 5, false).build();
		
		reservoir.update(null, clock.tock(), 1);
		reservoir.update(null, clock.tock(), 2);
		reservoir.update(null, clock.tock(), 3);
		reservoir.update(null, clock.tock(), 4);
		reservoir.update(null, clock.tock(), 5);
		reservoir.update(null, clock.tock(), 6);
		reservoir.update(null, clock.tock(), 7);
		reservoir.update(null, clock.tock(), 8);
		reservoir.update(null, clock.tock(), 9);
		reservoir.update(null, clock.tock(), 10);
		reservoir.update(null, clock.tock(), 11);
		reservoir.update(null, clock.tock(), 12);
		reservoir.update(null, clock.tock(), 13);
		reservoir.update(null, clock.tock(), 14);
		
		long queryResolution = 10;
		List<AggregatedReservoirSnapshot> snapshots = reservoir.getSnapshots(1, 12, queryResolution);
		Assert.assertEquals(2, snapshots.size());
		
		Iterator<AggregatedReservoirSnapshot> it = snapshots.iterator();
		
		AggregatedReservoirSnapshot resolution5Reservoir1 = it.next();
		Assert.assertEquals(1, resolution5Reservoir1 .getMin());
		Assert.assertEquals(10, resolution5Reservoir1 .getMax());
		
		AggregatedReservoirSnapshot resolution5Reservoir2 = it.next();
		Assert.assertEquals(11, resolution5Reservoir2 .getMin());
		Assert.assertEquals(14, resolution5Reservoir2 .getMax());
	}
	
	@Test
	public void testReservoirWithSpareUpdates() {
		ManualClock2 clock = new ManualClock2.Builder().tick(0, 1).tock(0, 1).build();
		
		RoundRobinReservoir reservoir = new RoundRobinReservoir.Builder()
		        .clock(clock).start(0).step(5)
                .addArchive(5, 5, false)
                .addArchive(10, 5, false).build();
		
		reservoir.update(null, clock.tock(), 1); // time: 1
		reservoir.update(null, clock.tock(), 2); // time: 2
		reservoir.update(null, clock.tock(9), 3); // time: 11
		reservoir.update(null, clock.tock(), 4); // time: 12
		reservoir.update(null, clock.tock(8), 5); // time: 20
		reservoir.update(null, clock.tock(12), 6); // time: 32
		
		long primQueryResolution = 5;
		long secQueryResolution = 10;
		List<AggregatedReservoirSnapshot> snapshots = reservoir.getSnapshots(1, 32, primQueryResolution);
		Assert.assertEquals(0, snapshots.size());
		
		snapshots = reservoir.getSnapshots(11, 32, primQueryResolution);
		Assert.assertEquals(5, snapshots.size());
		
		snapshots = reservoir.getSnapshots(1, 32, secQueryResolution);
		Assert.assertEquals(4, snapshots.size());
	}
	
	@Test
    public void testReservoirWithSpareUpdates2() {
	    ManualClock2 clock = new ManualClock2.Builder().tick(0, 1).tock(0, 1).build();
        RoundRobinReservoir reservoir = new RoundRobinReservoir.Builder()
                .clock(clock).start(0).step(5)
                .addArchive(5, 4, false).build();
        
        reservoir.update(null, clock.tock(), 1); //time: 1
        reservoir.update(null, clock.tock(), 2); //time: 2
        
        ArchiveContainer container = reservoir.getContainers()[0];
        Assert.assertEquals(0, container.getCurrentIndex());
        
        reservoir.update(null, clock.tock(9), 3); //time: 11
        Assert.assertEquals(2, container.getCurrentIndex());
        
        reservoir.update(null, clock.tock(), 4); //time:12
        
        reservoir.update(null, clock.tock(22) , 5); //time:34
        reservoir.update(null, clock.tock(8) , 6); //time:42
        
        reservoir.update(null, clock.tock(130) , 6); //time:172
        
        reservoir.update(null, clock.tock(99920000) , 6); //time:172
    }
	
	@Test
    public void testReservoirWithSingleRetention() {
        ManualClock2 clock = new ManualClock2.Builder().tick(0, 1).tock(0, 1).build();
        RoundRobinReservoir reservoir = new RoundRobinReservoir.Builder()
                .clock(clock).start(0).step(5)
                .addArchive(5, 1, false).build();
        
        reservoir.update(null, clock.tock(), 1); //time: 1
        reservoir.update(null, clock.tock(), 2); //time: 2
        
        ArchiveContainer container = reservoir.getContainers()[0];
        Assert.assertEquals(0, container.getCurrentIndex());
        
        reservoir.update(null, clock.tock(9), 3); //time: 11
        
        reservoir.update(null, clock.tock(), 4); //time:12
        
        reservoir.update(null, clock.tock(22) , 5); //time:34
        reservoir.update(null, clock.tock(8) , 6); //time:42
        
        reservoir.update(null, clock.tock(130) , 6); //time:172
        
        reservoir.update(null, clock.tock(99920000) , 6); //time:172
    }
	
	@Test
    public void testReservoirWithTimeRunningBackwards() {
        ManualClock2 clock = new ManualClock2.Builder().tick(0, 1).tock(0, 1).build();
        RoundRobinReservoir reservoir = new RoundRobinReservoir.Builder()
                .clock(clock).start(0).step(5)
                .addArchive(5, 4, false).build();
        
        reservoir.update(null, clock.tock(), 1); //time: 1
        reservoir.update(null, clock.tock(), 2); //time: 2
        
        reservoir.update(null, clock.tock(9), 3); //time: 11
        
        reservoir.update(null, clock.tock(-5), 4); //time:12
        
       
    }
	
	@Test
    public void testReservoirWithValueArchives() {
	    ManualClock2 clock = new ManualClock2.Builder().tick(0, 1).tock(0, 1).build();
        RoundRobinReservoir reservoir = new RoundRobinReservoir.Builder()
                .clock(clock).start(0).step(5)
                .addArchive(5, 5, true)
                .addArchive(10, 5, false).build();
        
        reservoir.update(null, clock.tock(), 1);
        reservoir.update(null, clock.tock(), 2);
        reservoir.update(null, clock.tock(), 3);
        reservoir.update(null, clock.tock(), 4);
        reservoir.update(null, clock.tock(), 5);
        reservoir.update(null, clock.tock(), 6);
        reservoir.update(null, clock.tock(), 7);
        reservoir.update(null, clock.tock(), 8);
        reservoir.update(null, clock.tock(), 9);
        reservoir.update(null, clock.tock(), 10);
        reservoir.update(null, clock.tock(), 11);
        reservoir.update(null, clock.tock(), 12);
        reservoir.update(null, clock.tock(), 13);
        reservoir.update(null, clock.tock(), 14);
        
        long queryResolution = 5;
        List<AggregatedReservoirSnapshot> snapshots = reservoir.getSnapshots(1, 4, queryResolution);
        Assert.assertEquals(1, snapshots.size());
        
        Iterator<AggregatedReservoirSnapshot> it = snapshots.iterator();
        
        AggregatedReservoirSnapshot resolution5Reservoir1 = it.next();
        Assert.assertEquals(1, resolution5Reservoir1 .getMin());
        Assert.assertEquals(5, resolution5Reservoir1 .getMax());
        
        Assert.assertArrayEquals(new long[] {1, 2, 3, 4, 5}, resolution5Reservoir1.getValues(false));
        Assert.assertEquals(3.0,  resolution5Reservoir1.getMedian(), 0.0);
        Assert.assertEquals(3.0,  resolution5Reservoir1.getValue(0.5), 0.0);
        Assert.assertEquals(4.1,  resolution5Reservoir1.getValue(0.7), 0.1);
        Assert.assertEquals(5.0,  resolution5Reservoir1.getValue(1.0), 0.0);
        Assert.assertEquals(1.0,  resolution5Reservoir1.getValue(0.0), 0.0);
    }
	
}
