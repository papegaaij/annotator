package nl.topicus.annotator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.Proxy;
import javassist.util.proxy.ProxyFactory;

public class ClassAnnotator<T> {
	private T classProxy;

	public ClassAnnotator(Class<T> clazz) {
		classProxy = createProxy(clazz);
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

	public <A extends Annotation> T addTo(AnnotationBuilder<A> builder) {
		builder.build(builder.getAnn());
		((Proxy) classProxy).setHandler(new AnnotationCollectionHandler<>(builder));
		return classProxy;
	}
}
