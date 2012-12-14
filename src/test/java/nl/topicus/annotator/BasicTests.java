package nl.topicus.annotator;

import org.junit.Test;

public class BasicTests {
	@Test
	public void test() {
		Annotator annotator = new Annotator();
		ClassAnnotator<BasicTests> classAnnotator = annotator
				.annotate(BasicTests.class);
		classAnnotator.addTo(new AnnotationBuilder<Test>(Test.class) {
			@Override
			public void build(Test ann) {
				set(ann.timeout(), 1000L);
				set(ann.expected(), RuntimeException.class);
			}
		}).test();
	}
}
