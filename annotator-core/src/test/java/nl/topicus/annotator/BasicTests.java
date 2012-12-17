package nl.topicus.annotator;

import java.lang.reflect.Method;

import nl.topicus.annotator.agent.AnnotatorAgent;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class BasicTests {
	public static final Method m(Class<?> declaringClass, String name,
			Class<?>... types) {
		try {
			return declaringClass.getMethod(name, types);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}
	
	@BeforeClass
	public static void setup() {
		AnnotatorAgent.loadDynamicAgent();
	}

	@Test
	public void test() {
		Annotator annotator = new Annotator();
		ClassAnnotator<BasicTests> classAnnotator = annotator
				.annotate(BasicTests.class);
		classAnnotator.addToMethod(new AnnotationBuilder<Test>(Test.class) {
			@Override
			public void setup(Test ann) {
				set(ann.timeout(), 1000L);
				set(ann.expected(), RuntimeException.class);
			}
		}).test();

		Method testMethod = m(BasicTests.class, "test");
		Assert.assertEquals(1000L,
				annotator.getAnnotation(testMethod, Test.class).timeout());
		Assert.assertEquals(RuntimeException.class,
				annotator.getAnnotation(testMethod, Test.class).expected());
	}
}
