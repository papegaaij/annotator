package nl.topicus.annotator;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Map;

import nl.topicus.annotator.agent.AnnotatorAgent;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public class Annotator {
	private Table<Class<? extends Annotation>, AnnotatedElement, AnnotationBuilder<? extends Annotation>> annotations = HashBasedTable
			.create();

	public Annotator() {
		AnnotatorAgent.loadDynamicAgent();
	}

	public <T> ClassAnnotator<T> annotate(Class<T> clazz) {
		return new ClassAnnotator<>(clazz, this);
	}

	void add(AnnotatedElement e, AnnotationBuilder<? extends Annotation> builder) {
		annotations.put(builder.annotationType(), e, builder);
	}

	@SuppressWarnings("unchecked")
	public <A extends Annotation> A getAnnotation(AnnotatedElement element,
			Class<A> annotationClass) {
		return (A) annotations.get(annotationClass, element).build();
	}

	public void process() {
		for (AnnotatedElement curElement : annotations.columnKeySet()) {
			if (curElement instanceof Class<?>) {
				Map<Class<? extends Annotation>, AnnotationBuilder<? extends Annotation>> annotationsOnClass = annotations
						.column(curElement);
				for (Map.Entry<Class<? extends Annotation>, AnnotationBuilder<? extends Annotation>> curEntry : annotationsOnClass
						.entrySet()) {
					AnnotatorAgent.addAnnotations((Class<?>) curElement,
							new AnnotatorClassFileTransformer(curEntry.getKey()
									.getName(), curEntry.getValue().values()));
				}
			}
		}
	}
}
