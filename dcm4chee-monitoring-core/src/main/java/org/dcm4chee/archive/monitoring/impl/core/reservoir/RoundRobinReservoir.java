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

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.dcm4chee.archive.monitoring.impl.core.clocks.Clock;
import org.dcm4chee.archive.monitoring.impl.core.context.MonitoringContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 *
 */
public class RoundRobinReservoir implements AggregatedReservoir {
	private static final Logger LOGGER = LoggerFactory.getLogger(RoundRobinReservoir.class);
	
	private final static int VALUE_ARCHIVE_MAX_SIZE_DEFAULT = 10000;
	
	// archive containers sorted by increasing resolution
	private final ArchiveContainer[] containers;
	private final long step;
	private final Clock clock;
	
	private long lastNow = Long.MIN_VALUE;

	/**
     * 
     * @param clock
     * @param start Start time of the reservoir. Must be given in milliseconds since midnight, January 1, 1970 UTC
     * @param step Resolution step size of the reservoir. Must be given in milliseconds
     * @param resolutions Array containing the individual resolution of the archives. Must be given in milliseconds
     * @param retentions Array containing the number of individual retentions for archives 
     */
	public static class Builder {
	    private Clock clock;
	    private long start;
	    private long step;
	    private final List<ArchiveSpec> archiveSpecs = new ArrayList<>();
	    
	    public Builder clock(Clock clock) {
	        this.clock = clock;
	        return this;
	    }
	    
	    public Builder step(long step) {
	        this.step = step;
	        return this;
	    }
	    
	    public Builder start(long start) {
            this.start = start;
            return this;
        }
	    
	    public Builder addArchive(long resolution, int retentions, boolean valueArchive) {
	        archiveSpecs.add(new ArchiveSpec(resolution, retentions, valueArchive));
	        return this;
	    }
	    
	    public RoundRobinReservoir build() {
	        return new RoundRobinReservoir(this);
	    }
	}
	
	private static class ArchiveSpec {
	    private final long resolution;
	    private final int retentions;
	    private final boolean valueArchive;
	    
        public ArchiveSpec(long resolution, int retentions, boolean valueArchive) {
            this.resolution = resolution;
            this.retentions = retentions;
            this.valueArchive = valueArchive;
        }
        
        public long getResolution() {
            return resolution;
        }
        
        public int getRetentions() {
            return retentions;
        }
        
        public boolean isValueArchive() {
            return valueArchive;
        }
	    
	}
	
	private RoundRobinReservoir(Builder builder) {
        this.clock = builder.clock;
        this.step = builder.step;
        containers = new ArchiveContainer[builder.archiveSpecs.size()];
        for (int i = 0; i < builder.archiveSpecs.size(); i++) {
            ArchiveSpec archiveSpec = builder.archiveSpecs.get(i);
            long resolution = archiveSpec.getResolution();
            
            if (!checkResolution(resolution)) {
                throw new IllegalArgumentException(format("Archive resolution(s) must be a multiple(s) of step size (step size: %d)", step));
            }
            
            if (i == 0 && resolution != step) {
                throw new IllegalArgumentException(format("Smallest archive resolution must be equal to step size (step size: %d)", step));
            } else if (i > 0 && resolution <= builder.archiveSpecs.get(i - 1).getResolution()) {
                throw new IllegalArgumentException("Archive resolution(s) must be linearly increasing");
            }
            
            long end = builder.start + resolution;
            Archive firstArchive = archiveSpec.isValueArchive() ? new ValueArchive(builder.start, end, resolution, VALUE_ARCHIVE_MAX_SIZE_DEFAULT) : 
                new Archive(builder.start, end, resolution);
            ArchiveContainer container = new ArchiveContainer(resolution, archiveSpec.getRetentions(), firstArchive);
            containers[i] = container;
        }
    }
	
	@Override
	public void update(MonitoringContext context, long now, long value) {
	    if(!saneTime(now)) {
	        LOGGER.warn("Clock was turned back -> Metric value will be skipped by reservoir");
	        return;
	    }
	    
		for(int i = 0; i < containers.length; i++) {
			ArchiveContainer container = containers[i];
//			Archive currentArchive = container.updateCurrentArchiveOld(now);
			Archive currentArchive = container.updateCurrentArchive(now);
			currentArchive.update(now, value);
		}
	}
	
	private boolean checkResolution(long resolution) {
		return resolution % step == 0;
	}
	
