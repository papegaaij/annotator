package nl.topicus.annotator.impl;

public final class Types {
	private Types() {
	}

	public static Object defaultValue(Class<?> type) {
		if (type.equals(Void.TYPE) || !type.isPrimitive())
			return null;
		if (type == Boolean.TYPE)
			return false;
		else if (type == Character.TYPE)
			return '\u0000';
		else if (type == Byte.TYPE)
			return (byte) 0;
		else if (type == Short.TYPE)
			return (short) 0;
		else if (type == Integer.TYPE)
			return 0;
		else if (type == Long.TYPE)
			return 0L;
		else if (type == Float.TYPE)
			return 0.0F;
		else if (type == Double.TYPE)
			return 0.0;
		throw new IllegalArgumentException(type.getName());
	}
}
