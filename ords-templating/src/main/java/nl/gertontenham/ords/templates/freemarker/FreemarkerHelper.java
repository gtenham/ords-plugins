package nl.gertontenham.ords.templates.freemarker;

import freemarker.template.*;
import nl.gertontenham.ords.templates.config.Configurator;
import oracle.dbtools.plugin.api.di.annotations.Provides;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A generic helper to render Template instances with Freemarker templates.
 *
 */
@Provides
public class FreemarkerHelper {

    private final Configuration cfg;
    private final Configurator configurator;
    private final TemplatingFunctions templatingFunctions;
    private final Logger logger;

    @Inject
    public FreemarkerHelper(Configurator configurator, TemplatingFunctions templatingFunctions, Logger logger) {
        this.configurator = configurator;
        this.logger = logger;
        this.templatingFunctions = templatingFunctions;

        this.cfg = new Configuration(Configuration.VERSION_2_3_25);
        cfg.setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);
        cfg.setDefaultEncoding("UTF8");
        cfg.setURLEscapingCharset("UTF8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

        addSharedVariables();
        setTemplateBaseDirectory();
    }

    public void process(String templatePath, Object dataModel, Writer out) throws IOException, TemplateException {
        //cfg.setDirectoryForTemplateLoading(new File(configurator.getTemplateRootPath()));
        Template template = cfg.getTemplate(templatePath);

        template.process(dataModel, out);
    }

    public Map<String, Object> getRequestData(HttpServletRequest request) {
        final String[] splitContext = request.getContextPath().split("/");
        final String uriPattern = splitContext[splitContext.length-1];

        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("contextPath", request.getContextPath());
        map.put("pathInfo", request.getPathInfo());
        map.put("uriPattern", uriPattern);
        map.put("requestParams", request.getParameterMap());
        map.put("queryString", request.getQueryString());

        return map;
    }

    private void setTemplateBaseDirectory() {
        try {
            cfg.setDirectoryForTemplateLoading(new File(configurator.getTemplateRootPath()));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Invalid template loading base directory.");
        }
    }

    private void addSharedVariables() {
        try {
            cfg.setSharedVariable("tfn", templatingFunctions);
        } catch (TemplateModelException e) {
            logger.log(Level.SEVERE, "Not able to add shared variables to Freemarker processor.");
        }
    }
}
