package nl.topicus.annotator.impl;

import java.lang.reflect.Method;

import javassist.util.proxy.MethodHandler;

public abstract class StubMethodHandler implements MethodHandler {
	protected Object createReturnValue(Method method) {
		return Types.defaultValue(method.getReturnType());
	}
}
