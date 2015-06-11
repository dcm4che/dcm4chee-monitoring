
package org.dcm4chee.archive.monitoring.impl.jdbc;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

final class Parameters {
	static final String PARAMETER_SYSTEM_PREFIX = "javamelody.";
	static final String JAVA_VERSION = System.getProperty("java.version");

	private static FilterConfig filterConfig;
	private static ServletContext servletContext;


	private Parameters() {
		super();
	}

	static void initialize(FilterConfig config) {
		filterConfig = config;
		if (config != null) {
			final ServletContext context = config.getServletContext();
			initialize(context);
		}
	}

	static void initialize(ServletContext context) {
		if ("1.6".compareTo(JAVA_VERSION) > 0) {
			throw new IllegalStateException("La version java doit être 1.6 au minimum et non "
					+ JAVA_VERSION);
		}
		servletContext = context;
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

	static String getParameterByName(String parameterName) {
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
		if (filterConfig != null) {
			result = filterConfig.getInitParameter(parameterName);
			if (result != null) {
				return result;
			}
		}
		return null;
	}
}
