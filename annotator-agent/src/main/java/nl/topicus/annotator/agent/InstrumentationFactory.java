/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package nl.topicus.annotator.agent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URL;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.PrivilegedAction;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.sun.tools.attach.VirtualMachine;

/**
 * Factory for obtaining an {@link Instrumentation} instance.
 * 
 * @author Marc Prud'hommeaux
 * @since 1.0.0
 */
public class InstrumentationFactory {
	private static Instrumentation _inst;
	private static boolean _dynamicallyInstall = true;
	private static final String _name = InstrumentationFactory.class.getName();

	/**
	 * This method is not synchronized because when the agent is loaded from
	 * getInstrumentation() that method will cause agentmain(..) to be called.
	 * Synchronizing this method would cause a deadlock.
	 * 
	 * @param inst
	 *            The instrumentation instance to be used by this factory.
	 */
	public static void setInstrumentation(Instrumentation inst) {
		_inst = inst;
	}

	/**
	 * Configures whether or not this instance should attempt to dynamically
	 * install an agent in the VM. Defaults to <code>true</code>.
	 */
	public static synchronized void setDynamicallyInstallAgent(boolean val) {
		_dynamicallyInstall = val;
	}

	/**
	 * @param log
	 *            OpenJPA log.
	 * @return null if Instrumentation can not be obtained, or if any Exceptions
	 *         are encountered.
	 */
	public static synchronized Instrumentation getInstrumentation() {
		if (_inst != null || !_dynamicallyInstall)
			return _inst;

		AccessController.doPrivileged(new PrivilegedAction<Object>() {
			public Object run() {
				if (!InstrumentationFactory.class.getClassLoader().equals(
						ClassLoader.getSystemClassLoader())) {
					throw new IllegalStateException(
							"Cannot load the agent when InstrumentationFactory"
									+ " is not loaded by the system class loader");
				}
				loadAgent(getAgentJar());
				return null;
			}
		});
		// If the load(...) agent call was successful, this variable will no
		// longer be null.
		if (_inst == null)
			throw new IllegalStateException("Dynamic loading of the agent failed");
		return _inst;
	}

	/**
	 * The method that is called when a jar is added as an agent at runtime. All
	 * this method does is store the {@link Instrumentation} for later use.
	 */
	public static void agentmain(String agentArgs, Instrumentation inst) {
		InstrumentationFactory.setInstrumentation(inst);
	}

	/**
	 * Create a new jar file for the sole purpose of specifying an Agent-Class
	 * to load into the JVM.
	 * 
	 * @return absolute path to the new jar file.
	 */
	private static String createAgentJar() throws IOException {
		File file = File.createTempFile(InstrumentationFactory.class.getName(),
				".jar");
		file.deleteOnExit();

		ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(file));
		zout.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));

		PrintWriter writer = new PrintWriter(new OutputStreamWriter(zout));

		writer.println("Agent-Class: " + InstrumentationFactory.class.getName());
		writer.println("Can-Redefine-Classes: true");
		writer.println("Can-Retransform-Classes: true");

		writer.close();

		return file.getAbsolutePath();
	}

	/**
	 * This private worker method will return a fully qualified path to a jar
	 * that has this class defined as an Agent-Class in it's
	 * META-INF/manifest.mf file. Under normal circumstances the path should
	 * point to the OpenJPA jar. If running in a development environment a
	 * temporary jar file will be created.
	 * 
	 * @return absolute path to the agent jar or null if anything unexpected
	 *         happens.
	 */
	private static String getAgentJar() {
		File agentJarFile = null;
		// Find the name of the File that this class was loaded from. That
		// jar *should* be the same location as our agent.
		CodeSource cs = InstrumentationFactory.class.getProtectionDomain()
				.getCodeSource();
		if (cs != null) {
			URL loc = cs.getLocation();
			if (loc != null) {
				agentJarFile = new File(loc.getFile());
			}
		}

		// Determine whether the File that this class was loaded from has this
		// class defined as the Agent-Class.
		boolean createJar = false;
		if (cs == null || agentJarFile == null || agentJarFile.isDirectory()) {
			createJar = true;
		} else if (!validateAgentJarManifest(agentJarFile, _name)) {
			// We have an agentJarFile, but this class isn't the Agent-Class.
			createJar = true;
		}

		String agentJar;
		if (createJar) {
			// This can happen when running in eclipse as an OpenJPA
			// developer or for some reason the CodeSource is null. We
			// should log a warning here because this will create a jar
			// in your temp directory that doesn't always get cleaned up.
			try {
				agentJar = createAgentJar();
				System.out.println("Loading agent from " + agentJar);
			} catch (IOException ioe) {
				throw new RuntimeException(ioe);
			}
		} else {
			agentJar = agentJarFile.getAbsolutePath();
		}

		return agentJar;
	}

	/**
	 * Attach and load an agent class.
	 * 
	 * @param log
	 *            Log used if the agent cannot be loaded.
	 * @param agentJar
	 *            absolute path to the agent jar.
	 * @param vmClass
	 *            VirtualMachine.class from tools.jar.
	 */
	private static void loadAgent(String agentJar) {
		try {
			// first obtain the PID of the currently-running process
			// ### this relies on the undocumented convention of the
			// RuntimeMXBean's
			// ### name starting with the PID, but there appears to be no other
			// ### way to obtain the current process' id, which we need for
			// ### the attach process
			RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
			String pid = runtime.getName();
			if (pid.indexOf("@") != -1)
				pid = pid.substring(0, pid.indexOf("@"));

			VirtualMachine vm = VirtualMachine.attach(pid);
			vm.loadAgent(agentJar, "");
			vm.detach();
		} catch (Throwable t) {
			System.err.println(_name
					+ ".loadAgent() caught an exception. Message: "
					+ t.getMessage());
		}
	}

	/**
	 * This private worker method will validate that the provided agentClassName
	 * is defined as the Agent-Class in the manifest file from the provided jar.
	 * 
	 * @param agentJarFile
	 *            non-null agent jar file.
	 * @param log
	 *            non-null logger.
	 * @param agentClassName
	 *            the non-null agent class name.
	 * @return True if the provided agentClassName is defined as the Agent-Class
	 *         in the manifest from the provided agentJarFile. False otherwise.
	 */
	private static boolean validateAgentJarManifest(File agentJarFile,
			String agentClassName) {
		try (JarFile jar = new JarFile(agentJarFile)) {
			Manifest manifest = jar.getManifest();
			if (manifest == null) {
				return false;
			}
			Attributes attributes = manifest.getMainAttributes();
			String ac = attributes.getValue("Agent-Class");
			if (ac != null && ac.equals(agentClassName)) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
}
