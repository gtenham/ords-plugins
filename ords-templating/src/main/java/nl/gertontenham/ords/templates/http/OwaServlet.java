package nl.gertontenham.ords.templates.http;

import nl.gertontenham.ords.templates.db.BinaryFile;
import nl.gertontenham.ords.templates.db.DBInstaller;
import nl.gertontenham.ords.templates.db.PLSQLCallService;
import oracle.dbtools.plugin.api.di.annotations.Provides;
import oracle.dbtools.plugin.api.http.annotations.Dispatches;
import oracle.dbtools.plugin.api.http.annotations.PathTemplate;
import oracle.dbtools.plugin.api.routes.PathTemplateMatch;
import oracle.dbtools.plugin.api.routes.PathTemplates;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ORDS Plsql calling servlet endpoint
 */
@Provides
@Dispatches({
        @PathTemplate(name="normal", value="/owa/:package-name"),
        @PathTemplate(name="files", value="/owa-files/:package-name"),
        @PathTemplate(name="install", value="/owa-db-install")})
public class OwaServlet extends HttpServlet {

    private final PLSQLCallService plsqlCallService;
    private final PathTemplates pathTemplates;
    private final Logger logger;

    @Inject
    public OwaServlet(PLSQLCallService plsqlCallService, PathTemplates pathTemplates, Logger logger) {
        this.plsqlCallService = plsqlCallService;
        this.pathTemplates = pathTemplates;
        this.logger = logger;
    }

    private void installDB(String action) {
        try {
            if (action.equalsIgnoreCase("example")) {
                DBInstaller.example(plsqlCallService.getConnection());
            } else {
                DBInstaller.migrate(plsqlCallService.getConnection());
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error during installing database objects ");
        }
    }

    private void dispatch(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final PathTemplateMatch match = pathTemplates.matchedTemplate(request);
        final String[] splitContext = request.getContextPath().split("/");
        final String uriPattern = splitContext[splitContext.length - 1];

        Map<String, String[]> requestParameters = null;
        Map<String, String[]> headerParameters = buildHeaders(request);

        if (match.name().equals("install")) {
            String what = match.parameters().get("what");
            what = null == what ? "migrate" : what;
            installDB(what);
            response.sendError(HttpServletResponse.SC_ACCEPTED,"Installed db");
        } else if (match.name().equals("normal")) {

            final String packageName = match.parameters().get("package-name");
            requestParameters = buildParameters(request);
            Map<String, Object> templateMap = plsqlCallService.getFmHelper().getRequestData(request);

            try {

                plsqlCallService
                        .fetchSchemaOwner(uriPattern)
                        .executeAsCursor(headerParameters, requestParameters, packageName)
                        .processTemplate(templateMap, response.getWriter());

            } catch (IOException e) {
                if (!response.isCommitted()) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
            } catch (NotFoundException e) {
                if (!response.isCommitted()) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
            }
        } else if (match.name().equals("files")) {
            final String packageName = match.parameters().get("package-name");
            boolean isMultipart = ServletFileUpload.isMultipartContent(request);

            try {
                if (isMultipart) {
                    List<BinaryFile> uploadItems = new ArrayList<BinaryFile>();
                    requestParameters = buildParametersFromMultipart(request, uploadItems);

                    if (uploadItems.size() > 0) {
                        for (int i = 0; i < uploadItems.size(); i++) {
                            plsqlCallService
                                    .fetchSchemaOwner(uriPattern)
                                    .executeAsBinaryUpload(headerParameters, requestParameters, packageName,uploadItems.get(i));
                        }
                    }
                    response.sendError(HttpServletResponse.SC_NO_CONTENT);
                } else {
                    requestParameters = buildParameters(request);
                    try {
                        plsqlCallService
                                .fetchSchemaOwner(uriPattern)
                                .executeAsBinaryDownload(headerParameters,requestParameters,packageName)
                                .processDownload(response);
                    } catch (IOException e) {
                        if (!response.isCommitted()) {
                            response.sendError(HttpServletResponse.SC_NOT_FOUND);
                        }
                    }
                }
            } catch (NotFoundException e) {
                if (!response.isCommitted()) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
            }

        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
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

    private Map<String, String[]> buildParametersFromMultipart(HttpServletRequest request, List<BinaryFile> uploadItems) {
        Map<String, String[]> returnArguments = new HashMap<String, String[]>();
        Map<String, List<String>> intermediateArguments = new HashMap<String, List<String>>();
        // Create a new file upload handler
        ServletFileUpload upload = new ServletFileUpload();
        try {
            // Parse the request
            FileItemIterator fii = upload.getItemIterator(request);
            while (fii.hasNext()) {
                FileItemStream item = fii.next();
                String name = item.getFieldName();
                InputStream stream = item.openStream();
                if (item.isFormField()) {
                    String streamValue = Streams.asString(stream);
                    if (intermediateArguments.containsKey(name)) {
                        intermediateArguments.get(name).add(streamValue);
                    } else {
                        intermediateArguments.put(name, Arrays.asList(new String[] { streamValue }));
                    }
                } else {
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    String itemName = item.getName();
                    String itemType = item.getContentType();
                    long length = Streams.copy(stream, output, true);
                    InputStream is = new ByteArrayInputStream(output.toByteArray());
                    output.close();
                    BinaryFile bf = new BinaryFile(is,length,itemName,itemType);
                    uploadItems.add(bf);
                }
            }
        } catch (FileUploadException e) {
            logger.log(Level.SEVERE, "Unable to upload file");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to copy file data");
        }

        for (String s : intermediateArguments.keySet()) {
            returnArguments.put(s, intermediateArguments.get(s).toArray(new String[intermediateArguments.get(s).size()]));
        }
        return returnArguments;
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
