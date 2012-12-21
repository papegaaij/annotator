package nl.topicus.annotator;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.ElementType;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

import nl.topicus.annotator.annotations.ClassRetained;
import nl.topicus.annotator.annotations.ComplexAnnotation;
import nl.topicus.annotator.annotations.Marker1;
import nl.topicus.annotator.annotations.Marker2;
import nl.topicus.annotator.annotations.Marker3;
import nl.topicus.annotator.annotations.NestedAnnotation;
import nl.topicus.annotator.annotations.SourceRetained;
import nl.topicus.annotator.annotations.Unretained;

import org.junit.Test;

public class AnnotateClasses {
	@XmlRootElement
	@XmlSeeAlso({ String.class })
	public static class TestClass {
	}

	@Test
	public void complexAnnotation() {
		Annotator annotator = new Annotator();
		ClassAnnotator<TestClass> classAnnotator = annotator
				.annotate(TestClass.class);
		classAnnotator.addToClass(new AnnotationBuilder<ComplexAnnotation>(
				ComplexAnnotation.class) {
			@Override
			public void setup(ComplexAnnotation ann) {
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

		assertEquals(annotator.getAnnotation(TestClass.class,
				ComplexAnnotation.class),
				TestClass.class.getAnnotation(ComplexAnnotation.class));
	}

	@Test
	public void classAnnotation() {
		Annotator annotator = new Annotator();
		ClassAnnotator<TestClass> classAnnotator = annotator
				.annotate(TestClass.class);
		classAnnotator.addToClass(AnnotationBuilder.of(Deprecated.class));
		annotator.process();
		assertTrue(TestClass.class.isAnnotationPresent(Deprecated.class));
	}

	@Test
	public void preserveExisting() {
		assertFalse("@Marker1 was already added",
				TestClass.class.isAnnotationPresent(Marker1.class));

		Annotator annotator = new Annotator();
		ClassAnnotator<TestClass> classAnnotator = annotator
				.annotate(TestClass.class);
		classAnnotator.addToClass(AnnotationBuilder.of(Marker1.class));
		annotator.process();

		assertTrue("@Marker1 not added",
				TestClass.class.isAnnotationPresent(Marker1.class));
		assertTrue("@XmlRootElement not preserved",
				TestClass.class.isAnnotationPresent(XmlRootElement.class));
	}

	@Test
	public void addTwoAnnotations() {
		assertFalse("@Marker2 was already added",
				TestClass.class.isAnnotationPresent(Marker2.class));
		assertFalse("@Marker3 was already added",
				TestClass.class.isAnnotationPresent(Marker3.class));

		Annotator annotator = new Annotator();
		ClassAnnotator<TestClass> classAnnotator = annotator
				.annotate(TestClass.class);
		classAnnotator.addToClass(AnnotationBuilder.of(Marker2.class));
		classAnnotator.addToClass(AnnotationBuilder.of(Marker3.class));
		annotator.process();

		assertTrue("@Marker2 not added",
				TestClass.class.isAnnotationPresent(Marker2.class));
		assertTrue("@Marker3 not added",
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
				.addToClass(AnnotationBuilder.of(XmlType.class))
				.addToClass(AnnotationBuilder.of(XmlType.class));
	}

	@Test
	public void overrideNew() {
		Annotator annotator = new Annotator();
		ClassAnnotator<TestClass> classAnnotator = annotator
				.annotate(TestClass.class);
		classAnnotator
				.addToClass(new AnnotationBuilder<XmlType>(XmlType.class) {
					@Override
					public void setup(XmlType ann) {
						set(ann.name(), "nondefault1");
					}
				});
		classAnnotator
				.setOnClass(new AnnotationBuilder<XmlType>(XmlType.class) {
					@Override
					public void setup(XmlType ann) {
						set(ann.name(), "nondefault2");
					}
				});
		annotator.process();

		assertEquals("nondefault2", TestClass.class
				.getAnnotation(XmlType.class).name());
	}

	@Test
	public void overrideExisting() {
		Annotator annotator = new Annotator();
		ClassAnnotator<TestClass> classAnnotator = annotator
				.annotate(TestClass.class);
		classAnnotator.setOnClass(new AnnotationBuilder<XmlSeeAlso>(
				XmlSeeAlso.class) {
			@Override
			public void setup(XmlSeeAlso ann) {
				set(ann.value(), new Class[] { Integer.class, Long.class });
			}
		});
		annotator.process();

		assertArrayEquals(new Class[] { Integer.class, Long.class },
				TestClass.class.getAnnotation(XmlSeeAlso.class).value());
	}

	@Test(expected = IllegalArgumentException.class)
	public void addMisplacedAnnotation() {
		new Annotator().annotate(TestClass.class).addToClass(
				AnnotationBuilder.of(XmlSchema.class));
	}

	@Test(expected = IllegalArgumentException.class)
	public void addSourceAnnotation() {
		new Annotator().annotate(TestClass.class).addToClass(
				AnnotationBuilder.of(SourceRetained.class));
	}

	@Test(expected = IllegalArgumentException.class)
	public void addClassAnnotation() {
		new Annotator().annotate(TestClass.class).addToClass(
				AnnotationBuilder.of(ClassRetained.class));
	}

	@Test(expected = IllegalArgumentException.class)
	public void addUnretainedAnnotation() {
		new Annotator().annotate(TestClass.class).addToClass(
				AnnotationBuilder.of(Unretained.class));
	}
}
