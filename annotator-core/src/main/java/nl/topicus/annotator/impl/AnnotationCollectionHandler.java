package nl.topicus.annotator.impl;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;

import nl.topicus.annotator.AnnotationBuilder;
import nl.topicus.annotator.Annotator;

public class AnnotationCollectionHandler<A extends Annotation> extends
		StubMethodHandler {
	private Annotator annotator;
	private AnnotationBuilder<A> builder;
	private AnnationUpdateAction updateAction;

	public AnnotationCollectionHandler(Annotator annotator,
			AnnotationBuilder<A> builder, AnnationUpdateAction updateAction) {
		this.annotator = annotator;
		this.builder = builder;
		this.updateAction = updateAction;
	}

	@Override
	public Object invoke(Object self, Method thisMethod, Method proceed,
			Object[] args) throws Throwable {
		A curAnn = annotator
				.getAnnotation(thisMethod, builder.annotationType());
		if (updateAction == AnnationUpdateAction.ADD && curAnn != null) {
			throw new IllegalArgumentException(thisMethod.getName()
					+ " is already annotated with @"
					+ builder.annotationType().getName());
		}
		if (updateAction == AnnationUpdateAction.MERGE) {
			if (curAnn == null) {
				throw new IllegalArgumentException(thisMethod.getName()
						+ " is not annotated with @"
						+ builder.annotationType().getName()
						+ ", nothing to merge with");
			} else {
				builder.baseOn(curAnn);
			}
		}

		Target target = builder.annotationType().getAnnotation(Target.class);
		if (target != null
				&& !Arrays.asList(target.value()).contains(ElementType.METHOD)) {
			throw new IllegalArgumentException("@" + builder.annotationType()
					+ " is not allowed on methods");
		}

		Retention retention = builder.annotationType().getAnnotation(
				Retention.class);
		if (retention == null || retention.value() != RetentionPolicy.RUNTIME) {
			throw new IllegalArgumentException("@" + builder.annotationType()
					+ " is not retained at runtime");
		}

		annotator.add(thisMethod, builder);
		return createReturnValue(thisMethod);
	}
}
