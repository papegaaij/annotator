package nl.topicus.annotator.impl;

import java.util.Map;

import org.slf4j.LoggerFactory;

import org.slf4j.Logger;

import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.annotation.Annotation;

public class ClassAnnotationCreator extends AbstractAnnotationCreator {
	private static final Logger log = LoggerFactory
			.getLogger(ClassAnnotationCreator.class);

	public ClassAnnotationCreator(String annotationName,
			Map<String, Object> values) {
		super(annotationName, values);
	}

	@Override
	protected void addAnnotationToElement(CtClass jClass, Annotation annotation)
			throws NotFoundException {
		if (log.isInfoEnabled()) {
			log.info("Adding " + annotation + " to " + jClass);
		}
		AnnotationsAttribute attr = findOrCreateAttribute(jClass);
		attr.addAnnotation(annotation);
	}

	private AnnotationsAttribute findOrCreateAttribute(CtClass jClass) {
		ClassFile classFile = jClass.getClassFile();
		AnnotationsAttribute attr = (AnnotationsAttribute) classFile
				.getAttribute(AnnotationsAttribute.visibleTag);
		if (attr == null) {
			attr = new AnnotationsAttribute(classFile.getConstPool(),
					AnnotationsAttribute.visibleTag);
			classFile.addAttribute(attr);
		}
		return attr;
	}
}
