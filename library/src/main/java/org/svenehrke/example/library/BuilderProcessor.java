package org.svenehrke.example.library;

import com.google.auto.service.AutoService;
import org.stringtemplate.v4.ST;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedAnnotationTypes("org.svenehrke.example.library.BuilderProperty")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
public class BuilderProcessor extends AbstractProcessor {
	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		for (TypeElement annotation : annotations) {
			System.out.printf("found annotation: '%s'%n", annotation.getSimpleName());
			Set<? extends Element> annotatedElements
				= roundEnv.getElementsAnnotatedWith(annotation);
			annotatedElements.forEach((it) -> System.out.printf("element '%s'%n", it.getSimpleName()));

			Map<Boolean, List<Element>> annotatedMethods = annotatedElements.stream().collect(
				Collectors.partitioningBy(element ->
					((ExecutableType) element.asType()).getParameterTypes().size() == 1
						&& element.getSimpleName().toString().startsWith("set")));

			List<Element> setters = annotatedMethods.get(true);
			List<Element> otherMethods = annotatedMethods.get(false);

			otherMethods.forEach(element ->
				processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
					"@BuilderProperty must be applied to a setXxx method "
						+ "with a single argument", element));

			if (setters.isEmpty()) {
				continue;
			}
			String className = ((TypeElement) setters.get(0).getEnclosingElement()).getQualifiedName().toString();
			Map<String, String> setterMap = setters.stream().collect(Collectors.toMap(
				setter -> setter.getSimpleName().toString(),
				setter -> ((ExecutableType) setter.asType())
					.getParameterTypes().get(0).toString()
			));
			JavaFileObject builderFile = null;
			try {
				builderFile = processingEnv.getFiler().createSourceFile(className + "Builder");
				try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
					writeBuilderFile(out, className, setterMap);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return false;
	}

	private void writeBuilderFile(
		PrintWriter out,
		String className,
		Map<String, String> setterMap
	)
		throws IOException {

		String packageName = null;
		int lastDot = className.lastIndexOf('.');
		if (lastDot > 0) {
			packageName = className.substring(0, lastDot);
		}

		String simpleClassName = className.substring(lastDot + 1);
		String builderClassName = className + "Builder";
		String builderSimpleClassName = builderClassName.substring(lastDot + 1);
		List<String> setterList = setterList(setterMap, builderSimpleClassName);

		var st = new ST("""
			<if(PN)>package <PN>;

			<endif>
			public class <CN> {
			 private <SCN> object = new <SCN>();

			 public <SCN> build() {
			 	return object;
			 }

			 <setters:{x | <x>}; separator={\n}>
			}
			""");
		if (packageName != null) {
			st.add("PN", packageName);
		}
		st.add("CN", builderSimpleClassName);
		st.add("SCN", simpleClassName);
		st.add("setters", setterList);
		out.print(st.render());
	}

	private List<String> setterList(Map<String, String> setterMap, String builderSimpleClassName) {
		return setterMap.entrySet().stream().map(setter -> {
			String methodName = setter.getKey();
			String argumentType = setter.getValue();

			ST setterST = new ST("""
				  public <CN> <MN>(<AT> value) {
				    object.<MN>(value);
				    return this;
				  }
				""");
			setterST.add("CN", builderSimpleClassName);
			setterST.add("MN", methodName);
			setterST.add("AT", argumentType);
			return setterST.render();
		}).toList();
	}
}
