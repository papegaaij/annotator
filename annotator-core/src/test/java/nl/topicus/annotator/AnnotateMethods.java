package nl.topicus.annotator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.reflect.Method;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchema;

import nl.topicus.annotator.annotations.ClassRetained;
import nl.topicus.annotator.annotations.ComplexAnnotation;
import nl.topicus.annotator.annotations.Marker1;
import nl.topicus.annotator.annotations.Marker2;
import nl.topicus.annotator.annotations.Marker3;
import nl.topicus.annotator.annotations.NestedAnnotation;
import nl.topicus.annotator.annotations.SourceRetained;
import nl.topicus.annotator.annotations.Unretained;

import org.junit.Test;

public class AnnotateMethods {
	public static final Method m(Class<?> declaringClass, String name,
			Class<?>... types) {
		try {
			return declaringClass.getMethod(name, types);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}

	private static Method staticMethod() {
		return m(TestClass.class, "staticMethod");
	}

	private static Method noArgMethod() {
		return m(TestClass.class, "noArgMethod");
	}

	private static Method oneArgMethod() {
		return m(TestClass.class, "oneArgMethod", String.class);
	}

	private static Method twoArgMethod() {
		return m(TestClass.class, "twoArgMethod", String.class, String.class);
	}

	private static Method overloadedMethodStr() {
		return m(TestClass.class, "overloadedMethod", String.class);
	}

	private static Method overloadedMethodInt() {
		return m(TestClass.class, "overloadedMethod", Integer.class);
	}

	private static Method annotatedMethod() {
		return m(TestClass.class, "annotatedMethod");
	}

	private static Method abstractMethod() {
		return m(AbstractTestClass.class, "abstractMethod");
	}

	private static Method overriddenMethod() {
		return m(SubTestClass.class, "noArgMethod");
	}

	public static class TestClass {
		public static void staticMethod() {
		}

		public void noArgMethod() {
		}

		public void oneArgMethod(String arg) {
		}

		public void twoArgMethod(String arg1, String arg2) {
		}

		public void overloadedMethod(String str) {
		}

		public void overloadedMethod(Integer i) {
		}

		@XmlElement
		public void annotatedMethod() {
		}
	}

	public static abstract class AbstractTestClass {
		public abstract void abstractMethod();
	}

	public static class SubTestClass extends TestClass {
		@Override
		public void noArgMethod() {
		}
	}

	@Test
	public void addSimpleAnnotation() {
		assertFalse(noArgMethod().isAnnotationPresent(Marker1.class));

		Annotator annotator = new Annotator();
		ClassAnnotator<TestClass> classAnnotator = annotator
				.annotate(TestClass.class);
		classAnnotator.addToMethod(AnnotationBuilder.of(Marker1.class))
				.noArgMethod();
		annotator.process();

		assertTrue(noArgMethod().isAnnotationPresent(Marker1.class));
	}

	@Test
	public void addComplexAnnotation() {
		assertFalse(noArgMethod().isAnnotationPresent(ComplexAnnotation.class));

		Annotator annotator = new Annotator();
		ClassAnnotator<TestClass> classAnnotator = annotator
				.annotate(TestClass.class);
		classAnnotator.addToMethod(
				new AnnotationBuilder<ComplexAnnotation>(
						ComplexAnnotation.class) {
					@Override
					public void setup(ComplexAnnotation ann) {
						set(ann.enumValue(), ElementType.ANNOTATION_TYPE);
						set(ann.longs(), new long[] { 5, 6, 7 });
						set(ann.nested(),
								new AnnotationBuilder<NestedAnnotation>() {
									@Override
									public void setup(NestedAnnotation ann) {
										set(ann.value(), "blaat");
									}
								}.build());
					}
				}).noArgMethod();
		annotator.process();

		assertEquals(
				annotator.getAnnotation(noArgMethod(), ComplexAnnotation.class),
				noArgMethod().getAnnotation(ComplexAnnotation.class));
	}

	@Test
	public void addAnnotationToOneArgMethod() {
		assertFalse(oneArgMethod().isAnnotationPresent(Marker1.class));

		Annotator annotator = new Annotator();
		ClassAnnotator<TestClass> classAnnotator = annotator
				.annotate(TestClass.class);
		classAnnotator.addToMethod(AnnotationBuilder.of(Marker1.class))
				.oneArgMethod(null);
		annotator.process();

		assertTrue(oneArgMethod().isAnnotationPresent(Marker1.class));
	}

	@Test
	public void addAnnotationToTwoArgMethod() {
		assertFalse(twoArgMethod().isAnnotationPresent(Marker1.class));

		Annotator annotator = new Annotator();
		ClassAnnotator<TestClass> classAnnotator = annotator
				.annotate(TestClass.class);
		classAnnotator.addToMethod(AnnotationBuilder.of(Marker1.class))
				.twoArgMethod(null, null);
		annotator.process();

		assertTrue(twoArgMethod().isAnnotationPresent(Marker1.class));
	}

	@Test
	public void addAnnotationToOverloadedMethod() {
		assertFalse(overloadedMethodStr().isAnnotationPresent(Marker1.class));
		assertFalse(overloadedMethodInt().isAnnotationPresent(Marker1.class));
		assertFalse(overloadedMethodStr().isAnnotationPresent(Marker2.class));
		assertFalse(overloadedMethodInt().isAnnotationPresent(Marker2.class));

		Annotator annotator = new Annotator();
		ClassAnnotator<TestClass> classAnnotator = annotator
				.annotate(TestClass.class);
		classAnnotator.addToMethod(AnnotationBuilder.of(Marker1.class))
				.overloadedMethod("str");
		classAnnotator.addToMethod(AnnotationBuilder.of(Marker2.class))
				.overloadedMethod(5);
		annotator.process();

		assertTrue(overloadedMethodStr().isAnnotationPresent(Marker1.class));
		assertFalse(overloadedMethodInt().isAnnotationPresent(Marker1.class));
		assertFalse(overloadedMethodStr().isAnnotationPresent(Marker2.class));
		assertTrue(overloadedMethodInt().isAnnotationPresent(Marker2.class));
	}

	@Test
	public void addAnnotationToAbstractMethod() {
		assertFalse(abstractMethod().isAnnotationPresent(Marker1.class));

		Annotator annotator = new Annotator();
		ClassAnnotator<AbstractTestClass> classAnnotator = annotator
				.annotate(AbstractTestClass.class);
		classAnnotator.addToMethod(AnnotationBuilder.of(Marker1.class))
				.abstractMethod();
		annotator.process();

		assertTrue(abstractMethod().isAnnotationPresent(Marker1.class));
	}

	@Test
	public void addAnnotationToOverriddenMethod() {
		assertFalse(noArgMethod().isAnnotationPresent(Marker2.class));
		assertFalse(overriddenMethod().isAnnotationPresent(Marker2.class));
		assertFalse(noArgMethod().isAnnotationPresent(Marker3.class));
		assertFalse(overriddenMethod().isAnnotationPresent(Marker3.class));

		Annotator annotator = new Annotator();
		annotator.annotate(TestClass.class)
				.addToMethod(AnnotationBuilder.of(Marker2.class)).noArgMethod();
		annotator.annotate(SubTestClass.class)
				.addToMethod(AnnotationBuilder.of(Marker3.class)).noArgMethod();
		annotator.process();

		assertTrue(noArgMethod().isAnnotationPresent(Marker2.class));
		assertFalse(overriddenMethod().isAnnotationPresent(Marker2.class));
		assertFalse(noArgMethod().isAnnotationPresent(Marker3.class));
		assertTrue(overriddenMethod().isAnnotationPresent(Marker3.class));
	}

	@Test
	public void addAnnotationToStaticMethod() {
		assertFalse(staticMethod().isAnnotationPresent(Marker1.class));

		Annotator annotator = new Annotator();
		annotator.add(staticMethod(), AnnotationBuilder.of(Marker1.class));
		annotator.process();

		assertTrue(staticMethod().isAnnotationPresent(Marker1.class));
	}

	@Test
	public void preserveExisting() {
		assertFalse("@Marker1 was already added", annotatedMethod()
				.isAnnotationPresent(Marker1.class));

		Annotator annotator = new Annotator();
		ClassAnnotator<TestClass> classAnnotator = annotator
				.annotate(TestClass.class);
		classAnnotator.addToMethod(AnnotationBuilder.of(Marker1.class))
				.annotatedMethod();
		annotator.process();

		assertTrue("@Marker1 not added",
				annotatedMethod().isAnnotationPresent(Marker1.class));
		assertTrue("@XmlElement not preserved", annotatedMethod()
				.isAnnotationPresent(XmlElement.class));
	}

	@Test(expected = IllegalArgumentException.class)
	public void addExisting() {
		new Annotator().annotate(TestClass.class)
				.addToMethod(AnnotationBuilder.of(XmlElement.class))
				.annotatedMethod();
	}

	@Test(expected = IllegalArgumentException.class)
	public void addSameTwice() {
		ClassAnnotator<TestClass> classAnnotator = new Annotator()
				.annotate(TestClass.class);
		classAnnotator.addToMethod(AnnotationBuilder.of(Marker2.class))
				.annotatedMethod();
		classAnnotator.addToMethod(AnnotationBuilder.of(Marker2.class))
				.annotatedMethod();
	}

	@Test(expected = IllegalArgumentException.class)
	public void addMisplacedAnnotation() {
		new Annotator().annotate(TestClass.class)
				.addToMethod(AnnotationBuilder.of(XmlSchema.class))
				.annotatedMethod();
	}

	@Test(expected = IllegalArgumentException.class)
	public void addSourceAnnotation() {
		new Annotator().annotate(TestClass.class)
				.addToMethod(AnnotationBuilder.of(SourceRetained.class))
				.annotatedMethod();
	}

	@Test(expected = IllegalArgumentException.class)
	public void addClassAnnotation() {
		new Annotator().annotate(TestClass.class)
				.addToMethod(AnnotationBuilder.of(ClassRetained.class))
				.annotatedMethod();
	}

	@Test(expected = IllegalArgumentException.class)
	public void addUnretainedAnnotation() {
		new Annotator().annotate(TestClass.class)
				.addToMethod(AnnotationBuilder.of(Unretained.class))
				.annotatedMethod();
	}
}
