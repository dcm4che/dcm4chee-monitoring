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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.dcm4chee.archive.monitoring.impl.core.CounterImpl;
import org.dcm4chee.archive.monitoring.impl.core.Timer;
import org.dcm4chee.archive.monitoring.impl.core.TimerImpl;
import org.dcm4chee.archive.monitoring.impl.core.clocks.Clock;
import org.dcm4chee.archive.monitoring.impl.core.context.MonitoringContextTree;
import org.dcm4chee.archive.monitoring.impl.core.reservoir.AggregatedReservoir;
import org.dcm4chee.archive.monitoring.impl.core.reservoir.RoundRobinReservoir;
import org.dcm4chee.archive.monitoring.impl.util.UnitOfTime;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;



/**
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 *
 */
public class MetricResponsesTest {
	private final ManualClock clock = new ManualClock(0, 50, UnitOfTime.MILLISECONDS);
	
	@Test
	@Ignore
	public void testMetricListXmlMarshalling() throws JAXBException {
	    TestReservoirBuilder reservoirBuilder = new TestReservoirBuilder(clock, 0, 60 * 1000, 60 *1000);
		JAXBContext jc = JAXBContext.newInstance(MetricResponses.class);
		 
        MetricResponses metricResponse = new MetricResponses();
        
        clock.tick();
        
        CounterImpl counter = new CounterImpl(new MonitoringContextTree(clock).getRoot().getOrCreateContext("test.counter1"), reservoirBuilder.build(), clock);
        counter.inc();
        CounterResponse counterResponse = CounterResponse.create(counter.getSnapshot());
        metricResponse.addCounter(counterResponse);
        
        clock.tick();
        
        counter = new CounterImpl(new MonitoringContextTree(clock).getRoot().getOrCreateContext("test.counter2"), reservoirBuilder.build(), clock);
        counter.inc(); counter.inc();
        counterResponse = CounterResponse.create(counter.getSnapshot());
        metricResponse.addCounter(counterResponse);
        
        clock.tick();
        
        TimerImpl timer = new TimerImpl(new MonitoringContextTree(clock).getRoot().getOrCreateContext("test.timer1"), reservoirBuilder.build(), clock);
        try( Timer.Split cxt = timer.time()) {
        	clock.tick();
        } 
        
        TimerResponse timerResponse = TimerResponse.create(timer.getSnapshot(), UnitOfTime.MILLISECONDS);
        metricResponse.addTimer(timerResponse);
 
        Marshaller marshaller = jc.createMarshaller();
 
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        marshaller.setProperty("jaxb.fragment", Boolean.TRUE);
//        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(metricResponse, out);
        
//        System.out.println(out.toString());
        
        Assert.assertEquals(
        	"<metrics size=\"3\">" +
            "<counters>" +
            "<counter>" +
                "<path>test.counter1</path>" +
                "<start>1970-01-01 01:00:00.000</start>" +
                "<end>1970-01-01 01:01:00.000</end>" +
                "<attributes/>" +
                "<firstUsageTimestamp>1970-01-01 01:00:00.050</firstUsageTimestamp>" +
                "<lastUsageTimestamp>1970-01-01 01:00:00.050</lastUsageTimestamp>" +
                "<minTimestamp>1970-01-01 01:00:00.050</minTimestamp>" +
                "<min>1</min>" +
                "<maxTimestamp>1970-01-01 01:00:00.050</maxTimestamp>" +
                "<max>1</max>" +
                "<count>1</count>" +
            "</counter>" +
            "<counter>" +
            	"<path>test.counter2</path>" +
            	"<start>1970-01-01 01:00:00.000</start>" +
                "<end>1970-01-01 01:01:00.000</end>" +
            	"<attributes/>" +
            	"<firstUsageTimestamp>1970-01-01 01:00:00.100</firstUsageTimestamp>" +
            	"<lastUsageTimestamp>1970-01-01 01:00:00.100</lastUsageTimestamp>" +
            	"<minTimestamp>1970-01-01 01:00:00.100</minTimestamp>" +
            	"<min>1</min>" +
            	"<maxTimestamp>1970-01-01 01:00:00.100</maxTimestamp>" +
            	"<max>2</max>" +
            	"<count>2</count>" +
            "</counter>" +
            "</counters>" +
            "<timers>" +
            "<timer timeUnit=\"MILLISECONDS\">" +
                "<path>test.timer1</path>" +
                "<start>1970-01-01 01:00:00.000</start>" +
                "<end>1970-01-01 01:01:00.000</end>" +
                "<attributes/>" +
                "<firstUsageTimestamp>1970-01-01 01:00:00.200</firstUsageTimestamp>" +
                "<lastUsageTimestamp>1970-01-01 01:00:00.200</lastUsageTimestamp>" +
                "<minTimestamp>1970-01-01 01:00:00.200</minTimestamp>" +
                "<min>50</min>" +
                "<maxTimestamp>1970-01-01 01:00:00.200</maxTimestamp>" +
                "<max>50</max>" +
                "<median>50</median>" +
                "<mean>50</mean>" +
                "<sum>50</sum>" +
                "<size>1</size>" +
            "</timer>" +
            "</timers>" +
            "<aggregates/>" +
            "</metrics>",
        out.toString());
	}
	
