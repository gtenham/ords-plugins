package nl.gertontenham.ords.templates.db;

import freemarker.template.TemplateException;
import nl.gertontenham.ords.templates.freemarker.FreemarkerHelper;
import nl.gertontenham.ords.templates.http.NotFoundException;
import oracle.dbtools.plugin.api.di.annotations.Provides;
import oracle.jdbc.OracleCallableStatement;
import oracle.jdbc.OracleTypes;
import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;
import org.apache.commons.fileupload.util.Streams;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A generic helper to render Template instances with Freemarker templates.
 *
 */
@Provides
public class PLSQLCallService {

    private final Connection conn;
    private final Logger logger;
    private final FreemarkerHelper fmHelper;

    private List<HashMap<String,GenericData>> list;
    private String templatePath;
    private String owner;
    private BinaryDownload downloadFile;

    @Inject
    public PLSQLCallService(Connection conn, FreemarkerHelper fmHelper, Logger logger) {
        this.conn = conn;
        this.fmHelper = fmHelper;
        this.logger = logger;
    }

    /**
     * PLSQL executer for a binary download
     *
     * @param headerParameters Request headers
     * @param requestParameters Parameters in the request
     * @param packageName Database package name containing the download procedure
     * @return this
     */
    public PLSQLCallService executeAsBinaryDownload(Map<String, String[]> headerParameters,
                                                    Map<String, String[]> requestParameters,
                                                    String packageName) throws NotFoundException {
        final String call = "{ call ";
        final String procedureTemplate = ".download(?,?,?) }";
        try {
            OracleCallableStatement oracleCallableStatement =
                    (OracleCallableStatement) conn.prepareCall(call + owner + "." + packageName + procedureTemplate);

            oracleCallableStatement.setObject(1, createStruct(headerParameters), OracleTypes.STRUCT);
            oracleCallableStatement.setObject(2, createStruct(requestParameters), OracleTypes.STRUCT);
            oracleCallableStatement.registerOutParameter (3, OracleTypes.STRUCT, "DOWNLOADABLE_TYPE");

            oracleCallableStatement.execute();
            Struct download = (Struct)oracleCallableStatement.getObject(3);

            Struct binaryStruct = (Struct)download.getAttributes()[0];
            String contentDisposition = (String) download.getAttributes()[1];

            Blob binary = (Blob)binaryStruct.getAttributes()[0];
            long size = ((java.lang.Number) binaryStruct.getAttributes()[1]).longValue();
            String fileName = (String) binaryStruct.getAttributes()[2];
            String contentType = (String) binaryStruct.getAttributes()[3];

            BinaryFile binaryFile = new BinaryFile(binary.getBinaryStream(), size, fileName, contentType);
            downloadFile = new BinaryDownload(binaryFile, contentDisposition);

            oracleCallableStatement.close();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error during download call in package " + owner + "." + packageName);
            throw new NotFoundException();
        }
        return this;
    }

    /**
     * Execute "upload" procedure withing given package and provides the request parameters as
     * input parameters
     *
     * @param headerParameters Request headers
     * @param requestParameters Parameters in the request
     * @param packageName Database package name containing the upload procedure
     * @param uploadItem Binary file which needs to be written to the upload procedure
     * @return this
     */
    public PLSQLCallService executeAsBinaryUpload(Map<String, String[]> headerParameters,
                                                  Map<String, String[]> requestParameters,
                                                  String packageName,
                                                  BinaryFile uploadItem) throws NotFoundException {
        final String call = "{ call ";
        final String procedureTemplate = ".upload(?,?,?) }";
        try {
            OracleCallableStatement oracleCallableStatement =
                    (OracleCallableStatement) conn.prepareCall(call + owner + "." + packageName + procedureTemplate);

            oracleCallableStatement.setObject(1, createStruct(headerParameters), OracleTypes.STRUCT);
            oracleCallableStatement.setObject(2, createStruct(requestParameters), OracleTypes.STRUCT);
            oracleCallableStatement.setObject(3, createBinaryStruct(uploadItem), OracleTypes.STRUCT);

            oracleCallableStatement.execute();

            oracleCallableStatement.close();

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error during upload call in package " + owner + "." + packageName);
            throw new NotFoundException();
        }
        return this;
    }

    /**
     * Execute "execute" procedure within given package procedure and provides the request parameters
     * as input parameters
     *
     * @param headerParameters Request headers
     * @param requestParameters Parameters in the request
     * @param packageName Database package name containing the execute procedure
     * @return this
     */
    public PLSQLCallService executeAsCursor(Map<String, String[]> headerParameters,
                                            Map<String, String[]> requestParameters,
                                            String packageName) throws NotFoundException {
        final String call = "{ call ";
        final String procedureTemplate = ".execute(?,?,?,?) }";

        try {
            OracleCallableStatement oracleCallableStatement =
                    (OracleCallableStatement)conn.prepareCall (call + owner + "." + packageName + procedureTemplate);

            oracleCallableStatement.setObject(1, createStruct(headerParameters), OracleTypes.STRUCT);
            oracleCallableStatement.setObject(2, createStruct(requestParameters), OracleTypes.STRUCT);
            oracleCallableStatement.registerOutParameter (3, OracleTypes.CURSOR);
            oracleCallableStatement.registerOutParameter (4, OracleTypes.CHAR);

            oracleCallableStatement.execute();
            ResultSet rs = (ResultSet)oracleCallableStatement.getObject(3);
            templatePath = oracleCallableStatement.getString(4);

            list = convertResultSetToList(rs);

            rs.close();
            oracleCallableStatement.close();

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error during execute call in package " + owner + "." + packageName);
            throw new NotFoundException();
        }

        return this;
    }

