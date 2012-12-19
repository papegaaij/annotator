package nl.topicus.annotator.agent;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.logging.Logger;

public class AnnotatorAgent {

	private static final Logger log = Logger.getLogger(AnnotatorAgent.class
			.getName());

	private static boolean loadAttempted = false;
	private static boolean loadSuccessful = false;
	private static boolean disableDynamicAgent = false;

	/**
	 * @return True if the Agent has ran successfully. False otherwise.
	 */
	public static synchronized boolean getLoadSuccessful() {
		return loadSuccessful;
	}

	/**
	 * @return True if the dynamic agent was disabled via configuration.
	 */
	public static void disableDynamicAgent() {
		disableDynamicAgent = true;
	}

	/**
	 * @param log
	 * @return True if the agent is loaded successfully
	 */
	public static synchronized boolean loadDynamicAgent() {
		log.info("Dynamically loading the Annotator agent");
		if (loadAttempted == false && disableDynamicAgent == false) {
			Instrumentation inst = InstrumentationFactory.getInstrumentation();
			if (inst != null) {
				premain("", inst);
				return true;
			}
			// If we successfully get the Instrumentation, we will call premain
			// where loadAttempted will be set to true. This case is the path
			// where we were unable to get Instrumentation so we need to set the
			// loadAttempted flag to true. We do this so we will only run
			// through this code one time.
			loadAttempted = true;
		}

		return false;
	}

	public static void premain(String args, Instrumentation inst) {
		// If the enhancer has already completed, noop. This can happen
		// if runtime enhancement is specified via javaagent and is also loaded
		// at runtime.
		synchronized (AnnotatorAgent.class) {
			if (loadAttempted == true) {
				return;
			}
			// See the comment in loadDynamicAgent as to why we set this to true
			// in multiple places.
			loadAttempted = true;
		}

		registerClassLoadEnhancer(inst);
		InstrumentationFactory.setDynamicallyInstallAgent(false);
		loadSuccessful = true;
	}

	private static void registerClassLoadEnhancer(Instrumentation inst) {
		//log.info("I will now annotate your classes");
		// inst.addTransformer(new AnnotatorClassFileTransformer(), true);
	}

	public static void retransform(Class<?>... classes) {
		try {
			InstrumentationFactory.getInstrumentation().retransformClasses(
					classes);
		} catch (UnmodifiableClassException e) {
			throw new RuntimeException(e);
		}
	}
}