	@Test
	@Ignore
	public void testMetricListJsonMarshalling() throws IOException {
	    TestReservoirBuilder reservoirBuilder = new TestReservoirBuilder(clock, 0, 60 * 1000, 60 *1000);
	    
		ObjectMapper objectMapper = new ObjectMapper();
		 
        MetricResponses metricResponses = new MetricResponses();
        
        clock.tick();
        
        CounterImpl counter = new CounterImpl(new MonitoringContextTree(clock).getRoot(), reservoirBuilder.build(), clock);
        counter.inc();
        
        clock.tick();
        
        CounterResponse counterResponse = CounterResponse.create(counter.getSnapshot());
        metricResponses.addCounter(counterResponse);
        
        counter = new CounterImpl(new MonitoringContextTree(clock).getRoot(), reservoirBuilder.build(), clock);
        counter.inc(); counter.inc();
        
        clock.tick();
        
        counterResponse = CounterResponse.create(counter.getSnapshot());
        metricResponses.addCounter(counterResponse);
        
        TimerImpl timer = new TimerImpl(new MonitoringContextTree(clock).getRoot(), reservoirBuilder.build(), clock);
        try( Timer.Split cxt = timer.time()) {
            clock.tick();
        } 
        
        TimerResponse timerResponse = TimerResponse.create(timer.getSnapshot(), UnitOfTime.NANOSECONDS);
        metricResponses.addTimer(timerResponse);
 
        String json = objectMapper.setSerializationInclusion(Inclusion.NON_NULL).writeValueAsString(metricResponses);
        
//        String json = objectMapper.setSerializationInclusion(Inclusion.NON_NULL)
//                .writerWithDefaultPrettyPrinter().writeValueAsString(metricResponses);
//        System.out.println(json);
        
        Assert.assertEquals(
        		"{" +
        			  "\"size\":3," +
        			  "\"counters\":[{" +
        			    "\"path\":\"\"," +
        			    "\"start\":\"1970-01-01 01:00:00.000\"," +
        			    "\"end\":\"1970-01-01 01:01:00.000\"," +
        			    "\"attributes\":{}," +
        			    "\"firstUsageTimestamp\":\"1970-01-01 01:00:00.050\"," +
        			    "\"lastUsageTimestamp\":\"1970-01-01 01:00:00.050\"," +
        			    "\"minTimestamp\":\"1970-01-01 01:00:00.050\"," +
        			    "\"min\":\"1\"," +
        			    "\"maxTimestamp\":\"1970-01-01 01:00:00.050\"," +
        			    "\"max\":\"1\"," +
        			    "\"count\":\"1\"" +
        			  "},{" +
        			    "\"path\":\"\"," +
        			    "\"start\":\"1970-01-01 01:00:00.000\"," +
        			    "\"end\":\"1970-01-01 01:01:00.000\"," +
        			    "\"attributes\":{}," +
        			    "\"firstUsageTimestamp\":\"1970-01-01 01:00:00.100\"," +
        			    "\"lastUsageTimestamp\":\"1970-01-01 01:00:00.100\"," +
        			    "\"minTimestamp\":\"1970-01-01 01:00:00.100\"," +
        			    "\"min\":\"1\"," +
        			    "\"maxTimestamp\":\"1970-01-01 01:00:00.100\"," +
        			    "\"max\":\"2\"," +
        			    "\"count\":\"2\"" +
        			  "}]," +
        			  "\"timers\":[{" +
        			    "\"timeUnit\":\"NANOSECONDS\"," +
        			    "\"path\":\"\"," +
        			    "\"start\":\"1970-01-01 01:00:00.000\"," +
        			    "\"end\":\"1970-01-01 01:01:00.000\"," +
        			    "\"max\":\"50000000\"," +
        			    "\"min\":\"50000000\"," +
        			    "\"median\":\"50000000\"," +
        			    "\"mean\":\"50000000\"," +
        			    "\"sum\":\"50000000\"," +
        			    "\"maxTimestamp\":\"1970-01-01 01:00:00.200\"," +
        			    "\"minTimestamp\":\"1970-01-01 01:00:00.200\"," +
        			    "\"firstUsageTimestamp\":\"1970-01-01 01:00:00.200\"," +
        			    "\"lastUsageTimestamp\":\"1970-01-01 01:00:00.200\"," +
        			    "\"attributes\":{}," +
        			    "\"size\":1" +
        			  "}]," +
        			  "\"aggregates\":[]" +
        			"}",
        json);
	}
	
	private static class TestReservoirBuilder {
        private final Clock clock;
        private long start;
        private long step;
        private long resolution;
        
        private TestReservoirBuilder(Clock clock, long start, long step, long resolution) {
            this.clock = clock;
            this.start = start;
            this.resolution = resolution;
            this.step = step;
        }
        
        public AggregatedReservoir build() {
            return new RoundRobinReservoir.Builder()
            .clock(clock).start(start).step(step)
            .addArchive(resolution, 1, 10000).build();
        }
        
    }
}
