package nl.topicus.annotator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.Proxy;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;

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
	
	public <A extends Annotation> void addToClass(AnnotationBuilder<A> builder) {
		annotator.add(classToAnnotate, builder);
	}

	public <A extends Annotation> T addToMethod(AnnotationBuilder<A> builder) {
		((ProxyObject) classProxy).setHandler(new AnnotationCollectionHandler<>(
				annotator, builder));
		return classProxy;
	}
}
