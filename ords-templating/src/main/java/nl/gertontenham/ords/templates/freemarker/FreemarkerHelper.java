package nl.gertontenham.ords.templates.freemarker;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import nl.gertontenham.ords.templates.config.Configurator;
import oracle.dbtools.plugin.api.di.annotations.Provides;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.Writer;

/**
 * A generic helper to render Template instances with Freemarker templates.
 *
 */
@Provides
public class FreemarkerHelper {

    private final Configuration cfg;
    private final Configurator configurator;

    @Inject
    public FreemarkerHelper(Configurator configurator) {
        this.configurator = configurator;

        this.cfg = new Configuration(Configuration.VERSION_2_3_25);
        cfg.setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);
        cfg.setDefaultEncoding("UTF8");
        cfg.setURLEscapingCharset("UTF8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

    }

    public void process(String templatePath, Object dataModel, Writer out) throws IOException, TemplateException {
        cfg.setDirectoryForTemplateLoading(new File(configurator.getTemplateRootPath()));
        Template template = cfg.getTemplate(templatePath);

        template.process(dataModel, out);
    }
}
