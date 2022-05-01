package org.svenehrke.example.library;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class MyLibTest {
	@Test
	void ftl() throws IOException {
		Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
		cfg.setTemplateLoader(new ClassTemplateLoader(getClass(), "/org/svenehrke/example/library"));
		cfg.setDefaultEncoding(StandardCharsets.UTF_8.name());
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

		var t = cfg.getTemplate("test1.ftl");
		var model = new HashMap<String, Object>();
		model.put("PN", "org.svenehrke.example.play");
		try(StringWriter sw = new StringWriter()) {
			t.process(model, sw);
			assertThat(sw.toString()).isEqualTo("""
				package org.svenehrke.example.play;

				public class Doit {
				}
				""");
		} catch (TemplateException e) {
			throw new RuntimeException(e);
		}
	}

}