    /**
     * Fetch ORDS mapped schema owner for given uri-pattern
     *
     * @param pattern URI pattern used in ORDS schema
     * @return this
     */
    public PLSQLCallService fetchSchemaOwner(String pattern) {
        owner = DBCache.currentSchema(pattern, conn);
        logger.log(Level.FINE, "From dbcache " + owner);
        return this;
    }

    /**
     * Download file processor
     *
     * @param response Binary stream will be written directly to the response
     * @throws IOException when binary stream can not be written to the response
     */
    public void processDownload(HttpServletResponse response) throws IOException {
        byte[] buffer = new byte[10240];
        if (downloadFile != null) {
            BinaryFile binaryFile = downloadFile.getBinaryFile();
            String contentDispositionHeader = String.format("%s; filename=\"%s\""
                    ,downloadFile.getContentDisposition(),binaryFile.getName());
            response.setContentType(binaryFile.getContentType());
            response.addHeader("Content-Disposition",contentDispositionHeader);
            for (int length = 0; (length = binaryFile.getBlobStream().read(buffer)) > 0; ) {
                response.getOutputStream().write(buffer, 0, length);
            }

        }
    }

    /**
     * Process freemarker template
     *
     * @param map Freemarker model
     * @param writer Freemarker output will be written here
     * @throws IOException When freemarker template can not be found
     */
    public void processTemplate(Map<String, Object> map, PrintWriter writer) throws IOException {
        map.put("templatePath", templatePath);
        if (list != null && !list.isEmpty()) {
            map.put("results", list);
        }

        try {
            // Fetch template through Freemarker engine (server side parsing)
            if (templatePath != null) {
                fmHelper.process(templatePath, map, writer);
            }

        } catch (TemplateException e) {
            logger.log(Level.SEVERE, "Error during processing freemarker template");
        }
    }

    public FreemarkerHelper getFmHelper() {
        return fmHelper;
    }

    public Connection getConnection() {
        return conn;
    }

    private List<HashMap<String,GenericData>> convertResultSetToList(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int columns = md.getColumnCount();
        List<HashMap<String,GenericData>> list = new ArrayList<HashMap<String,GenericData>>();

        while (rs.next()) {
            HashMap<String,GenericData> row = new HashMap<String, GenericData>(columns);
            for(int i=1; i<=columns; ++i) {
                row.put(md.getColumnLabel(i), new GenericData(md.getColumnTypeName(i)
                                                            , md.isNullable(i)
                                                            , md.getPrecision(i)
                                                            , md.getScale(i)
                                                            , md.getColumnDisplaySize(i)
                                                            , rs.getObject(i))
                                                            );
            }
            list.add(row);
        }

        return list;
    }

    @SuppressWarnings("Since15")
    private Struct createBinaryStruct(BinaryFile uploadItem) throws SQLException {
        Struct binaryType = null;
        Blob binary;
        try {
            binary = conn.createBlob();
            final String filename = uploadItem.getName();
            final String contentType = uploadItem.getContentType();
            long length = Streams.copy(uploadItem.getBlobStream(), binary.setBinaryStream(1), true);

            binaryType = conn.createStruct("BINARY_TYPE", new Object[] { binary, length, filename, contentType });

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to copy uploaded file into Oracle blob");
        }

        return binaryType;
    }


    @SuppressWarnings("Since15")
    private Struct createStruct(Map<String, String[]> args) throws SQLException {
        ArrayDescriptor values_type_array = ArrayDescriptor.createDescriptor("VALUES_TYPE", conn);
        ArrayDescriptor multivalued_type_array = ArrayDescriptor.createDescriptor("MULTIVALUED_TYPE", conn);

        List<Struct> structsList = new ArrayList<Struct>();
        for (String s : args.keySet()) {

            //Array _values = conn.createArrayOf("VALUES_TYPE", args.get(s));
            ARRAY _values = new ARRAY(values_type_array, conn, args.get(s));
            Struct _namevalues = conn.createStruct("NAMEVALUES_TYPE", new Object[] { s, _values });
            structsList.add(_namevalues);

        }

        //Array mValueArray = conn.createArrayOf("MULTIVALUED_TYPE", structsList.toArray());
        ARRAY mValueArray = new ARRAY(multivalued_type_array, conn, structsList.toArray());
        Struct multiValuedType = conn.createStruct("MULTIVALUED_PARAMETER_TYPE", new Object[] { mValueArray });

        return multiValuedType;

    }
}
