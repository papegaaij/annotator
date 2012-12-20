package nl.topicus.annotator;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import nl.topicus.annotator.agent.AnnotatorAgent;
import nl.topicus.annotator.impl.AnnotationMutator;
import nl.topicus.annotator.impl.AnnotatorClassFileTransformer;
import nl.topicus.annotator.impl.ClassAnnotationCreator;
import nl.topicus.annotator.impl.MethodAnnotationCreator;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class Annotator {
	private static class AnnotationContainer<A extends Annotation> {
		private AnnotatedElement element;
		private AnnotationBuilder<A> builder;

		private AnnotationContainer(AnnotatedElement element,
				AnnotationBuilder<A> builder) {
			this.element = element;
			this.builder = builder;
		}

		public Class<?> getDeclaringClass() {
			return declaringClass(element);
		}

		public Class<A> getAnnotationType() {
			return builder.annotationType();
		}

		public AnnotatedElement getElement() {
			return element;
		}

		public AnnotationBuilder<A> getBuilder() {
			return builder;
		}

		public AnnotationMutator getMutator() {
			String name = builder.annotationType().getName();
			Map<String, Object> values = builder.values();
			if (element instanceof Class) {
				return new ClassAnnotationCreator(name, values);
			} else if (element instanceof Method) {
				return new MethodAnnotationCreator((Method) element, name,
						values);
			}
			throw new IllegalStateException(element.getClass().getName());
		}
	}

	private static Class<?> declaringClass(AnnotatedElement element) {
		if (element instanceof Member)
			return ((Member) element).getDeclaringClass();
		return (Class<?>) element;
	}

	private Multimap<Class<?>, AnnotationContainer<?>> annotations = HashMultimap
			.create();

	public Annotator() {
		AnnotatorAgent.loadDynamicAgent();
	}

	public <T> ClassAnnotator<T> annotate(Class<T> clazz) {
		return new ClassAnnotator<>(clazz, this);
	}

	public <A extends Annotation> void add(AnnotatedElement e,
			AnnotationBuilder<A> builder) {
		AnnotationContainer<A> container = new AnnotationContainer<A>(e,
				builder);
		annotations.put(container.getDeclaringClass(), container);
	}

	@SuppressWarnings("unchecked")
	private <A extends Annotation> AnnotationContainer<A> getContainer(
			AnnotatedElement element, Class<A> annotationClass) {
		for (AnnotationContainer<?> curContainer : annotations
				.get(declaringClass(element))) {
			if (curContainer.getElement().equals(element)
					&& curContainer.getAnnotationType().equals(annotationClass)) {
				return (AnnotationContainer<A>) curContainer;
			}
		}
		return null;
	}

	public boolean isAnnotationPresent(AnnotatedElement element,
			Class<? extends Annotation> annotationClass) {
		return getContainer(element, annotationClass) != null;
	}

	public <A extends Annotation> A getAnnotation(AnnotatedElement element,
			Class<A> annotationClass) {
		AnnotationContainer<A> container = getContainer(element,
				annotationClass);
		return container == null ? null : container.getBuilder().build();
	}

	public void process() {
		for (Map.Entry<Class<?>, Collection<AnnotationContainer<?>>> curElement : annotations
				.asMap().entrySet()) {
			List<AnnotationMutator> mutators = new ArrayList<>();
			for (AnnotationContainer<?> curContainer : curElement.getValue()) {
				mutators.add(curContainer.getMutator());
			}
			AnnotatorAgent.addAnnotations((Class<?>) curElement.getKey(),
					new AnnotatorClassFileTransformer(mutators));
		}
	}
}
