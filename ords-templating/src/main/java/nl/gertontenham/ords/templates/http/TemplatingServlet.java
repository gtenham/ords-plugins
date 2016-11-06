package nl.gertontenham.ords.templates.http;

import freemarker.template.TemplateException;
import nl.gertontenham.ords.templates.freemarker.FreemarkerHelper;
import oracle.dbtools.plugin.api.di.annotations.Provides;
import oracle.dbtools.plugin.api.http.annotations.Dispatches;
import oracle.dbtools.plugin.api.http.annotations.PathTemplate;
import oracle.dbtools.plugin.api.routes.PathTemplateMatch;
import oracle.dbtools.plugin.api.routes.PathTemplates;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ORDS Templating servlet endpoint
 */
@Provides
@Dispatches(@PathTemplate("/templates/:template-path*"))
public class TemplatingServlet extends HttpServlet {

    private final FreemarkerHelper fmHelper;
    private final PathTemplates pathTemplates;
    private final Logger logger;

    @Inject
    public TemplatingServlet(FreemarkerHelper fmHelper, PathTemplates pathTemplates, Logger logger) {
        this.fmHelper = fmHelper;
        this.pathTemplates = pathTemplates;
        this.logger = logger;
    }

    private void dispatch(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final PathTemplateMatch match = pathTemplates.matchedTemplate(request);
        final String templatePath = match.parameters().get("template-path");

        Map<String, Object> templateMap = fmHelper.getRequestData(request);
        templateMap.put("templatePath", templatePath);

        try {
            // Fetch template through Freemarker engine (server side parsing)
            if (templatePath != null) {
                fmHelper.process(templatePath, templateMap, response.getWriter());
            }

        } catch (TemplateException e) {
            logger.log(Level.SEVERE, "Error during processing freemarker template");
        } catch (IOException e) {
            if (!response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        dispatch(req, resp);
    }

}
