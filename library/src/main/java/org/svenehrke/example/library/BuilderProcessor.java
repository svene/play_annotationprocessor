package org.svenehrke.example.library;

import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;

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

			// …
		}
		return false;
	}
}
