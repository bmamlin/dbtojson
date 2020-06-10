/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at 
 * http://mozilla.org/MPL/2.0/.
 */

/**
 * DbMeta.groovy
 *
 * This script connects to a MySQL database, inspects the tables, and returns 
 * an object representing the database schema.
 *
 */
@GrabConfig( systemClassLoader=true )
@Grab('mysql:mysql-connector-java:5.1.30')

import groovy.sql.*
import java.sql.Types

class DbMeta {

    static class Table {
        String name
        def columns = []
    }

    static class Column {
        Boolean key
        Boolean required
        String name
        String type
        Integer size
        String fkTable = null
        String fkColumn = null
        Boolean isFk() { fkTable != null }
    }

    // Pretty names for java.sql.Types
    static def knownDataTypes = [
            (Types.ARRAY)   : "array",
            (Types.BIGINT)  : "bigint",
            (Types.BINARY)  : "binary",
            (Types.BIT)     : "bit",
            (Types.BLOB)    : "blob",
            (Types.BOOLEAN) : "boolean",
            (Types.CHAR)    : "char",
            (Types.CLOB)    : "clob",
            (Types.DATALINK): "datalink",
            (Types.DATE)    : "date",
            (Types.DECIMAL) : "decimal",
            (Types.DISTINCT): "distinct",
            (Types.DOUBLE)   : "double",
            (Types.FLOAT)    : "float",
            (Types.INTEGER)  : "integer",
            (Types.JAVA_OBJECT) : "java object",
            (Types.LONGNVARCHAR) : "long n varchar",
            (Types.LONGVARBINARY) : "long varbinary",
            (Types.LONGVARCHAR) : "long varchar",
            (Types.NCHAR)    : "n char",
            (Types.NCLOB)    : "n clob",
            (Types.NULL)     : "null",
            (Types.NUMERIC)  : "numeric",
            (Types.NVARCHAR) : "n varchar",
            (Types.OTHER)    : "other",
            (Types.REAL)     : "real",
            (Types.REF)      : "ref",
            (Types.ROWID)    : "row id",
            (Types.SMALLINT) : "smallint",
            (Types.SQLXML)   : "xml",
            (Types.STRUCT)   : "struct",
            (Types.TIME)     : "time",
            (Types.TIMESTAMP): "timestamp",
            (Types.TINYINT)  : "tinyint",
            (Types.VARBINARY): "varbinary",
            (Types.VARCHAR)  : "varchar"
    ]

    static String dataType(type) {
        (knownDataTypes[type] ?: "*** unknown ***")
    }

    public static parse(url, username, password) {
        def sql = Sql.newInstance(url, username, password, 'com.mysql.jdbc.Driver')
        def tables = []
        def meta = sql.connection.metaData
        def metaTables = meta.getTables(null, null, "%", "TABLE")
        while (metaTables.next()) {
            def table = new Table()
            table.name = metaTables.getString("TABLE_NAME")
            def keys = []
            def metaKeys = meta.getPrimaryKeys(null, null, table.name)
            while (metaKeys.next()) {
                keys << metaKeys.getString("COLUMN_NAME")
            }
            def fks = [:]
            def metaFks = meta.getImportedKeys(null, null, table.name)
            while (metaFks.next()) {
                fks[metaFks.getString("FKCOLUMN_NAME")] = [
                  'table' : metaFks.getString("PKTABLE_NAME"),
                  'column' : metaFks.getString("PKCOLUMN_NAME")
                ]
            }
            def metaColumns = meta.getColumns(null, null, table.name, "%")
            while (metaColumns.next()) {
                def column = new Column()
                column.name = metaColumns.getString("COLUMN_NAME")
                column.key = keys.contains(column.name)
                column.type = dataType(metaColumns.getInt("DATA_TYPE"))
                column.size = metaColumns.getInt("COLUMN_SIZE")
                column.required = (metaColumns.getString("IS_NULLABLE") == 'NO')
                if (fks[column.name]) {
                    column.fkTable = fks[column.name].table
                    column.fkColumn = fks[column.name].column
                }
                table.columns << column
            }
            tables << table
        }
        return tables
    }

}