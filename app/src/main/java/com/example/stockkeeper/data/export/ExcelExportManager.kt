package com.example.stockkeeper.data.export

import android.database.Cursor
import android.net.Uri
import android.util.Base64
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.stockkeeper.StockKeeperApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ExcelExportManager(private val application: StockKeeperApplication) {
    suspend fun export(destination: Uri) = withContext(Dispatchers.IO) {
        val database = application.database.openHelper.readableDatabase
        application.contentResolver.openOutputStream(destination, "w")?.use { output ->
            writeWorkbook(database, output)
        } ?: error("Selected file cannot be opened")
    }

    private fun writeWorkbook(database: SupportSQLiteDatabase, output: OutputStream) {
        val tables = userTables(database)
        ZipOutputStream(output.buffered()).use { zip ->
            zip.writeEntry("[Content_Types].xml", contentTypes(tables.size))
            zip.writeEntry("_rels/.rels", ROOT_RELS)
            zip.writeEntry("xl/workbook.xml", workbook(tables))
            zip.writeEntry("xl/_rels/workbook.xml.rels", workbookRelationships(tables.size))
            zip.writeEntry("xl/styles.xml", STYLES)
            tables.forEachIndexed { index, table ->
                zip.putNextEntry(ZipEntry("xl/worksheets/sheet${index + 1}.xml"))
                database.query("SELECT * FROM ${quoteIdentifier(table)}").use { cursor ->
                    writeWorksheet(zip, cursor)
                }
                zip.closeEntry()
            }
        }
    }

    private fun userTables(database: SupportSQLiteDatabase): List<String> = buildList {
        database.query(
            "SELECT name FROM sqlite_master WHERE type='table' " +
                "AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%' " +
                "AND name NOT LIKE 'room_%' ORDER BY name",
        ).use { cursor -> while (cursor.moveToNext()) add(cursor.getString(0)) }
    }

    private fun writeWorksheet(output: OutputStream, cursor: Cursor) {
        output.write((XML_HEADER + "<worksheet xmlns=\"$SPREADSHEET_NS\">" +
            "<sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" " +
            "activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews><sheetData>").toByteArray())
        output.write("<row r=\"1\">".toByteArray())
        cursor.columnNames.forEachIndexed { column, name ->
            output.write(cell(column, 1, name, true).toByteArray())
        }
        output.write("</row>".toByteArray())

        var row = 2
        while (cursor.moveToNext()) {
            output.write("<row r=\"$row\">".toByteArray())
            repeat(cursor.columnCount) { column ->
                val value = when (cursor.getType(column)) {
                    Cursor.FIELD_TYPE_NULL -> null
                    Cursor.FIELD_TYPE_BLOB -> Base64.encodeToString(cursor.getBlob(column), Base64.NO_WRAP)
                    else -> cursor.getString(column)
                }
                if (value != null) output.write(cell(column, row, value, false).toByteArray())
            }
            output.write("</row>".toByteArray())
            row++
        }
        val lastColumn = columnName((cursor.columnCount - 1).coerceAtLeast(0))
        output.write("</sheetData><autoFilter ref=\"A1:${lastColumn}1\"/></worksheet>".toByteArray())
    }

    private fun cell(column: Int, row: Int, value: String, header: Boolean): String =
        "<c r=\"${columnName(column)}$row\" t=\"inlineStr\"${if (header) " s=\"1\"" else ""}>" +
            "<is><t xml:space=\"preserve\">${escape(value)}</t></is></c>"

    private fun columnName(index: Int): String {
        var number = index + 1
        return buildString {
            while (number > 0) {
                insert(0, ('A'.code + (number - 1) % 26).toChar())
                number = (number - 1) / 26
            }
        }
    }

    private fun quoteIdentifier(value: String) = "\"${value.replace("\"", "\"\"")}\""

    private fun escape(value: String): String = buildString(value.length) {
        value.forEach { char ->
            when (char) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> if (char == '\t' || char == '\n' || char == '\r' || char.code >= 0x20) append(char)
            }
        }
    }

    private fun workbook(tables: List<String>) = XML_HEADER +
        "<workbook xmlns=\"$SPREADSHEET_NS\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>" +
        tables.mapIndexed { index, table ->
            "<sheet name=\"${escape(sheetName(table, index))}\" sheetId=\"${index + 1}\" r:id=\"rId${index + 1}\"/>"
        }.joinToString("") + "</sheets></workbook>"

    private fun sheetName(table: String, index: Int): String =
        table.replace(Regex("[\\\\/?*\\[\\]:]"), "_").take(31).ifBlank { "Table ${index + 1}" }

    private fun workbookRelationships(count: Int) = XML_HEADER +
        "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
        (1..count).joinToString("") { index ->
            "<Relationship Id=\"rId$index\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet$index.xml\"/>"
        } + "<Relationship Id=\"rId${count + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/></Relationships>"

    private fun contentTypes(count: Int) = XML_HEADER +
        "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
        "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
        "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
        "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
        "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>" +
        (1..count).joinToString("") { index ->
            "<Override PartName=\"/xl/worksheets/sheet$index.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
        } + "</Types>"

    private fun ZipOutputStream.writeEntry(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray())
        closeEntry()
    }

    private companion object {
        const val XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        const val SPREADSHEET_NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
        const val ROOT_RELS = "${XML_HEADER}<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>"
        const val STYLES = "${XML_HEADER}<styleSheet xmlns=\"$SPREADSHEET_NS\"><fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font><font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts><fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill></fills><borders count=\"1\"><border/></borders><cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs><cellXfs count=\"2\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/><xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/></cellXfs></styleSheet>"
    }
}
