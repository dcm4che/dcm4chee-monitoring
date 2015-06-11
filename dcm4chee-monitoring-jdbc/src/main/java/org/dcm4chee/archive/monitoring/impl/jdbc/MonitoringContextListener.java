package org.dcm4chee.archive.monitoring.impl.jdbc;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet context listener implementation which ensures wrapping of the JBoss JDBC Data Source for monitoring.
 * 
 * @author Alexander Hoermandinger <alexander.hoermandinger@agfa.com>
 */
public class MonitoringContextListener implements ServletContextListener {
	private static Logger LOGGER = LoggerFactory.getLogger(MonitoringContextListener.class);
	private static ServletContext servletContext;

	public MonitoringContextListener() {
		// NOOOP
	}	

	@Override
	public void contextInitialized(ServletContextEvent event) {
		final long start = System.currentTimeMillis();
	
		servletContext = event.getServletContext();
		
		System.getProperty("java.io.tmpdir");

		Parameters.initialize(servletContext);

		LOGGER.debug("Monitoring session context listener init started");
		
		final long duration = System.currentTimeMillis() - start;
		LOGGER.debug("Monitoring session context listener init done in " + duration + " ms");
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		LOGGER.debug("Monitoring session context listener destroy done");
	}
	
	public static ServletContext getServletContext() {
	    return servletContext;
	}
	
}
