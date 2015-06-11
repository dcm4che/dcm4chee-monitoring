package org.dcm4chee.archive.monitoring.impl.jdbc;

import java.io.Serializable;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.dcm4chee.archive.monitoring.impl.core.MonitoredObject;


class ConnectionInformation implements Serializable, MonitoredObject {
	private static final long serialVersionUID = -89432879297834L;
	private static final String OWN_PACKAGE = ConnectionInformation.class.getName().substring(0,
			ConnectionInformation.class.getName().lastIndexOf('.'));
	private static final boolean CONNECTIONS_STACK_TRACES_DISABLED = Boolean
			.parseBoolean(Parameters.getParameter(Parameter.CONNECTIONS_STACK_TRACES_DISABLED));
	private final long openingTime;
	private final StackTraceElement[] openingStackTrace;
	private final long threadId;

	ConnectionInformation() {
		this.openingTime = System.currentTimeMillis();
		final Thread currentThread = Thread.currentThread();
		if (CONNECTIONS_STACK_TRACES_DISABLED) {
			this.openingStackTrace = null;
		} else {
			this.openingStackTrace = currentThread.getStackTrace();
		}
		this.threadId = currentThread.getId();
	}

	static int getUniqueIdOfConnection(Connection connection) {
		return System.identityHashCode(connection);
	}

	Date getOpeningDate() {
		return new Date(openingTime);
	}

	List<StackTraceElement> getOpeningStackTrace() {
		if (openingStackTrace == null) {
			return Collections.emptyList();
		}
		final List<StackTraceElement> stackTrace = new ArrayList<StackTraceElement>(
				Arrays.asList(openingStackTrace));
		
		stackTrace.remove(0);
		while (stackTrace.get(0).getClassName().startsWith(OWN_PACKAGE)) {
			stackTrace.remove(0);
		}
		return stackTrace;
	}

	long getThreadId() {
		return threadId;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[openingDate=" + getOpeningDate() + ", threadId=" + getThreadId() + ']';
	}
}
