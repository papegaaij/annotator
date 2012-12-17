package nl.topicus.annotator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

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
		annotator.add(thisMethod, builder.build());
		return createReturnValue(thisMethod);
	}
}
