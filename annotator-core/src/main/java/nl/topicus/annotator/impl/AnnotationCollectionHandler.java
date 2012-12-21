package nl.topicus.annotator.impl;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.topicus.annotator.AnnotationBuilder;
import nl.topicus.annotator.Annotator;

public class AnnotationCollectionHandler<A extends Annotation> extends
		StubMethodHandler {
	private static final Logger log = LoggerFactory
			.getLogger(AnnotationCollectionHandler.class);

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
		boolean isAnnotated = annotator.isAnnotationPresent(thisMethod,
				builder.annotationType());
		if (log.isDebugEnabled()) {
			log.debug("Annotating " + thisMethod + " with @"
					+ builder.annotationType().getSimpleName());
		}
		if (updateAction == AnnationUpdateAction.ADD && isAnnotated) {
			throw new IllegalArgumentException(thisMethod.getName()
					+ " is already annotated with @"
					+ builder.annotationType().getName());
		}
		if (updateAction == AnnationUpdateAction.MERGE) {
			if (!isAnnotated) {
				throw new IllegalArgumentException(thisMethod.getName()
						+ " is not annotated with @"
						+ builder.annotationType().getName()
						+ ", nothing to merge with");
			} else {
				A oldAnn = annotator.getAnnotation(thisMethod,
						builder.annotationType());
				if (log.isDebugEnabled()) {
					log.debug("Starting with " + oldAnn);
				}
				builder.baseOn(oldAnn);
			}
		} else {
			if (log.isDebugEnabled()) {
				A oldAnn = annotator.getAnnotation(thisMethod,
						builder.annotationType());
				if (oldAnn != null) {
					log.debug("Replacing " + oldAnn);
				}
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
