package nl.topicus.annotator;

import java.lang.annotation.ElementType;
import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicTests {
	private static final Logger log = LoggerFactory.getLogger(BasicTests.class);

	public static final Method m(Class<?> declaringClass, String name,
			Class<?>... types) {
		try {
			return declaringClass.getMethod(name, types);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void complexAnnotation() {
		Annotator annotator = new Annotator();
		ClassAnnotator<BasicTests> classAnnotator = annotator
				.annotate(BasicTests.class);
		classAnnotator.addToClass(new AnnotationBuilder<TestAnnotation>(
				TestAnnotation.class) {
			@Override
			public void setup(TestAnnotation ann) {
				set(ann.enumValue(), ElementType.ANNOTATION_TYPE);
				set(ann.longs(), new long[] { 5, 6, 7 });
				set(ann.nested(), new AnnotationBuilder<NestedAnnotation>(
						NestedAnnotation.class) {
					@Override
					public void setup(NestedAnnotation ann) {
						set(ann.value(), "blaat");
					}
				}.build());
			}
		});
		annotator.process();

		TestAnnotation builtAnnotation = BasicTests.class
				.getAnnotation(TestAnnotation.class);
		Assert.assertNotNull(builtAnnotation);
		log.info(builtAnnotation.toString());
	}

	@Test
	public void classAnnotation() {
		Annotator annotator = new Annotator();
		ClassAnnotator<BasicTests> classAnnotator = annotator
				.annotate(BasicTests.class);
		classAnnotator.addToClass(AnnotationBuilder.of(Deprecated.class));
		annotator.process();
		Assert.assertTrue(BasicTests.class
				.isAnnotationPresent(Deprecated.class));
	}
}
