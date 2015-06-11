package org.dcm4chee.archive.monitoring.impl.jdbc;

import java.util.Locale;

public enum Parameter {
	
	/**
	 * true | false, true will disable opening stack-traces of jdbc connections (default: false).
	 */
	CONNECTIONS_STACK_TRACES_DISABLED("connections-stack-traces-disabled"),
	
	DISABLED("disabled"),

	DATASOURCES("datasources"),
	
	SYSTEM_ACTIONS_ENABLED("system-actions-enabled");

	private final String code;

	private Parameter(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}

	static Parameter valueOfIgnoreCase(String parameter) {
		return valueOf(parameter.toUpperCase(Locale.ENGLISH).trim());
	}
}