	@Override
	public AggregatedReservoirSnapshot getCurrentSnapshot() {
	    ArchiveContainer smallestResolutionContainer = containers[0];
	    
	    long now = clock.getTime();
	    if(saneTime(now)) {
//	    smallestResolutionContainer.updateCurrentArchiveOld(now);
	        smallestResolutionContainer.updateCurrentArchive(now);
	    }
	    
	    return smallestResolutionContainer.getCurrentArchiveSnapshot();
    }
	
	@Override
	public List<AggregatedReservoirSnapshot> getSnapshots(long start, long end, long resolution) {
		return getSnapshots(start, end, resolution, true);
	}
	
	private List<AggregatedReservoirSnapshot> getSnapshots(long start, long end, long resolution, boolean exactResolution) {
		if(end <= start) {
			throw new IllegalArgumentException("Query end date must not be equal or before start date");
		}
		
		if (!checkResolution(resolution)) {
			throw new IllegalArgumentException(String.format("Query resolution (%d) must be a multiple of step size (step size is: %d)", resolution, step) );
		}
		
		int startContainerIdx = -1;
		for(int i = 0; i < containers.length; i++) {
		    ArchiveContainer resolutionContainer = containers[i];
		    boolean found = false;
		    if(exactResolution) {
		        found = resolutionContainer.resolution == resolution;
		    } else {
		        found = resolutionContainer.resolution >= resolution;
		    }
		    
		    if(found) {
		        startContainerIdx = i;
		        break;
		    }
		}
		
		if(startContainerIdx == -1)
		{
			LOGGER.warn("Unsupported time resolution: {}", resolution);
			return Collections.emptyList();
		}
		
		int stopResolution = exactResolution ? startContainerIdx + 1 : containers.length;
		
		for( int i = startContainerIdx; i < stopResolution; i++) {
			ArchiveContainer resolutionContainer = containers[i];
			
			long now = clock.getTime();
			if(saneTime(now)) {
			    resolutionContainer.updateCurrentArchive(now);
			}
			
			List<AggregatedReservoirSnapshot> snapshots = resolutionContainer.getSpanningArchives(start, end);
			if ( snapshots != Collections.<AggregatedReservoirSnapshot>emptyList() ) {
				return snapshots;
			}
		}
		
		return Collections.emptyList();
	}
	
	private boolean saneTime(long now) {
        boolean sane = now >= lastNow;
        if (sane) {
            lastNow = now;
        }
        return sane;
    }
	
	public ArchiveContainer[] getContainers() {
	    return containers;
	}
	
	protected static class Archive {
		protected final long start;
		protected final long end;
		protected final long resolution;
		
		private int size;
		private double m2;
		private double mean = Double.NaN;
		private long sum = Long.MIN_VALUE;
		
		private long max = Long.MIN_VALUE;
		private long min = Long.MAX_VALUE;
		
	  	private long lastUsage = Long.MIN_VALUE;
	  	private long firstUsage = Long.MIN_VALUE;
	  	private long maxTimestamp = Long.MIN_VALUE;
	  	private long minTimestamp = Long.MIN_VALUE;
	  	
	  	private long lastValue = Long.MIN_VALUE;
		
		protected Archive(long start, long end, long resolution) {
			this.start = start;
			this.end = end;
			this.resolution = resolution;
		}
		
		protected void update(long now, long value) {
			updateUsages(now);
			
			lastValue = value;
			
            if (sum == Long.MIN_VALUE) {
                sum = value;
            } else {
                sum += value;
            }
			
			size++;
			
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
		}
		
		private void updateUsages(long now) {
			lastUsage = now;
			if (firstUsage == Long.MIN_VALUE) {
				firstUsage = lastUsage;
			}
		}
		
		private void updateMinMax(long value) {
			if (value < min) {
				min = value;
				minTimestamp = lastUsage;
			}
			if (value > max) {
				max = value;
				maxTimestamp = lastUsage;
			}
		}
		
		private double getVariance() {
			if(size < 2) {
				return Double.NaN;
			}
			
			return m2 / (size - 1);
		}
		
		public double getStdDev() {
			return Math.sqrt(getVariance());
		}
		
		public double getMean() {
			if (size == 0) {
				return Double.NaN;
			}

			return mean;
		}
		
		public long getLastValue() {
		    return lastValue;
		}
		
		public long getSum() {
            return sum;
        }
		
		public long[] getValues(boolean copy) {
		    return null;
		}
		
		public long getMax() {
			return max;
		}
		
		public long getMaxTimestamp() {
			return maxTimestamp;
		}
		
		public long getMin() {
			return min;
		}
		
		public long getMinTimestamp() {
			return minTimestamp;
		}
		
