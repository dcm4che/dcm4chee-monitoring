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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;


/**
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 *
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "metrics")
@XmlType(propOrder={"counterResponses", "timerResponses", "aggregateResponses"})

@JsonPropertyOrder({"size", "counterResponses", "timerResponses", "aggregateResponses"})
public class MetricResponses {
	@JsonProperty("timers")
	private final List<TimerResponse> timerResponses = new ArrayList<>();
	@JsonProperty("counters")
	private final List<CounterResponse> counterResponses = new ArrayList<>();
	@JsonProperty("aggregates")
	private final List<AggregateTimerResponse> aggregateResponses = new ArrayList<>();
	 
	public void addCounter(CounterResponse counter) {
		counterResponses.add(counter);
	}
	
	public void addTimer(TimerResponse timer) {
		timerResponses.add(timer);
	}
	
	public void addAggregate(AggregateTimerResponse aggregate) {
		aggregateResponses.add(aggregate);
	}

	@XmlAttribute
	public int getSize() {
		return timerResponses.size() + counterResponses.size() + aggregateResponses.size();
	}

	@XmlElementWrapper(name = "counters")
	@XmlElement(name = "counter")
	public List<CounterResponse> getCounterResponses() {
		return counterResponses;
	}

	@XmlElementWrapper(name = "timers")
	@XmlElement(name = "timer")
	public List<TimerResponse> getTimerResponses() {
		return timerResponses;
	}
	
	@XmlElementWrapper(name = "aggregates")
	@XmlElement(name = "aggregate")
	public List<AggregateTimerResponse> getAggregateResponses() {
		return aggregateResponses;
	}
	
}
