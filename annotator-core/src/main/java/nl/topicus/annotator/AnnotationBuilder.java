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
import javassist.util.proxy.Proxy;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;

import javax.enterprise.util.AnnotationLiteral;

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
	private Map<Method, Object> values = new HashMap<>();
	private Class<A> annotationClass;

	public AnnotationBuilder(Class<A> annotationClass) {
		this.annotationClass = annotationClass;
		this.ann = createAnnotationProxy(annotationClass);
		for (Method curProperty : annotationClass.getDeclaredMethods()) {
			if (curProperty.getDefaultValue() != null)
				values.put(curProperty, curProperty.getDefaultValue());
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

	public <T, V extends T> AnnotationBuilder<A> set(
			Class<? extends T> annotationProperty, Class<V> value) {
		if (value == null)
			throw new NullPointerException("value cannot be null");
		values.put(selectedProperty, value);
		return this;
	}

	public Class<A> annotationType() {
		return annotationClass;
	}

	public <T, V extends T> AnnotationBuilder<A> set(T annotationProperty,
			V value) {
		if (value == null)
			throw new NullPointerException("value cannot be null");
		values.put(selectedProperty, value);
		return this;
	}

	public A build() {
		ProxyFactory factory = new ProxyFactory();
		factory.setFilter(new MethodFilter() {
			public boolean isHandled(Method m) {
				return values.containsKey(m)
						|| m.equals(ANNOTATION_TYPE_METHOD);
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
					return values.get(m);
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
		Map<String, Object> ret = new TreeMap<>();
		for (Map.Entry<Method, Object> curValue : values.entrySet()) {
			ret.put(curValue.getKey().getName(),
					convertValue(curValue.getValue()));
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
}
