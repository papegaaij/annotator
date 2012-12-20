package nl.topicus.annotator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Map;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnnotatorClassFileTransformer implements ClassFileTransformer {
	private static final Logger log = LoggerFactory
			.getLogger(AnnotatorClassFileTransformer.class);
	private String annotationName;
	private Map<String, Object> values;

	public AnnotatorClassFileTransformer(String annotationName,
			Map<String, Object> values) {
		this.annotationName = annotationName;
		this.values = values;
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
			ClassFile classFile = jClass.getClassFile();
			ConstPool cp = classFile.getConstPool();
			AnnotationsAttribute attr = new AnnotationsAttribute(cp,
					AnnotationsAttribute.visibleTag);
			attr.setAnnotation(createAnnotation(jClass.getClassFile()
					.getConstPool()));
			// TODO moet ook met methods
			classFile.addAttribute(attr);
			return jClass.toBytecode();
		} catch (IOException | CannotCompileException | NotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	protected Annotation createAnnotation(ConstPool cp)
			throws NotFoundException {
		CtClass annotationCtClass = ClassPool.getDefault().get(annotationName);
		Annotation ret = new Annotation(cp, annotationCtClass);
		return ret;
	}
}
