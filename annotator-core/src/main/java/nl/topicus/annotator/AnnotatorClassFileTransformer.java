package nl.topicus.annotator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.StringMemberValue;

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
		if (className.equals("nl/topicus/annotator/BasicTests")) {
			ClassPool pool = ClassPool.getDefault();
			try {
				CtClass jClass = pool.makeClass(new ByteArrayInputStream(
						classfileBuffer));
				ClassFile classFile = jClass.getClassFile();
				ConstPool cp = classFile.getConstPool();
				AnnotationsAttribute attr = new AnnotationsAttribute(cp,
						AnnotationsAttribute.visibleTag);
				Annotation a = new Annotation("java/lang/Deprecated", cp);
				attr.setAnnotation(a);
				classFile.addAttribute(attr);
				return jClass.toBytecode();
			} catch (IOException | CannotCompileException e) {
				throw new RuntimeException(e);
			}
		}
		return classfileBuffer;
	}
}
