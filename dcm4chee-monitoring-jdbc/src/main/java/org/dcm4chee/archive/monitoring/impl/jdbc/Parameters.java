
package org.dcm4chee.archive.monitoring.impl.jdbc;

import javax.servlet.ServletContext;

import org.dcm4chee.archive.monitoring.impl.config.ModuleConfiguration;

final class Parameters {
	private static final String PARAMETER_SYSTEM_PREFIX = "csp-monitoring.";
	
	private static ServletContext servletContext;
	private static ModuleConfiguration moduleCfg;


	private Parameters() {
		super();
	}

	static void initialize(ServletContext context) {
		servletContext = context;
	}
	
	static void initialize(ModuleConfiguration cfg) {
		moduleCfg = cfg;
	}

	static boolean isSystemActionsEnabled() {
		final String parameter = Parameters.getParameter(Parameter.SYSTEM_ACTIONS_ENABLED);
		return parameter == null || Boolean.parseBoolean(parameter);
	}

	static String getParameter(Parameter parameter) {
		assert parameter != null;
		final String name = parameter.getCode();
		return getParameterByName(name);
	}

	private static String getParameterByName(String parameterName) {
		assert parameterName != null;
		final String globalName = PARAMETER_SYSTEM_PREFIX + parameterName;
		String result = System.getProperty(globalName);
		if (result != null) {
			return result;
		}
		if (servletContext != null) {
			result = servletContext.getInitParameter(globalName);
			if (result != null) {
				return result;
			}
			// issue 463: in a ServletContextListener, it's also possible to call servletContext.setAttribute("javamelody.log", "true"); for example
			final Object attribute = servletContext.getAttribute(globalName);
			if (attribute instanceof String) {
				return (String) attribute;
			}
		}
		
		if(moduleCfg != null) {
			result = moduleCfg.getParameters().get(parameterName);
			if (result != null) {
				return result;
			}
		}
		
		return null;
	}
}
