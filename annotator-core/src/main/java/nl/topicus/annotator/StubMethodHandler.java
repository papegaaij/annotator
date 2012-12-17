package nl.topicus.annotator;

import java.lang.reflect.Method;

import javassist.util.proxy.MethodHandler;

public abstract class StubMethodHandler implements MethodHandler {

	protected Object createReturnValue(Method method) {
		if (method.getReturnType().equals(Void.TYPE)
				|| !method.getReturnType().isPrimitive())
			return null;
		if (method.getReturnType() == Boolean.TYPE)
			return false;
		else if (method.getReturnType() == Character.TYPE)
			return '\u0000';
		else if (method.getReturnType() == Byte.TYPE)
			return (byte) 0;
		else if (method.getReturnType() == Short.TYPE)
			return (short) 0;
		else if (method.getReturnType() == Integer.TYPE)
			return 0;
		else if (method.getReturnType() == Long.TYPE)
			return 0L;
		else if (method.getReturnType() == Float.TYPE)
			return 0.0F;
		else if (method.getReturnType() == Double.TYPE)
			return 0.0;

		throw new IllegalArgumentException(method.toGenericString());
	}
}
