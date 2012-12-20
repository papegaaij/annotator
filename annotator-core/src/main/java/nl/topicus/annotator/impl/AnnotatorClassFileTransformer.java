package nl.topicus.annotator.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.List;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnnotatorClassFileTransformer implements ClassFileTransformer {
	private static final Logger log = LoggerFactory
			.getLogger(AnnotatorClassFileTransformer.class);

	private List<? extends AnnotationMutator> mutators;

	public AnnotatorClassFileTransformer(
			List<? extends AnnotationMutator> mutators) {
		this.mutators = mutators;
	}

	@Override
	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) {
		log.warn("Transforming " + className);
		ClassPool pool = ClassPool.getDefault();
		try {
			CtClass jClass = pool.makeClass(new ByteArrayInputStream(
					classfileBuffer));
			for (AnnotationMutator curCreator : mutators) {
				curCreator.mutate(jClass);
			}
			byte[] ret = jClass.toBytecode();
			jClass.defrost();
			return ret;
		} catch (IOException | CannotCompileException | NotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

}
