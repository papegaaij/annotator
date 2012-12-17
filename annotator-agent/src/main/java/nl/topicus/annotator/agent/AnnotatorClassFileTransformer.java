package nl.topicus.annotator.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnnotatorClassFileTransformer implements ClassFileTransformer {
	private static final Logger log = LoggerFactory
			.getLogger(AnnotatorClassFileTransformer.class);

	@Override
	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {
		log.debug("Transforming " + className);
		return classfileBuffer;
	}
}
