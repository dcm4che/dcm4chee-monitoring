
package org.dcm4chee.archive.monitoring.impl.jdbc;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.sql.DataSource;

import org.dcm4chee.archive.monitoring.impl.core.Counter;
import org.dcm4chee.archive.monitoring.impl.core.MetricFactory;
import org.dcm4chee.archive.monitoring.impl.core.MetricProvider;
import org.dcm4chee.archive.monitoring.impl.core.Timer;
import org.dcm4chee.archive.monitoring.impl.core.context.MonitoringContext;
import org.dcm4chee.archive.monitoring.impl.core.context.MonitoringContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class JdbcWrapper {
	private static final Logger LOG = LoggerFactory.getLogger(JdbcWrapper.class);
	
	private static final String[] USED_CONNECTIONS_CXT = new String[] { "jdbc", "connections", "used" };
	private static final String[] ACTIVE_CONNECTIONS_CXT = new String[] { "jdbc", "connections", "active" };
	
	private static final String[] JDBC_CONNECTIONS = new String[] { "jdbc", "connections" };
	private static final String CONNECTION = "connection";
	private static final String STATEMENT = "statement";

	private static final String PREPARE_STATEMENT_METHOD_NAME = "prepareStatement";
	private static final String PREPARE_CALL_METHOD_NAME = "prepareCall";
	private static final String ADD_BATCH_METHOD_NAME = "addBatch";
	private static final String CLOSE_METHOD_NAME = "close";
	
	private static final String EXECTURE_METHOD_NAME_PREFIX = "execute";
	private static final String EXPLAIN_STATEMENT_PREFIX = "explain ";
	
	/**
	 * Instance singleton
	 */
	public static final JdbcWrapper SINGLETON = new JdbcWrapper();

	private ServletContext servletContext;
	private boolean connectionInformationsEnabled;
	
	// JdbcWrapper is initialized by server before monitoring is configured (and MetricProvider is created) 
	// => ensure to access MetricProvider lazy 
	private static MetricFactory getMetricFactory() {
		return MetricProvider.getInstance().getMetricFactory();
	}
	
	// JdbcWrapper is initialized by server before monitoring is configured (and MetricProvider is created) 
	// => ensure to access MetricProvider lazy
	private static MonitoringContextProvider getContextProvider() {
		return MetricProvider.getInstance().getMonitoringContextProvider();
	}
	
	private class StatementInvocationHandler implements InvocationHandler {
		private String requestName;
		private final Statement statement;
		private MonitoringContext monitoringContext;

		private StatementInvocationHandler(String query, Statement statement, MonitoringContext parentMonitoringContext) {
			this.requestName = query;
			this.statement = statement;
			initMonitoringContext(parentMonitoringContext);
		}
		
		private void initMonitoringContext(MonitoringContext parentMonitoringContext) {
			this.monitoringContext = parentMonitoringContext.getOrCreateInstanceContext(statement, STATEMENT);
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			final String methodName = method.getName();
			if (isEqualsMethod(methodName, args)) {
				return statement.equals(args[0]);
			} else if (isHashCodeMethod(methodName, args)) {
				return statement.hashCode();
			} else if (methodName.startsWith(EXECTURE_METHOD_NAME_PREFIX)) {
				if (isFirstArgAString(args)) {
					requestName = (String) args[0];
				}

				requestName = String.valueOf(requestName);

				Object result = doExecute(requestName, statement, method, args, monitoringContext);
				
				monitoringContext.dispose();
				
				return result;
			} else if (ADD_BATCH_METHOD_NAME.equals(methodName) && isFirstArgAString(args)) {
				requestName = (String) args[0];
			}

			return method.invoke(statement, args);
		}

		private boolean isFirstArgAString(Object[] args) {
			return args != null && args.length > 0 && args[0] instanceof String;
		}
	}

	private class ConnectionInvocationHandler implements InvocationHandler {
		private final Connection connection;
		private boolean alreadyClosed;
		private MonitoringContext monitoringContext;

		private ConnectionInvocationHandler(Connection connection) {
			this.connection = connection;
		}
		
		private void initMonitoringContext() {
			this.monitoringContext = getContextProvider().getActiveInstanceContext().getOrCreateInstanceContext(connection, CONNECTION);
		}

		private void init() {
			MonitoringContext context = getContextProvider().getNodeContext().getOrCreateInstanceContext(connection, JDBC_CONNECTIONS);
			getMetricFactory().register(context, new ConnectionInformation());
			
			incUsedConnectionCounter();
		}
		
		private void incUsedConnectionCounter() {
			Counter usedConnectionCounter = getMetricFactory().counter(getContextProvider().getNodeContext().getOrCreateContext(USED_CONNECTIONS_CXT), Counter.TYPE.DEFAULT);
			usedConnectionCounter.inc();
		}
		
		private void decUsedConnectionCounter() {
			Counter usedConnectionCounter = getMetricFactory().counter(getContextProvider().getNodeContext().getOrCreateContext(USED_CONNECTIONS_CXT), Counter.TYPE.DEFAULT);
			usedConnectionCounter.dec();
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			final String methodName = method.getName();
			if (isEqualsMethod(methodName, args)) {
				return areConnectionsEquals(args[0]);
			} else if (isHashCodeMethod(methodName, args)) {
				return connection.hashCode();
			}
			try {
				Object result = method.invoke(connection, args);
				if (result instanceof Statement) {
					final String requestName;
					if (PREPARE_STATEMENT_METHOD_NAME.equals(methodName) || PREPARE_CALL_METHOD_NAME.equals(methodName)) {
						requestName = (String) args[0];
					} else {
						requestName = null;
					}
					initMonitoringContext();
					result = createStatementProxy(requestName, (Statement) result, monitoringContext);
				}
				return result;
			} finally {
				if (CLOSE_METHOD_NAME.equals(methodName) && !alreadyClosed) {
					decUsedConnectionCounter();
					
					MonitoringContext context = getContextProvider().getNodeContext().getOrCreateInstanceContext(connection, JDBC_CONNECTIONS);
					context.dispose();
					alreadyClosed = true;
				}
			}
		}

		private boolean areConnectionsEquals(Object object) {
			if (Proxy.isProxyClass(object.getClass())) {
				final InvocationHandler invocationHandler = Proxy.getInvocationHandler(object);
				if (invocationHandler instanceof DelegatingInvocationHandler) {
					final DelegatingInvocationHandler d = (DelegatingInvocationHandler) invocationHandler;
					if (d.getDelegate() instanceof ConnectionInvocationHandler) {
						final ConnectionInvocationHandler c = (ConnectionInvocationHandler) d
								.getDelegate();
						return connection.equals(c.connection);
					}
				}
			}
			return connection.equals(object);
		}
	}

	private static class ConnectionManagerInvocationHandler extends AbstractInvocationHandler<Object> {
		private static final long serialVersionUID = 1L;

		private ConnectionManagerInvocationHandler(Object javaxConnectionManager) {
			super(javaxConnectionManager);
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			final Object result = method.invoke(getProxiedObject(), args);
			if (result instanceof Connection) {
				SINGLETON.rewrapConnection((Connection) result);
			}
			return result;
		}
	}

	private abstract static class AbstractInvocationHandler<T> implements InvocationHandler, Serializable {
		private static final long serialVersionUID = 1L;

		@SuppressWarnings("all")
		private final T proxiedObject;

		AbstractInvocationHandler(T proxiedObject) {
			super();
			this.proxiedObject = proxiedObject;
		}

		T getProxiedObject() {
			return proxiedObject;
		}
	}

	private static class DelegatingInvocationHandler implements InvocationHandler, Serializable {
		private static final long serialVersionUID = 7515240588169084785L;
		@SuppressWarnings("all")
		private final InvocationHandler delegate;

		private DelegatingInvocationHandler(InvocationHandler delegate) {
			this.delegate = delegate;
		}

		InvocationHandler getDelegate() {
			return delegate;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			try {
				return delegate.invoke(proxy, method, args);
			} catch (final InvocationTargetException e) {
				if (e.getTargetException() != null) {
					throw e.getTargetException();
				}
				throw e;
			}
		}
	}

	private JdbcWrapper() {
		this.servletContext = null;
		connectionInformationsEnabled = Parameters.isSystemActionsEnabled();
	}

	void initServletContext(ServletContext context) {
		this.servletContext = context;
		final String serverInfo = servletContext.getServerInfo();
		boolean jboss = serverInfo.contains("JBoss") || serverInfo.contains("WildFly");
		if ( !jboss ) {
			throw new IllegalStateException( "JDBC Wrapping only supported for JBoss & Wildfly");
		}
			
		connectionInformationsEnabled = Parameters.isSystemActionsEnabled();
	}

	boolean isConnectionInformationsEnabled() {
		return connectionInformationsEnabled;
	}

	private Object doExecute(String requestName, Statement statement, Method method, Object[] args, MonitoringContext statementMonitoringContext)
			throws IllegalAccessException, InvocationTargetException {
		if (requestName.startsWith(EXPLAIN_STATEMENT_PREFIX)) {
			incActiveConnectionCounter();
			try {
				return method.invoke(statement, args);
			} finally {
				decActiveConnectionCounter();
			}
		}

//		boolean systemError = true;
		Timer.Split timerContext = getMetricFactory().timer(statementMonitoringContext, Timer.TYPE.ONE_SHOT).time();
		try {
			incActiveConnectionCounter();
			
//			timerContext.setAttribute("sql.request", requestName);

			final Object result = method.invoke(statement, args);
//			systemError = false;
			return result;
		} catch (final InvocationTargetException e) {
			if (e.getCause() instanceof SQLException) {
				final int errorCode = ((SQLException) e.getCause()).getErrorCode();
//				if (errorCode >= 20000 && errorCode < 30000) {
//					systemError = false;
//				}
			}
			throw e;
		} finally {
			decActiveConnectionCounter();
//			if ( systemError ) {
//				timerContext.setAttribute("sql.error", "true");
//			}
			timerContext.stop();
		}
	}
	
	private void incActiveConnectionCounter() {
		Counter activeConnectionCounter = getMetricFactory().counter(getContextProvider().getNodeContext().getOrCreateContext(ACTIVE_CONNECTIONS_CXT), Counter.TYPE.DEFAULT);
		activeConnectionCounter.inc();
	}
	
	private void decActiveConnectionCounter() {
		Counter activeConnectionCounter = getMetricFactory().counter(getContextProvider().getNodeContext().getOrCreateContext(ACTIVE_CONNECTIONS_CXT), Counter.TYPE.DEFAULT);
		activeConnectionCounter.dec();
	}

	boolean rebindDataSources() {
		boolean ok;
		try {
			final Map<String, DataSource> jndiDataSources = JdbcWrapperHelper.getJndiDataSources();
			LOG.debug("datasources found in JNDI: " + jndiDataSources.keySet());
			for (final Map.Entry<String, DataSource> entry : jndiDataSources.entrySet()) {
				final String jndiName = entry.getKey();
				final DataSource dataSource = entry.getValue();
				
				rewrapDataSource(jndiName, dataSource);
			}
			ok = true;
		} catch (final Throwable t) { // NOPMD
			LOG.debug("rebinding datasources failed, skipping", t);
			ok = false;
		}
		return ok;
	}

	private void rewrapDataSource(String jndiName, DataSource dataSource) throws IllegalAccessException {
		final String dataSourceClassName = dataSource.getClass().getName();
		LOG.debug("Datasource needs rewrap: " + jndiName + " of class " + dataSourceClassName);
		final String dataSourceRewrappedMessage = "Datasource rewrapped: " + jndiName;
	
		Object javaxConnectionManager = JdbcWrapperHelper.getFieldValue(dataSource, "cm");
		javaxConnectionManager = createJavaxConnectionManagerProxy(javaxConnectionManager);
		JdbcWrapperHelper.setFieldValue(dataSource, "cm", javaxConnectionManager);
		LOG.debug(dataSourceRewrappedMessage);
	}

	boolean stop() {
		boolean ok;
		try {
			JdbcWrapperHelper.rebindInitialDataSources(servletContext);

			final Map<String, DataSource> jndiDataSources = JdbcWrapperHelper.getJndiDataSources();
			
			for (final Map.Entry<String, DataSource> entry : jndiDataSources.entrySet()) {
				final String jndiName = entry.getKey();
				final DataSource dataSource = entry.getValue();
				
				unwrapDataSource(jndiName, dataSource);
			}

			JdbcWrapperHelper.clearProxyCache();

			ok = true;
		} catch (final Throwable t) { // NOPMD
			LOG.debug("rebinding initial datasources failed, skipping", t);
			ok = false;
		}
		return ok;
	}

	private void unwrapDataSource(String jndiName, DataSource dataSource) throws IllegalAccessException {
		final String dataSourceClassName = dataSource.getClass().getName();
		LOG.debug("Datasource needs unwrap: " + jndiName + " of class " + dataSourceClassName);
		final String dataSourceUnwrappedMessage = "Datasource unwrapped: " + jndiName;
	
		unwrap(dataSource, "cm", dataSourceUnwrappedMessage);
	}

	private void unwrap(Object parentObject, String fieldName, String unwrappedMessage) throws IllegalAccessException {
		final Object proxy = JdbcWrapperHelper.getFieldValue(parentObject, fieldName);
		if (Proxy.isProxyClass(proxy.getClass())) {
			InvocationHandler invocationHandler = Proxy.getInvocationHandler(proxy);
			if (invocationHandler instanceof DelegatingInvocationHandler) {
				invocationHandler = ((DelegatingInvocationHandler) invocationHandler).getDelegate();
				if (invocationHandler instanceof AbstractInvocationHandler) {
					final Object proxiedObject = ((AbstractInvocationHandler<?>) invocationHandler)
							.getProxiedObject();
					JdbcWrapperHelper.setFieldValue(parentObject, fieldName, proxiedObject);
					LOG.debug(unwrappedMessage);
				}
			}
		}
	}

	private Object createJavaxConnectionManagerProxy(Object javaxConnectionManager) {
		final InvocationHandler invocationHandler = new ConnectionManagerInvocationHandler(
				javaxConnectionManager);
		return createProxy(javaxConnectionManager, invocationHandler);
	}

	private void rewrapConnection(Connection connection) throws IllegalAccessException {
		final Object baseWrapperManagedConnection = JdbcWrapperHelper.getFieldValue(connection, "mc");
		final String conFieldName = "con";
		Connection con = (Connection) JdbcWrapperHelper.getFieldValue(baseWrapperManagedConnection, conFieldName);

		if (!isProxyAlready(con)) {
			con = createConnectionProxy(con);
			JdbcWrapperHelper.setFieldValue(baseWrapperManagedConnection,
					conFieldName, con);
		}

	}

	public Connection createConnectionProxy(Connection connection) {
		if (isMonitoringDisabled()) {
			return connection;
		}
		final ConnectionInvocationHandler invocationHandler = new ConnectionInvocationHandler(
				connection);
		final Connection result = createProxy(connection, invocationHandler);
		
		if (result != connection) { // NOPMD
			invocationHandler.init();
		}
		return result;
	}

	private static boolean isMonitoringDisabled() {
		return false;
	}

	private Statement createStatementProxy(String query, Statement statement, MonitoringContext parentMonitoringContext) {
		final InvocationHandler invocationHandler = new StatementInvocationHandler(query, statement, parentMonitoringContext);
		return createProxy(statement, invocationHandler);
	}

	private static boolean isEqualsMethod(Object methodName, Object[] args) {
		// == for perf (strings interned: == is ok)
		return "equals" == methodName && args != null && args.length == 1; // NOPMD
	}

	private static boolean isHashCodeMethod(Object methodName, Object[] args) {
		// == for perf (strings interned: == is ok)
		return "hashCode" == methodName && (args == null || args.length == 0); // NOPMD
	}

	private static <T> T createProxy(T object, InvocationHandler invocationHandler) {
		return createProxy(object, invocationHandler, null);
	}

	private static <T> T createProxy(T object, InvocationHandler invocationHandler, List<Class<?>> interfaces) {
		if (isProxyAlready(object)) {
			return object;
		}
		final InvocationHandler ih = new DelegatingInvocationHandler(invocationHandler);
		return JdbcWrapperHelper.createProxy(object, ih, interfaces);
	}

	private static boolean isProxyAlready(Object object) {
		return Proxy.isProxyClass(object.getClass())
				&& Proxy.getInvocationHandler(object).getClass().getName()
						.equals(DelegatingInvocationHandler.class.getName());
	}

}
