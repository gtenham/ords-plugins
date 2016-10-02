package nl.gertontenham.ords.templates;

import freemarker.template.TemplateException;
import nl.gertontenham.ords.templates.freemarker.FreemarkerHelper;
import oracle.dbtools.plugin.api.di.annotations.Provides;
import oracle.dbtools.plugin.api.http.annotations.Dispatches;
import oracle.dbtools.plugin.api.http.annotations.PathTemplate;
import oracle.dbtools.plugin.api.routes.PathTemplateMatch;
import oracle.dbtools.plugin.api.routes.PathTemplates;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ORDS Templating servlet endpoint
 */
@Provides
@Dispatches(@PathTemplate("/templates/:template-path*"))
public class TemplatingPlugin extends HttpServlet {

    private final FreemarkerHelper fmHelper;
    private final PathTemplates pathTemplates;
    private final Logger logger;

    @Inject
    public TemplatingPlugin(FreemarkerHelper fmHelper, PathTemplates pathTemplates, Logger logger) {
        this.fmHelper = fmHelper;
        this.pathTemplates = pathTemplates;
        this.logger = logger;
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final PathTemplateMatch match = pathTemplates.matchedTemplate(request);
        final String templatePath = match.parameters().get("template-path");

        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("templatePath", templatePath);
        map.put("requestParams", match.parameters());
        map.put("queryString", request.getQueryString());

        try {
            // Fetch template through Freemarker engine (server side parsing)
            fmHelper.process(templatePath, map, response.getWriter());

        } catch (TemplateException e) {
            logger.log(Level.SEVERE, "Error during rendering freemarker template");
        } catch (IOException e) {
            if (!response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }
    }

}
