package nl.topicus.annotator.impl;

import java.lang.reflect.Method;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.annotation.Annotation;
import javassist.util.proxy.RuntimeSupport;

public class MethodAnnotationCreator extends AbstractAnnotationCreator {
	private static final Logger log = LoggerFactory
			.getLogger(MethodAnnotationCreator.class);

	private String name;
	private String descriptor;

	public MethodAnnotationCreator(Method method, String annotationName,
			Map<String, Object> values) {
		super(annotationName, values);
		name = method.getName();
		descriptor = RuntimeSupport.makeDescriptor(method);
	}

	@Override
	protected void addAnnotationToElement(CtClass jClass, Annotation annotation)
			throws NotFoundException {
		CtMethod jMethod = jClass.getMethod(name, descriptor);
		if (log.isInfoEnabled()) {
			log.info("Adding " + annotation + " to " + jMethod.getLongName());
		}
		AnnotationsAttribute attr = findOrCreateAttribute(jMethod);
		attr.addAnnotation(annotation);
	}

	private AnnotationsAttribute findOrCreateAttribute(CtMethod jMethod) {
		MethodInfo methodInfo = jMethod.getMethodInfo();
		AnnotationsAttribute attr = (AnnotationsAttribute) methodInfo
				.getAttribute(AnnotationsAttribute.visibleTag);
		if (attr == null) {
			attr = new AnnotationsAttribute(methodInfo.getConstPool(),
					AnnotationsAttribute.visibleTag);
			methodInfo.addAttribute(attr);
		}
		return attr;
	}
}
