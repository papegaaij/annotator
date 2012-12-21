package nl.topicus.annotator;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import nl.topicus.annotator.impl.AnnotationCollectionHandler;

public class ClassAnnotator<T> {
	private T classProxy;
	private Annotator annotator;
	private Class<T> classToAnnotate;

	public ClassAnnotator(Class<T> classToAnnotate, Annotator annotator) {
		this.classToAnnotate = classToAnnotate;
		classProxy = createProxy(classToAnnotate);
		this.annotator = annotator;
	}

	private T createProxy(Class<T> clazz) {
		ProxyFactory factory = new ProxyFactory();
		factory.setFilter(new MethodFilter() {
			public boolean isHandled(Method m) {
				return !m.getName().equals("finalize");
			}
		});
		if (clazz.isInterface()) {
			factory.setSuperclass(Object.class);
			factory.setInterfaces(new Class[] { clazz });
		} else {
			factory.setSuperclass(clazz);
		}

		@SuppressWarnings("unchecked")
		Class<T> proxyClass = factory.createClass();
		try {
			return proxyClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public <A extends Annotation> ClassAnnotator<T> addToClass(
			AnnotationBuilder<A> builder) {
		if (classToAnnotate.isAnnotationPresent(builder.annotationType())
				|| annotator.isAnnotationPresent(classToAnnotate,
						builder.annotationType())) {
			throw new IllegalArgumentException(classToAnnotate.getName()
					+ " is already annotated with @"
					+ builder.annotationType().getName());
		}
		return setOnClass(builder);
	}

	public <A extends Annotation> ClassAnnotator<T> setOnClass(
			AnnotationBuilder<A> builder) {
		Target target = builder.annotationType().getAnnotation(Target.class);
		boolean isClass = !classToAnnotate.isAnnotation();
		if (target != null
				&& !Arrays.asList(target.value()).contains(
						isClass ? ElementType.TYPE
								: ElementType.ANNOTATION_TYPE)) {
			throw new IllegalArgumentException("@" + builder.annotationType()
					+ " is not allowed on "
					+ (isClass ? "types" : "annotations"));
		}

		Retention retention = builder.annotationType().getAnnotation(
				Retention.class);
		if (retention == null || retention.value() != RetentionPolicy.RUNTIME) {
			throw new IllegalArgumentException("@" + builder.annotationType()
					+ " is not retained at runtime");
		}

		annotator.add(classToAnnotate, builder);
		return this;
	}

	public <A extends Annotation> T addToMethod(AnnotationBuilder<A> builder) {
		((ProxyObject) classProxy)
				.setHandler(new AnnotationCollectionHandler<>(annotator,
						builder, false));
		return classProxy;
	}

	public <A extends Annotation> T setOnMethod(AnnotationBuilder<A> builder) {
		((ProxyObject) classProxy)
				.setHandler(new AnnotationCollectionHandler<>(annotator,
						builder, true));
		return classProxy;
	}
}
