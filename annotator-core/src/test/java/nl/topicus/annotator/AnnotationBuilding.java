package nl.topicus.annotator;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.ElementType;
import java.util.Arrays;
import java.util.Map;

import nl.topicus.annotator.annotations.AnnotationWithDefault;
import nl.topicus.annotator.annotations.Marker1;
import nl.topicus.annotator.annotations.NestedAnnotation;
import nl.topicus.annotator.annotations.ComplexAnnotation;

import org.junit.Test;

public class AnnotationBuilding {
	@Test
	public void simpleAnnotation() {
		AnnotationBuilder<Marker1> builder = AnnotationBuilder
				.of(Marker1.class);
		Marker1 dummy = builder.build();
		assertTrue(builder.values().isEmpty());
		assertNotNull(dummy);
		assertEquals(Marker1.class, dummy.annotationType());
	}

	@Test
	public void complexAnnotation() {
		AnnotationBuilder<ComplexAnnotation> builder = new AnnotationBuilder<ComplexAnnotation>(
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
		};
		Map<String, Object> values = builder.values();
		assertEquals(3, values.size());
		assertEquals(ElementType.ANNOTATION_TYPE.name(),
				values.get("enumValue"));
		assertEquals(Arrays.asList(5L, 6L, 7L), values.get("longs"));
		@SuppressWarnings("unchecked")
		Map<String, Object> nested = (Map<String, Object>) values.get("nested");
		assertEquals("blaat", nested.get("value"));

		ComplexAnnotation dummy = builder.build();
		assertNotNull(dummy);
		assertEquals(ComplexAnnotation.class, dummy.annotationType());
		assertEquals(ElementType.ANNOTATION_TYPE, dummy.enumValue());
		assertArrayEquals(new long[] { 5, 6, 7 }, dummy.longs());
		assertEquals(NestedAnnotation.class, dummy.nested().annotationType());
		assertEquals("blaat", dummy.nested().value());
	}

	@Test
	public void defaultValues() {
		AnnotationBuilder<AnnotationWithDefault> builder = new AnnotationBuilder<AnnotationWithDefault>(
				AnnotationWithDefault.class) {
			@Override
			public void setup(AnnotationWithDefault ann) {
				set(ann.noDefault(), "setValue");
			}
		};
		Map<String, Object> values = builder.values();
		assertEquals(2, values.size());
		assertEquals(String.class.getName(), values.get("hasDefault"));
		assertEquals("setValue", values.get("noDefault"));

		AnnotationWithDefault dummy = builder.build();
		assertNotNull(dummy);
		assertEquals(AnnotationWithDefault.class, dummy.annotationType());
		assertEquals(String.class, dummy.hasDefault());
		assertEquals("setValue", dummy.noDefault());
	}

	@Test(expected = IllegalStateException.class)
	public void incompleteSimple() {
		AnnotationBuilder.of(AnnotationWithDefault.class);
	}

	@Test(expected = NullPointerException.class)
	public void defaultInvalidValue() {
		new AnnotationBuilder<AnnotationWithDefault>(
				AnnotationWithDefault.class) {
			@Override
			public void setup(AnnotationWithDefault ann) {
				set(ann.noDefault(), null);
			}
		};
	}
	
	
}
