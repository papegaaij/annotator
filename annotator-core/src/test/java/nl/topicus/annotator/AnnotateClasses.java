package nl.topicus.annotator;

import java.lang.annotation.ElementType;
import java.lang.reflect.Method;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlSeeAlso;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class AnnotateClasses {
	@XmlRootElement
	public static class TestClass {
		public void testMethod() {
		}
	}

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
		ClassAnnotator<AnnotateClasses> classAnnotator = annotator
				.annotate(AnnotateClasses.class);
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

		TestAnnotation builtAnnotation = AnnotateClasses.class
				.getAnnotation(TestAnnotation.class);
		Assert.assertNotNull(builtAnnotation);
	}

	@Test
	public void classAnnotation() {
		Annotator annotator = new Annotator();
		ClassAnnotator<AnnotateClasses> classAnnotator = annotator
				.annotate(AnnotateClasses.class);
		classAnnotator.addToClass(AnnotationBuilder.of(Deprecated.class));
		annotator.process();
		Assert.assertTrue(AnnotateClasses.class
				.isAnnotationPresent(Deprecated.class));
	}

	@Test
	public void preserveExisting() {
		Annotator annotator = new Annotator();
		ClassAnnotator<TestClass> classAnnotator = annotator
				.annotate(TestClass.class);
		classAnnotator.addToClass(AnnotationBuilder.of(Marker1.class));
		annotator.process();
		Assert.assertTrue("@Marker1 not added",
				TestClass.class.isAnnotationPresent(Marker1.class));
		Assert.assertTrue("@XmlRootElement not preserved",
				TestClass.class.isAnnotationPresent(XmlRootElement.class));
	}

	@Test
	public void addTwoAnnotations() {
		Annotator annotator = new Annotator();
		ClassAnnotator<TestClass> classAnnotator = annotator
				.annotate(TestClass.class);
		classAnnotator.addToClass(AnnotationBuilder.of(Marker2.class));
		classAnnotator.addToClass(AnnotationBuilder.of(Marker3.class));
		annotator.process();
		Assert.assertTrue("@Marker1 not added",
				TestClass.class.isAnnotationPresent(Marker2.class));
		Assert.assertTrue("@Marker2 not added",
				TestClass.class.isAnnotationPresent(Marker3.class));
	}

	@Test(expected = IllegalArgumentException.class)
	public void addExisting() {
		new Annotator().annotate(TestClass.class).addToClass(
				AnnotationBuilder.of(XmlRootElement.class));
	}

	@Test(expected = IllegalArgumentException.class)
	public void addSameTwice() {
		new Annotator().annotate(TestClass.class)
				.addToClass(AnnotationBuilder.of(XmlSeeAlso.class))
				.addToClass(AnnotationBuilder.of(XmlSeeAlso.class));
	}

	@Ignore("Een package annotation op een class, moet dat kunnen of niet? De JVM heeft er geen moeite mee")
	@Test(expected = IllegalArgumentException.class)
	public void addMisplacedAnnotation() {
		Annotator annotator = new Annotator();
		annotator.annotate(TestClass.class).addToClass(
				AnnotationBuilder.of(XmlSchema.class));
		annotator.process();
		Assert.assertFalse(TestClass.class.isAnnotationPresent(XmlSchema.class));
	}
}
