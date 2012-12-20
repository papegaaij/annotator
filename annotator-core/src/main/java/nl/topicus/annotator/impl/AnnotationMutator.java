package nl.topicus.annotator.impl;

import javassist.CtClass;
import javassist.NotFoundException;

public interface AnnotationMutator {
	public void mutate(CtClass jClass) throws NotFoundException;
}
