package nl.topicus.annotator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class AnnotationCollectionHandler<A extends Annotation> extends
		StubMethodHandler {
	private AnnotationBuilder<A> builder;

	public AnnotationCollectionHandler(AnnotationBuilder<A> builder) {
		this.builder = builder;
	}

	@Override
	public Object invoke(Object self, Method thisMethod, Method proceed,
			Object[] args) throws Throwable {
		builder.bindTo(thisMethod);
		return createReturnValue(thisMethod);
	}
}