		public long getFirstUsageTimestamp() {
			return firstUsage;
		}
		
		public long getLastUsageTimestamp() {
			return lastUsage;
		}
		
		public long size() {
			return size;
		}
		
		private long getStart() {
			return start;
		}
		
		private long getEnd() {
			return end;
		}
		
		protected boolean withinTimeInterval(long timestamp) {
			return start <= timestamp && end >= timestamp;
		}
		
		protected Archive next() {
			long nextStart = end + 1;
			return new Archive(nextStart, nextStart + resolution - 1, resolution);
		}
		
		protected Archive createArchive(long start) {
		    return new Archive(start, start + resolution - 1, resolution);
		}
		
		@Override
		public String toString() {
		    Date startDate = new Date(start);
		    Date endDate = new Date(end);
			return String.format("Archive([%s (%d) - %s (%d)], resolution: %d, count: %d, min: %d, max: %d, mean: %.2f, stdDev: %.2f", startDate, start, endDate, end, resolution, size, min, max, getMean(), getStdDev());
		}
	}
	
	protected static class ValueArchive extends Archive {
	    private static final int INITIAL_CAPACITY = 10; 
	    private final long maxSize;
	    private List<Long> values = new ArrayList<>(INITIAL_CAPACITY);
	    
	    protected ValueArchive(long start, long end, long resolution, long maxSize) {
	        super(start, end, resolution);
	        this.maxSize = maxSize;
	    }
	    
	    @Override
	    protected void update(long now, long value) {
	        super.update(now, value);
	        if( size() > maxSize ) {
	            LOGGER.error("Archive exceeds allowed maximum of values: {}", maxSize);
	            values = null;
	        }
	        
	        if(values != null) {
	            values.add(value);
	        }
	    }
	    
	    @Override
	    public long[] getValues(boolean copy) {
	        return (values != null) ? getValuesCopy() : null;
	    }
	    
	    private long[] getValuesCopy() {
	        long[] copy = new long[values.size()];
	        for(int i = 0; i < values.size(); i++) {
	            copy[i] = values.get(i);
	        }
	        return copy;
	    }
	    
	    @Override
	    protected Archive next() {
            long nextStart = end + 1;
            return new ValueArchive(nextStart, nextStart + resolution - 1, resolution, maxSize);
        }
	    
	    @Override
	    protected Archive createArchive(long start) {
            return new ValueArchive(start, start + resolution - 1, resolution, maxSize);
        }
	}
	
	 protected static class ArchiveContainer {
		// start time-stamp of the oldest archive contained
		private long start;
		// end time-stamp of the youngest archive contained
		private long end;
		// number of archives contained
		private final int size;
		// resolution
		private final long resolution;
		
		/*
		 *  ring-buffer holding the archives:
		 *  1) The archives contained are sorted by increasing time
		 *  2) Due to ring-buffer nature the oldest archives are overwritten automatically
		 */
		private final Archive[] archives;
		// index of the currently active archive in the ring-buffer
		private int currentIdx = 0;
		
		private ArchiveContainer(long resolution, int size, Archive firstArchive) {
		    this.resolution = resolution;
			this.size = size;
			archives = new Archive[size];
			archives[currentIdx] = firstArchive;
			this.start = firstArchive.getStart();
			this.end = firstArchive.getEnd(); 
		}
		
		private Archive updateCurrentArchive(long now) {
            Archive currentArchive = archives[currentIdx];
            long currentStart = currentArchive.start;
            
            if( currentStart == 0) {
                currentStart = 1;
            }
            
            long loffset = (now - currentStart) / resolution;
            if(loffset > Integer.MAX_VALUE) {
                LOGGER.warn("Integer overflow while calculating reservoir archive offset");
            }
            
            int offset = (int)loffset;
            if(offset == 0) {
                return currentArchive;
            }
            
            long startNew = currentStart + (offset * resolution);
            int currentIdxNew = (currentIdx + offset) % size;
            Archive newCurrentArchive = currentArchive.createArchive(startNew);
            
//            Archive oldArchive = archives[currentIdxNew];
//            if(oldArchive != null) {
//                LOGGER.debug("Overriding round-robin archive {} with new archive {}", oldArchive, newCurrentArchive);
//            }
            
            //SET
            archives[currentIdxNew] = newCurrentArchive;
            this.end = newCurrentArchive.getEnd();
            
            // if this is a roundtrip (all positions in ringbuffer are overwritten)
            // -> constrain offset to size
            if (offset >= size) {
                offset = size;
            }
            
            fillArchivesBetweenOldAndNewCurrent(newCurrentArchive, currentIdxNew, startNew, offset);
            
            Archive oneAfterNewCurrent = archives[(currentIdxNew + 1) % size];
            if(oneAfterNewCurrent != null ) {
                //SET
                this.start = oneAfterNewCurrent.getStart();
            }
            
            currentIdx = currentIdxNew;
            return newCurrentArchive;
        }
		
