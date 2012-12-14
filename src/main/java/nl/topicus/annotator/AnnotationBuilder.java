package nl.topicus.annotator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.util.AnnotationLiteral;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.Proxy;
import javassist.util.proxy.ProxyFactory;

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

	public abstract void setup(A ann);

	public <T, V extends T> AnnotationBuilder<A> set(
			Class<? extends T> annotationProperty, Class<V> value) {
		values.put(selectedProperty, value);
		return this;
	}

	public <T, V extends T> AnnotationBuilder<A> set(T annotationProperty,
			V value) {
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
			((Proxy) ret).setHandler(new StubMethodHandler() {
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
			((Proxy) ret).setHandler(new StubMethodHandler() {
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
}
