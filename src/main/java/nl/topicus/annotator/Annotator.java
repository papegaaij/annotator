package nl.topicus.annotator;

public class Annotator {
	public Annotator() {
	}

	public <T> ClassAnnotator<T> annotate(Class<T> clazz) {
		return new ClassAnnotator<>(clazz);
	}
}
