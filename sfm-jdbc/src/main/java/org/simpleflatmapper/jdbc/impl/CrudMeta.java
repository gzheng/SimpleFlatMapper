package org.simpleflatmapper.jdbc.impl;

import org.simpleflatmapper.jdbc.JdbcColumnKey;
import org.simpleflatmapper.jdbc.JdbcMapperFactory;
import org.simpleflatmapper.jdbc.SqlTypeColumnProperty;
import org.simpleflatmapper.map.property.AutoGeneratedProperty;
import org.simpleflatmapper.map.property.FieldMapperColumnDefinition;
import org.simpleflatmapper.map.property.IgnoreAutoGeneratedProperty;
import org.simpleflatmapper.map.property.KeyProperty;
import org.simpleflatmapper.map.mapper.ColumnDefinitionProvider;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CrudMeta {

    private final DatabaseMeta databaseMeta;
    private final String table;
    private final ColumnMeta[] columnMetas;

    public CrudMeta(DatabaseMeta databaseMeta, String table, ColumnMeta[] columnMetas) {
        this.databaseMeta = databaseMeta;
        this.table = table;
        this.columnMetas = columnMetas;
    }

    public DatabaseMeta getDatabaseMeta() {
        return databaseMeta;
    }

    public String getTable() {
        return table;
    }

    public ColumnMeta[] getColumnMetas() {
        return columnMetas;
    }

    public boolean hasGeneratedKeys() {
        for(ColumnMeta cm : columnMetas) {
            if (cm.isKey() && cm.isGenerated()) {
                return true;
            }
        }
        return false;
    }

    public static CrudMeta of(Connection connection,
                              String table,
                              ColumnDefinitionProvider<FieldMapperColumnDefinition<JdbcColumnKey>, JdbcColumnKey> columnDefinitionProvider) throws SQLException {
        Statement statement = connection.createStatement();
        try {
            ResultSet resultSet = statement.executeQuery("SELECT * FROM " + table + " WHERE 1 = 2");
            try {


                ResultSetMetaData resultSetMetaData = resultSet.getMetaData();

                DatabaseMetaData metaData = connection.getMetaData();
                DatabaseMeta databaseMeta = new DatabaseMeta(metaData.getDatabaseProductName(), metaData.getDatabaseMajorVersion(), metaData.getDatabaseMinorVersion());
                ColumnMeta[] columnMetas = new ColumnMeta[resultSetMetaData.getColumnCount()];
                List<String> primaryKeys = getPrimaryKeys(connection, resultSetMetaData, columnDefinitionProvider);

                for(int i = 0; i < columnMetas.length; i++) {
                    String columnName = resultSetMetaData.getColumnName(i + 1);

                    FieldMapperColumnDefinition<JdbcColumnKey> columnDefinition = columnDefinitionProvider.getColumnDefinition(JdbcColumnKey.of(resultSetMetaData, i + 1));

                    AutoGeneratedProperty autoGeneratedProperty = columnDefinition.lookFor(AutoGeneratedProperty.class);
                    if (autoGeneratedProperty == null 
                            && resultSetMetaData.isAutoIncrement(i + 1)
                            && ! columnDefinition.has(IgnoreAutoGeneratedProperty.class)) {
                        autoGeneratedProperty = AutoGeneratedProperty.DEFAULT;
                    }
                    columnMetas[i] = new ColumnMeta(
                            columnName,
                            resultSetMetaData.getColumnType(i + 1),
                            primaryKeys.contains(columnName),
                            autoGeneratedProperty);
                }

                return new CrudMeta(databaseMeta, table, columnMetas);


            } finally {
                resultSet.close();
            }
        } finally {
            statement.close();
        }

    }

    private static List<String> getPrimaryKeys(Connection connection, ResultSetMetaData resultSetMetaData, ColumnDefinitionProvider<FieldMapperColumnDefinition<JdbcColumnKey>, JdbcColumnKey> columnDefinitionProvider) throws SQLException {
        List<String> primaryKeys = new ArrayList<String>();

        for(int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
            JdbcColumnKey key = JdbcColumnKey.of(resultSetMetaData, i);
            if (columnDefinitionProvider.getColumnDefinition(key).has(KeyProperty.class)) {
                primaryKeys.add(key.getName());
            }
        }

        if (!primaryKeys.isEmpty()) {
            return primaryKeys;
        }

        ResultSet set = connection.getMetaData().getPrimaryKeys(resultSetMetaData.getCatalogName(1), resultSetMetaData.getSchemaName(1), resultSetMetaData.getTableName(1));
        try {
            while (set.next()) {
                primaryKeys.add(set.getString("COLUMN_NAME"));
            }
        } finally {
            set.close();
        }
        return primaryKeys;
    }

    public void addColumnProperties(JdbcMapperFactory mapperFactory) {
        for(ColumnMeta columnMeta : columnMetas) {
            mapperFactory.addColumnProperty(columnMeta.getColumn(), SqlTypeColumnProperty.of(columnMeta.getSqlType()));
            if (columnMeta.isKey()) {
                mapperFactory.addColumnProperty(columnMeta.getColumn(), KeyProperty.DEFAULT);
            }
        }
    }
}
