package nl.topicus.annotator.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import nl.topicus.annotator.AnnotationBuilder;
import nl.topicus.annotator.Annotator;

public class AnnotationCollectionHandler<A extends Annotation> extends
		StubMethodHandler {
	private Annotator annotator;
	private AnnotationBuilder<A> builder;

	public AnnotationCollectionHandler(Annotator annotator,
			AnnotationBuilder<A> builder) {
		this.annotator = annotator;
		this.builder = builder;
	}

	@Override
	public Object invoke(Object self, Method thisMethod, Method proceed,
			Object[] args) throws Throwable {
		annotator.add(thisMethod, builder);
		return createReturnValue(thisMethod);
	}
}
