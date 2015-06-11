package org.dcm4chee.archive.monitoring.impl.jdbc;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingException;
import javax.naming.Referenceable;
import javax.servlet.ServletContext;
import javax.sql.DataSource;

final class JdbcWrapperHelper {
	private static final Map<String, DataSource> JNDI_DATASOURCES_BACKUP = new LinkedHashMap<String, DataSource>();

	private static final Map<Class<?>, Constructor<?>> PROXY_CACHE = Collections
			.synchronizedMap(new WeakHashMap<Class<?>, Constructor<?>>());

	private JdbcWrapperHelper() {
		// NOOOP
	}

	static void rebindInitialDataSources(ServletContext servletContext) throws Throwable {
		try {
			final InitialContext initialContext = new InitialContext();
			for (final Map.Entry<String, DataSource> entry : JNDI_DATASOURCES_BACKUP.entrySet()) {
				final String jndiName = entry.getKey();
				final DataSource dataSource = entry.getValue();
		
				initialContext.rebind(jndiName, dataSource);
			}
			initialContext.close();
		} finally {
			JNDI_DATASOURCES_BACKUP.clear();
		}
	}

	static Map<String, DataSource> getJndiDataSources() throws NamingException {
		final Map<String, DataSource> dataSources = new LinkedHashMap<String, DataSource>(2);
		final String datasourcesParameter = Parameters.getParameter(Parameter.DATASOURCES);
		if (datasourcesParameter == null) {
			dataSources.putAll(getJndiDataSourcesAt("java:comp/env/jdbc"));
			dataSources.putAll(getJndiDataSourcesAt("java:/jdbc"));
			dataSources.putAll(getJndiDataSourcesAt("java:global/jdbc"));
			dataSources.putAll(getJndiDataSourcesAt("jdbc"));
		} else if (datasourcesParameter.trim().length() != 0) { // NOPMD
			final InitialContext initialContext = new InitialContext();
			for (final String datasource : datasourcesParameter.split(",")) {
				final String jndiName = datasource.trim();
	
				final DataSource dataSource = (DataSource) initialContext.lookup(jndiName);
				dataSources.put(jndiName, dataSource);
			}
			initialContext.close();
		}
		return Collections.unmodifiableMap(dataSources);
	}

	private static Map<String, DataSource> getJndiDataSourcesAt(String jndiPrefix) throws NamingException {
		final InitialContext initialContext = new InitialContext();
		final Map<String, DataSource> dataSources = new LinkedHashMap<String, DataSource>(2);
		try {
			for (final NameClassPair nameClassPair : Collections.list(initialContext.list(jndiPrefix))) {
				final String jndiName;
				if (nameClassPair.getName().startsWith("java:")) {
					jndiName = nameClassPair.getName();
				} else {
					jndiName = jndiPrefix + '/' + nameClassPair.getName();
				}
				final Object value = initialContext.lookup(jndiName);
				if (value instanceof DataSource) {
					dataSources.put(jndiName, (DataSource) value);
				}
			}
		} catch (final NamingException e) {
			return dataSources;
		}
		initialContext.close();
		return dataSources;
	}

	static Object getFieldValue(Object object, String fieldName) throws IllegalAccessException {
		return getAccessibleField(object, fieldName).get(object);
	}

	static void setFieldValue(Object object, String fieldName, Object value)
			throws IllegalAccessException {
		getAccessibleField(object, fieldName).set(object, value);
	}

	private static Field getAccessibleField(Object object, String fieldName) {
		assert fieldName != null;
		Class<?> classe = object.getClass();
		Field result = null;
		do {
			for (final Field field : classe.getDeclaredFields()) {
				if (fieldName.equals(field.getName())) {
					result = field;
					break;
				}
			}
			classe = classe.getSuperclass();
		} while (result == null && classe != null);

		assert result != null;
		setFieldAccessible(result);
		return result;
	}

	private static void setFieldAccessible(final Field field) {
		AccessController.doPrivileged(new PrivilegedAction<Object>() {
			@Override
			public Object run() {
				field.setAccessible(true);
				return null;
			}
		});
	}

	static void clearProxyCache() {
		PROXY_CACHE.clear();
	}

	@SuppressWarnings("unchecked")
	static <T> T createProxy(T object, InvocationHandler invocationHandler, List<Class<?>> interfaces) {
		final Class<? extends Object> objectClass = object.getClass();
		
		Constructor<?> constructor = PROXY_CACHE.get(objectClass);
		if (constructor == null) {
			final Class<?>[] interfacesArray = getObjectInterfaces(objectClass, interfaces);
			constructor = getProxyConstructor(objectClass, interfacesArray);
			
			if (interfaces == null) {
				PROXY_CACHE.put(objectClass, constructor);
			}
		}
		try {
			return (T) constructor.newInstance(new Object[] { invocationHandler });
		} catch (final Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private static Constructor<?> getProxyConstructor(Class<? extends Object> objectClass, Class<?>[] interfacesArray) {
		final ClassLoader classLoader = objectClass.getClassLoader(); // NOPMD
		try {
			return Proxy.getProxyClass(classLoader, interfacesArray).getConstructor(
					new Class[] { InvocationHandler.class });
		} catch (final NoSuchMethodException e) {
			throw new IllegalStateException(e);
		}
	}

	private static Class<?>[] getObjectInterfaces(Class<?> objectClass, List<Class<?>> interfaces) {
		final List<Class<?>> myInterfaces;
		if (interfaces == null) {
			myInterfaces = new ArrayList<Class<?>>(Arrays.asList(objectClass.getInterfaces()));
			Class<?> classe = objectClass.getSuperclass();
			while (classe != null) {
				final Class<?>[] classInterfaces = classe.getInterfaces();
				if (classInterfaces.length > 0) {
					final List<Class<?>> superInterfaces = Arrays.asList(classInterfaces);
					
					myInterfaces.removeAll(superInterfaces);
					myInterfaces.addAll(superInterfaces);
				}
				classe = classe.getSuperclass();
			}
			
			myInterfaces.remove(Referenceable.class);
		} else {
			myInterfaces = interfaces;
		}
		return myInterfaces.toArray(new Class<?>[myInterfaces.size()]);
	}

}
