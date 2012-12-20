package nl.topicus.annotator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.AnnotationMemberValue;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.BooleanMemberValue;
import javassist.bytecode.annotation.ByteMemberValue;
import javassist.bytecode.annotation.CharMemberValue;
import javassist.bytecode.annotation.ClassMemberValue;
import javassist.bytecode.annotation.DoubleMemberValue;
import javassist.bytecode.annotation.EnumMemberValue;
import javassist.bytecode.annotation.FloatMemberValue;
import javassist.bytecode.annotation.IntegerMemberValue;
import javassist.bytecode.annotation.LongMemberValue;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.ShortMemberValue;
import javassist.bytecode.annotation.StringMemberValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnnotatorClassFileTransformer implements ClassFileTransformer {
	private static final Logger log = LoggerFactory
			.getLogger(AnnotatorClassFileTransformer.class);
	private String annotationName;
	private Map<String, Object> values;

	public AnnotatorClassFileTransformer(String annotationName,
			Map<String, Object> values) {
		this.annotationName = annotationName;
		this.values = values;
	}

	@Override
	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) {
		log.warn("Transforming " + className);
		ClassPool pool = ClassPool.getDefault();
		try {
			CtClass jClass = pool.makeClass(new ByteArrayInputStream(
					classfileBuffer));
			ClassFile classFile = jClass.getClassFile();
			ConstPool cp = classFile.getConstPool();
			AnnotationsAttribute attr = new AnnotationsAttribute(cp,
					AnnotationsAttribute.visibleTag);
			attr.setAnnotation(createAnnotation(jClass.getClassFile()
					.getConstPool(), annotationName, values));
			// TODO moet ook met methods
			classFile.addAttribute(attr);
			byte[] ret = jClass.toBytecode();
			jClass.defrost();
			return ret;
		} catch (IOException | CannotCompileException | NotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	protected Annotation createAnnotation(ConstPool cp, String className,
			Map<String, Object> members) throws NotFoundException {
		CtClass annotationCtClass = ClassPool.getDefault().get(className);
		Annotation ret = new Annotation(cp, annotationCtClass);
		for (Map.Entry<String, Object> curMember : members.entrySet()) {
			setValue(cp, getMemberType(annotationCtClass, curMember.getKey()),
					ret.getMemberValue(curMember.getKey()),
					curMember.getValue());
		}
		return ret;
	}

	private CtClass getMemberType(CtClass annotationCtClass, String memberName)
			throws NotFoundException {
		for (CtMethod curMethod : annotationCtClass.getMethods()) {
			if (curMethod.getName().equals(memberName))
				return curMethod.getReturnType();
		}
		throw new NoSuchElementException(memberName);
	}

	@SuppressWarnings("unchecked")
	private void setValue(ConstPool cp, CtClass type, MemberValue memberValue,
			Object value) throws NotFoundException {
		if (type == CtClass.booleanType) {
			((BooleanMemberValue) memberValue).setValue((Boolean) value);
		} else if (type == CtClass.byteType) {
			((ByteMemberValue) memberValue).setValue((Byte) value);
		} else if (type == CtClass.charType) {
			((CharMemberValue) memberValue).setValue((Character) value);
		} else if (type == CtClass.shortType) {
			((ShortMemberValue) memberValue).setValue((Short) value);
		} else if (type == CtClass.intType) {
			((IntegerMemberValue) memberValue).setValue((Integer) value);
		} else if (type == CtClass.longType) {
			((LongMemberValue) memberValue).setValue((Long) value);
		} else if (type == CtClass.floatType) {
			((FloatMemberValue) memberValue).setValue((Float) value);
		} else if (type == CtClass.doubleType) {
			((DoubleMemberValue) memberValue).setValue((Double) value);
		} else if (type.getName().equals("java.lang.Class")) {
			((ClassMemberValue) memberValue).setValue((String) value);
		} else if (type.getName().equals("java.lang.String")) {
			((StringMemberValue) memberValue).setValue((String) value);
		} else if (type.isArray()) {
			((ArrayMemberValue) memberValue).setValue(createArrayValue(cp,
					type.getComponentType(), (List<Object>) value));
		} else if (type.isInterface()) {
			((AnnotationMemberValue) memberValue).setValue(createAnnotation(cp,
					type.getName(), (Map<String, Object>) value));
		} else {
			((EnumMemberValue) memberValue).setValue((String) value);
		}
	}

	private MemberValue[] createArrayValue(ConstPool cp, CtClass type,
			List<Object> value) throws NotFoundException {
		MemberValue[] ret = new MemberValue[value.size()];
		for (int index = 0; index < value.size(); index++) {
			ret[index] = Annotation.createMemberValue(cp, type);
			setValue(cp, type, ret[index], value.get(index));
		}
		return ret;
	}
}