		private void fillArchivesBetweenOldAndNewCurrent(Archive archiveFactory, int currentIdxNew, long startNew, int offset) {
		    long emptyEnd = startNew - 1;
            for(int i = 1; i < offset; i++) {
                Archive emptyInBetweenArchive = archiveFactory.createArchive(emptyEnd - resolution + 1);
                
                int emptyInBetweenIdx = mod( currentIdxNew - i, size);
                
                Archive oldArchive = archives[emptyInBetweenIdx];
                if(oldArchive != null) {
                    LOGGER.debug("Overriding round-robin archive {} with new archive {}", oldArchive, emptyInBetweenArchive);
                }
                
                //SET
                archives[emptyInBetweenIdx] = emptyInBetweenArchive;
                
                emptyEnd -= resolution; 
            }
		}
		
        private static int mod(int a, int n) {
            return a < 0 ? (a % n + n) % n : a % n;
        }
		
		/*
		 * Find archives that when considered appended are spanning the given time range
		 */
		private List<AggregatedReservoirSnapshot> getSpanningArchives(long start, long end) {
			if ((this.start <= start) && (this.end >= end)) {
				int oldestArchiveIdx = getOldestArchiveIndex();

				List<AggregatedReservoirSnapshot> spanningArchives = null;
				
				// iterate over ring-buffer starting from the oldest archive
				for (int i = oldestArchiveIdx;; i = (i + 1) % size) {
					Archive archive = archives[i];

					if (spanningArchives == null && archive.withinTimeInterval(start)) {
						spanningArchives = new ArrayList<>();
					}

					if (spanningArchives != null) {
						spanningArchives.add(copyToSnapshot(archive));
					}
					
					if (archive.getEnd() >= end) {
						break;
					}
				}

				return (spanningArchives == null) ? Collections.<AggregatedReservoirSnapshot>emptyList(): spanningArchives;
			} else {
				return Collections.emptyList();
			}
		}
		
		private AggregatedReservoirSnapshot getCurrentArchiveSnapshot() {
		    Archive currentArchive = archives[currentIdx];
		    return copyToSnapshot(currentArchive);
		}
		
		private static AggregatedReservoirSnapshotImpl copyToSnapshot(Archive archive) {
			AggregatedReservoirSnapshotImpl snapshot = new AggregatedReservoirSnapshotImpl();
			snapshot.setStart(archive.getStart());
			snapshot.setEnd(archive.getEnd());
			snapshot.setSize(archive.size());
			snapshot.setValues(archive.getValues(true));
			snapshot.setLastValue(archive.getLastValue());
			snapshot.setSum(archive.getSum());
			snapshot.setMean(archive.getMean());
			snapshot.setStdDev(archive.getStdDev());
			snapshot.setMin(archive.getMin());
			snapshot.setMinTimestamp(archive.getMinTimestamp());
			snapshot.setMax(archive.getMax());
			snapshot.setMaxTimestamp(archive.getMaxTimestamp());
			snapshot.setFirstUsageTimestamp(archive.getFirstUsageTimestamp());
			snapshot.setLastUsageTimestamp(archive.getLastUsageTimestamp());
			return snapshot;
		}
		
		/*
		 * Get index of the oldest archive in the ring-buffer
		 */
		private int getOldestArchiveIndex() {
			int candidateIdx = (currentIdx + 1) % size;
			/*
			 * () If the next archive after the current one exists then the ring-buffer has been wrapped (at least once)
			 * so the next archive must be the oldest one
			 * () If the next archive after the current one does not exist then the ring-buffer is not completly filled
			 * (no wrapping has happended) so the first archive must still be the oldest one 
			 */
			return (archives[candidateIdx] != null) ? candidateIdx : 0;
		}
		
		public Archive[] getArchives() {
		    return archives;
		}
		
		public int getCurrentIndex() {
		    return currentIdx;
		}
		
		@Override
        public String toString() {
            Date startDate = new Date(start);
            Date endDate = new Date(end);
            return String.format("ArchiveContainer([%s (%d) - %s (%d)], size: %d", startDate, start, endDate, end, size);
        }
		
	}

}
