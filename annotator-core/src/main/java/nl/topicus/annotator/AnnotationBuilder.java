package nl.topicus.annotator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;

import javax.enterprise.util.AnnotationLiteral;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;

import nl.topicus.annotator.impl.StubMethodHandler;

public abstract class AnnotationBuilder<A extends Annotation> {
	private static final Method ANNOTATION_TYPE_METHOD;
	static {
		try {
			ANNOTATION_TYPE_METHOD = AnnotationLiteral.class
					.getMethod("annotationType");
		} catch (NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}
	private A ann;
	protected Method selectedProperty;
	private Map<Method, Object> implicitValues = new HashMap<>();
	private Map<Method, Object> explicitValues = new HashMap<>();
	private Class<A> annotationClass;

	public AnnotationBuilder(Class<A> annotationClass) {
		this.annotationClass = annotationClass;
		this.ann = createAnnotationProxy(annotationClass);
		for (Method curProperty : annotationClass.getDeclaredMethods()) {
			if (curProperty.getDefaultValue() != null)
				implicitValues.put(curProperty, curProperty.getDefaultValue());
		}
		setup(ann);
	}

	public static <A extends Annotation> AnnotationBuilder<A> of(
			Class<A> annotationClass) {
		return new AnnotationBuilder<A>(annotationClass) {
			@Override
			public void setup(A ann) {
			}
		};
	}

	public abstract void setup(A ann);

	public void baseOn(A base) {
		try {
			for (Method curProperty : annotationClass.getDeclaredMethods()) {
				implicitValues.put(curProperty, curProperty.invoke(base));
			}
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	public <T, V extends T> AnnotationBuilder<A> set(
			Class<? extends T> annotationProperty, Class<V> value) {
		if (value == null)
			throw new NullPointerException("value cannot be null");
		explicitValues.put(selectedProperty, value);
		return this;
	}

	public Class<A> annotationType() {
		return annotationClass;
	}

	public <T, V extends T> AnnotationBuilder<A> set(T annotationProperty,
			V value) {
		if (value == null)
			throw new NullPointerException("value cannot be null");
		explicitValues.put(selectedProperty, value);
		return this;
	}

	public Object getValue(Method method) {
		Object ret = explicitValues.get(method);
		return ret == null ? implicitValues.get(method) : ret;
	}

	public A build() {
		assertComplete();

		ProxyFactory factory = new ProxyFactory();
		factory.setFilter(new MethodFilter() {
			public boolean isHandled(Method m) {
				return getValue(m) != null || m.equals(ANNOTATION_TYPE_METHOD);
			}
		});
		factory.setSuperclass(AnnotationLiteral.class);
		factory.setInterfaces(new Class[] { annotationClass });

		@SuppressWarnings("unchecked")
		Class<A> proxyClass = factory.createClass();
		try {
			A ret = proxyClass.newInstance();
			((ProxyObject) ret).setHandler(new StubMethodHandler() {
				@Override
				public Object invoke(Object self, Method m, Method proceed,
						Object[] args) throws Throwable {
					if (m.equals(ANNOTATION_TYPE_METHOD)) {
						return annotationClass;
					}
					return getValue(m);
				}
			});
			return ret;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private A createAnnotationProxy(Class<A> clazz) {
		ProxyFactory factory = new ProxyFactory();
		factory.setFilter(new MethodFilter() {
			public boolean isHandled(Method m) {
				return !m.getName().equals("finalize");
			}
		});
		factory.setSuperclass(Object.class);
		factory.setInterfaces(new Class[] { clazz });

		@SuppressWarnings("unchecked")
		Class<A> proxyClass = factory.createClass();
		try {
			A ret = proxyClass.newInstance();
			((ProxyObject) ret).setHandler(new StubMethodHandler() {
				@Override
				public Object invoke(Object self, Method m, Method proceed,
						Object[] args) throws Throwable {
					selectedProperty = m;
					return createReturnValue(m);
				}
			});
			return ret;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public Map<String, Object> values() {
		assertComplete();

		Map<String, Object> ret = new TreeMap<>();
		for (Method curProperty : annotationClass.getDeclaredMethods()) {
			ret.put(curProperty.getName(), convertValue(getValue(curProperty)));
		}
		return ret;
	}

	private Object convertValue(Object value) {
		if (value instanceof Class)
			return ((Class<?>) value).getName();
		else if (value instanceof Enum<?>)
			return ((Enum<?>) value).name();
		else if (value.getClass().isArray()) {
			List<Object> ret = new ArrayList<>();
			for (int index = 0; index < Array.getLength(value); index++) {
				ret.add(convertValue(Array.get(value, index)));
			}
			return ret;
		} else if (value instanceof Annotation) {
			Map<String, Object> ret = new TreeMap<>();
			for (Method curMethod : ((Annotation) value).annotationType()
					.getDeclaredMethods()) {
				try {
					ret.put(curMethod.getName(),
							convertValue(curMethod.invoke(value)));
				} catch (IllegalAccessException | InvocationTargetException e) {
					throw new RuntimeException(e);
				}
			}
			return ret;
		}
		return value;
	}

	private void assertComplete() {
		List<Method> missingMembers = new ArrayList<>();
		for (Method curProperty : annotationClass.getDeclaredMethods()) {
			if (getValue(curProperty) == null) {
				missingMembers.add(curProperty);
			}
		}
		if (!missingMembers.isEmpty()) {
			throw new IllegalStateException("The annotation @"
					+ annotationClass.getSimpleName()
					+ " must define the attribute(s) "
					+ Joiner.on(", ").join(
							FluentIterable.from(missingMembers).transform(
									new Function<Method, String>() {
										public String apply(Method input) {
											return input.getName();
										}
									})));
		}
	}
}
