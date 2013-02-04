package org.lds.sso.appwrap.bootstrap;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.easymock.classextension.ConstructorArgs;
import org.easymock.classextension.EasyMock;
import org.lds.sso.appwrap.Service;
import org.lds.sso.appwrap.exception.ServerFailureException;
import org.testng.Assert;
import org.testng.annotations.Test;

public class RemoteStartStopTest {
	private static final String CONFIG = getConfigXml();
	
	private static final String getConfigXml() {
		System.getProperties().remove("non-existent-sys-prop");
		URL filePath = RemoteStartStopTest.class.getClassLoader().getResource("LocalHostTestConfig.xml");
		return 
	            "string:<?xml version='1.0' encoding='UTF-8'?>"
	            + "<?alias site=labs-local.lds.org?>"
                + "<?file-alias policy-src-xml=\"" + filePath.getPath() + "\"?>"
                + "<?system-alias usr-src-xml=non-existent-sys-prop default="
	            + "\""
	            + "  <users>"
	            + "    <user name='nnn' pwd='pwd'>"
	            + "    </user>"
	            + "  </users>"
	        	+ "\"?>"
	        	+ "<config console-port='8088' proxy-port='8045'>"
                + " <sso-cookie name='lds-policy' domain='localhost' />"
                + " <proxy-timeout inboundMillis='400000' outboundMillis='400000'/>"
                + " <sso-traffic strip-empty-headers='true'>"
                + "  <by-site scheme='http' host='localhost' port='45'>"
                + "    <cctx-mapping thost='127.0.0.1' tport='1000'>"
                + "      <policy-source>xml={{policy-src-xml}}</policy-source>"
                + "    </cctx-mapping>"
                + "  </by-site>"
                + " </sso-traffic>"
                + " <user-source type='xml'>{{usr-src-xml}}</user-source>"
                + "</config>";
	}

	@Test
	public void testRemoteStart() throws Exception {
		HttpURLConnection connection = EasyMock.createMock(HttpURLConnection.class);
		RemoteStartServiceCommand start = new EasyMockBuilder<RemoteStartServiceCommand>(RemoteStartServiceCommand.class)
			.constructorArgs(CONFIG, 5000)
			.methods(true, "executeJavaCommand", "openConnection")
			.createMock();
		RemoteStartServiceCommand slowStart = new EasyMockBuilder<RemoteStartServiceCommand>(RemoteStartServiceCommand.class)
			.constructorArgs(CONFIG, 1000)
			.methods(true, "executeJavaCommand", "openConnection", "onTimeout")
			.createMock();
		EasyMock.expect(connection.getResponseCode()).andReturn(404).andReturn(200).andReturn(404);
		EasyMock.expect(start.openConnection(start.getCheckUrl(8088))).andReturn(connection).times(2);
		EasyMock.expect(slowStart.openConnection(slowStart.getCheckUrl(8088))).andReturn(connection).times(1);
		start.executeJavaCommand((String[])EasyMock.anyObject());
		EasyMock.expectLastCall();
		slowStart.executeJavaCommand((String[])EasyMock.anyObject());
		EasyMock.expectLastCall();
		EasyMock.replay(start, slowStart, connection);

		Service.invoke(start);
		try {
			Service.invoke(slowStart);
			Assert.fail("Should have timed out");
		} catch ( ServerFailureException e ) {
		}

		EasyMock.verify(start, slowStart, connection);
		EasyMock.reset(start, slowStart, connection);
	}

	public void testRemoteStop() throws Exception {
		HttpURLConnection isAliveConnection = EasyMock.createMock(HttpURLConnection.class);
		HttpURLConnection shutdownConnection = EasyMock.createMock(HttpURLConnection.class);
		RemoteStopServiceCommand stop = new EasyMockBuilder<RemoteStopServiceCommand>(RemoteStopServiceCommand.class)
			.constructorArgs(CONFIG, 5000)
			.methods(true, "openConnection")
			.createMock();
		RemoteStartServiceCommand slowStop = new EasyMockBuilder<RemoteStartServiceCommand>(RemoteStartServiceCommand.class)
			.constructorArgs(CONFIG, 1000)
			.methods(true, "openConnection")
			.createMock();

		EasyMock.expect(isAliveConnection.getResponseCode()).andReturn(404).andReturn(200).andReturn(404);
		EasyMock.expect(shutdownConnection.getResponseCode()).andReturn(200).times(2);
		shutdownConnection.connect();
		EasyMock.expectLastCall().times(2);
		EasyMock.expect(stop.openConnection(stop.getCheckUrl(8088))).andReturn(isAliveConnection).times(2);
		EasyMock.expect(slowStop.openConnection(slowStop.getCheckUrl(8088))).andReturn(isAliveConnection).times(1);
		EasyMock.expect(stop.openConnection(stop.getShutdownURL(8088))).andReturn(shutdownConnection);
		EasyMock.expect(slowStop.openConnection(stop.getShutdownURL(8088))).andReturn(shutdownConnection);

		EasyMock.replay(stop, slowStop, isAliveConnection, shutdownConnection);

		Service.invoke(stop);
		try {
			Service.invoke(slowStop);
			Assert.fail("Should have timed out");
		} catch ( ServerFailureException e ) {
		}

		EasyMock.verify(stop, slowStop, isAliveConnection, shutdownConnection);
		EasyMock.reset(stop, slowStop, isAliveConnection, shutdownConnection);
	}


	private static class EasyMockBuilder<T> {
		private Constructor<T> con;
		private Object[] initArgs = new Object[0];
		private Method[] mockMethods = new Method[0];
		private Class<T> clazz;

		public EasyMockBuilder(Class<T> clazz) {
			this.clazz = clazz;
		}

		private T createMock() {
			if ( con != null ) {
				return EasyMock.createMock(clazz, new ConstructorArgs(con, initArgs), mockMethods);
			} else if ( mockMethods != null ) {
				return EasyMock.createMock(clazz, mockMethods);
			} else {
				return EasyMock.createMock(clazz);
			}
		}

		public EasyMockBuilder<T> methods(boolean include, String... methodNames) {
			Set<Method> allMethods = new HashSet<Method>(Arrays.asList(clazz.getDeclaredMethods()));
			Class<?> superClazz = clazz.getSuperclass();
			while ( superClazz != Object.class ) {
				allMethods.addAll(Arrays.asList(superClazz.getDeclaredMethods()));
				superClazz = superClazz.getSuperclass();
			}
			List<Method> filteredMethods = new ArrayList<Method>();
			List<String> methodNamesList = Arrays.asList(methodNames);
			for ( Method method : allMethods ) {
				boolean inMethodNames = methodNamesList != null && methodNamesList.contains(method.getName());
				if ( !(include ^ inMethodNames) ) {
					filteredMethods.add(method);
				}
			}
			mockMethods = filteredMethods.toArray(new Method[filteredMethods.size()]);
			return this;
		}

		public EasyMockBuilder<T> constructorArgs(Object... args) {
			Class<?>[] argTypes = new Class<?>[args.length];
			for ( int i = 0; i < args.length; i++ ) {
				argTypes[i] = args[i].getClass();
			}
			initArgs = args;
			try {
				con = clazz.getDeclaredConstructor(argTypes);
			} catch ( Exception e ) {
				throw new RuntimeException(e);
			}
			return this;
		}
	}
}
