package nl.topicus.annotator;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public class Annotator {
	private Table<Class<? extends Annotation>, AnnotatedElement, Annotation> annotations = HashBasedTable
			.create();

	public Annotator() {
	}

	public <T> ClassAnnotator<T> annotate(Class<T> clazz) {
		return new ClassAnnotator<>(clazz, this);
	}

	void add(AnnotatedElement e, Annotation a) {
		annotations.put(a.annotationType(), e, a);
	}

	@SuppressWarnings("unchecked")
	public <A extends Annotation> A getAnnotation(AnnotatedElement element,
			Class<A> annotationClass) {
		return (A) annotations.get(annotationClass, element);
	}
}
