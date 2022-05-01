package org.svenehrke.example.library;

import com.google.auto.service.AutoService;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
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
					writeBuilderFile(out, newModel(className, setterMap));
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return false;
	}

	private void writeBuilderFile(PrintWriter out, Map<String, Object> model) throws IOException {
		Configuration cfg = newFreemarkerConfig();
		try {
			var t = cfg.getTemplate("builder.ftl");
			t.process(model, out);
		} catch (TemplateException e) {
			throw new RuntimeException(e);
		}
	}

	private Map<String, Object> newModel(String className, Map<String, String> setterMap) {
		String packageName = null;
		int lastDot = className.lastIndexOf('.');
		if (lastDot > 0) {
			packageName = className.substring(0, lastDot);
		}

		String simpleClassName = className.substring(lastDot + 1);
		String builderClassName = className + "Builder";
		String builderSimpleClassName = builderClassName.substring(lastDot + 1);

		var model = new HashMap<String, Object>();
		model.put("PN", packageName);
		model.put("CN", builderSimpleClassName);
		model.put("SCN", simpleClassName);
		model.put("SL", setterModel(setterMap));
		return model;
	}

	private List<Map<String, String>> setterModel(Map<String, String> setterMap) {
		return setterMap.entrySet().stream().map(setter -> {
			Map<String, String> map = new HashMap<>();
			map.put("MN", setter.getKey());
			map.put("AT", setter.getValue());
			return map;
		}).toList();
	}

	private Configuration newFreemarkerConfig() {
		Configuration cfg;
		cfg = new Configuration(Configuration.VERSION_2_3_31);
		cfg.setTemplateLoader(new ClassTemplateLoader(getClass(), "/org/svenehrke/example/library"));
		cfg.setDefaultEncoding(StandardCharsets.UTF_8.name());
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		return cfg;
	}
}
