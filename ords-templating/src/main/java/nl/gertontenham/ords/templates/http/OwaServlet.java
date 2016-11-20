package nl.gertontenham.ords.templates.http;

import nl.gertontenham.ords.templates.db.DBInstaller;
import nl.gertontenham.ords.templates.db.PLSQLWriterService;
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
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ORDS Plsql calling servlet endpoint
 */
@Provides
@Dispatches({
        @PathTemplate(name="normal", value="/owa/:package-name"),
        @PathTemplate(name="install", value="/owa-db-install")})
public class OwaServlet extends HttpServlet {

    private final PLSQLWriterService plsqlWriterService;
    private final PathTemplates pathTemplates;
    private final Logger logger;

    @Inject
    public OwaServlet(PLSQLWriterService plsqlWriterService, PathTemplates pathTemplates, Logger logger) {
        this.plsqlWriterService = plsqlWriterService;
        this.pathTemplates = pathTemplates;
        this.logger = logger;
    }

    private void installDB() {
        try {
            DBInstaller.migrate(plsqlWriterService.getConnection());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error during installing database objects ");
        }
    }

    private void dispatch(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final PathTemplateMatch match = pathTemplates.matchedTemplate(request);

        if (match.name().equals("install")) {
            installDB();
            response.sendError(HttpServletResponse.SC_ACCEPTED,"Installed db");
        } else {
            final String[] splitContext = request.getContextPath().split("/");
            final String uriPattern = splitContext[splitContext.length - 1];
            final String packageName = match.parameters().get("package-name");

            Map<String, String[]> requestParameters = buildParameters(request);
            Map<String, String[]> headerParameters = buildHeaders(request);

            Map<String, Object> templateMap = plsqlWriterService.getFmHelper().getRequestData(request);

            try {

                plsqlWriterService
                        .fetchSchemaOwner(uriPattern)
                        .executeAsCursor(headerParameters, requestParameters, packageName)
                        .writeTemplate(templateMap, response.getWriter());

            } catch (IOException e) {
                if (!response.isCommitted()) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        dispatch(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        dispatch(req, resp);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        dispatch(req, resp);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        dispatch(req, resp);
    }

    private Map<String, String[]> buildHeaders(HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();
        Map<String, String[]> returnHeaders = new HashMap<String, String[]>();
        while (headerNames.hasMoreElements()) {
            String header = headerNames.nextElement();
            returnHeaders.put(header.toLowerCase(), new String[] { request.getHeader(header) });
        }

        returnHeaders.put("context-path", new String[] { request.getContextPath() });
        returnHeaders.put("path-info", new String[] { request.getPathInfo() });
        returnHeaders.put("query-string", new String[] { request.getQueryString() });
        returnHeaders.put("method", new String[] { request.getMethod() });
        returnHeaders.put("remote-user", new String[] { request.getRemoteUser() });
        //addAdditionalHeadersFromEnvironment(request, returnHeaders);

        return returnHeaders;
    }

    private Map<String, String[]> buildParameters(HttpServletRequest request) {
        Enumeration<String> argumentNames = request.getParameterNames();
        Map<String, String[]> returnArguments = new HashMap<String, String[]>();
        while (argumentNames.hasMoreElements()) {
            String argument = argumentNames.nextElement();
            returnArguments.put(argument.toLowerCase(), request.getParameterValues(argument));
        }
        return returnArguments;
    }
}
