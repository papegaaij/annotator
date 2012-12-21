package nl.topicus.annotator;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import nl.topicus.annotator.impl.AnnationUpdateAction;
import nl.topicus.annotator.impl.AnnotationCollectionHandler;
import nl.topicus.annotator.impl.Types;

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
		ReflectiveOperationException e = null;
		try {
			return proxyClass.newInstance();
		} catch (InstantiationException | IllegalAccessException ce) {
			e = ce;
		}
		
		for (Constructor<?> curConstructor : proxyClass.getConstructors()) {
			try {
				return instantiate(curConstructor);
			} catch (InstantiationException | IllegalAccessException
					| InvocationTargetException ce) {
				if (e == null)
					e = ce;
			}
		}
		throw new RuntimeException(
				"Non of the constructors was able to instantiate the proxy", e);
	}

	@SuppressWarnings("unchecked")
	private T instantiate(Constructor<?> constructor)
			throws InstantiationException, IllegalAccessException,
			InvocationTargetException {
		List<Object> args = new ArrayList<>();
		for (Class<?> curArg : constructor.getParameterTypes()) {
			args.add(Types.defaultValue(curArg));
		}
		return (T) constructor.newInstance(args.toArray());
	}

	public <A extends Annotation> ClassAnnotator<T> mergeOnClass(
			AnnotationBuilder<A> builder) {
		if (!annotator.isAnnotationPresent(classToAnnotate,
				builder.annotationType())) {
			throw new IllegalArgumentException(classToAnnotate.getName()
					+ " is not annotated with @"
					+ builder.annotationType().getName()
					+ ", nothing to merge with");
		}
		builder.baseOn(annotator.getAnnotation(classToAnnotate,
				builder.annotationType()));
		return setOnClass(builder);
	}

	public <A extends Annotation> ClassAnnotator<T> addToClass(
			AnnotationBuilder<A> builder) {
		if (annotator.isAnnotationPresent(classToAnnotate,
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

	public <A extends Annotation> T mergeOnMethod(AnnotationBuilder<A> builder) {
		((ProxyObject) classProxy)
				.setHandler(new AnnotationCollectionHandler<>(annotator,
						builder, AnnationUpdateAction.MERGE));
		return classProxy;
	}

	public <A extends Annotation> T addToMethod(AnnotationBuilder<A> builder) {
		((ProxyObject) classProxy)
				.setHandler(new AnnotationCollectionHandler<>(annotator,
						builder, AnnationUpdateAction.ADD));
		return classProxy;
	}

	public <A extends Annotation> T setOnMethod(AnnotationBuilder<A> builder) {
		((ProxyObject) classProxy)
				.setHandler(new AnnotationCollectionHandler<>(annotator,
						builder, AnnationUpdateAction.SET));
		return classProxy;
	}
}
