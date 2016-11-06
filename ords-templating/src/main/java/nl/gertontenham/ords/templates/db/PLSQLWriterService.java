package nl.gertontenham.ords.templates.db;

import freemarker.template.TemplateException;
import nl.gertontenham.ords.templates.freemarker.FreemarkerHelper;
import oracle.dbtools.plugin.api.di.annotations.Provides;
import oracle.jdbc.OracleCallableStatement;
import oracle.jdbc.OracleTypes;
import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;

import javax.inject.Inject;
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
public class PLSQLWriterService {

    private final Connection conn;
    private final Logger logger;
    private final FreemarkerHelper fmHelper;

    private List<HashMap<String,GenericData>> list;
    private String templatePath;
    private String owner;

    @Inject
    public PLSQLWriterService(Connection conn, FreemarkerHelper fmHelper, Logger logger) {
        this.conn = conn;
        this.fmHelper = fmHelper;
        this.logger = logger;
    }

    public PLSQLWriterService executeAsCursor(Map<String, String[]> headerParameters,
                                              Map<String, String[]> requestParameters,
                                              String packageName) {
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
            logger.log(Level.SEVERE, "Error during call package " + owner + "." + packageName,e);
        }

        return this;
    }

    public PLSQLWriterService fetchSchemaOwner(String pattern) {
        owner = DBCache.currentSchema(pattern, conn);
        logger.log(Level.FINE, "From dbcache " + owner);
        return this;
    }

    public void writeTemplate(Map<String, Object> map, PrintWriter writer) throws IOException {
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
    private Struct createStruct(Map<String, String[]> args) throws SQLException {
        ArrayDescriptor values_type_array = ArrayDescriptor.createDescriptor("VALUES_TYPE", conn);
        ArrayDescriptor multivalued_type_array = ArrayDescriptor.createDescriptor("MULTIVALUED_TYPE", conn);

        List<Struct> structsList = new ArrayList<Struct>();
        for (String s : args.keySet()) {
            ARRAY _values = new ARRAY(values_type_array, conn, args.get(s));
            Struct _namevalues = conn.createStruct("NAMEVALUES_TYPE", new Object[] { s, _values });
            structsList.add(_namevalues);
        }

        ARRAY mValueArray = new ARRAY(multivalued_type_array, conn, structsList.toArray());
        return conn.createStruct("MULTIVALUED_PARAMETER_TYPE", new Object[] { mValueArray });



        //List<Struct> structsList = new ArrayList<Struct>();

        //for (String s : args.keySet()) {
            //Array _values = conn.createArrayOf("OSERV.VALUES_TYPE", args.get(s));
            //Struct _namevalues = conn.createStruct("OSERV.NAMEVALUES_TYPE", new Object[] { s, _values });
            //structsList.add(_namevalues);
        //}
        //return conn.createStruct("OSERV.MULTIVALUED_PARAMETER_TYPE", new Object[] { conn.createArrayOf("OSERV.MULTIVALUED_TYPE", structsList.toArray())});

    }
}
