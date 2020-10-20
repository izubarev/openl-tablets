package org.openl.rules.excel.builder.export;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.openl.rules.excel.builder.CellRangeSettings;
import org.openl.rules.excel.builder.template.DataTypeTableStyle;
import org.openl.rules.excel.builder.template.TableStyle;
import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.FieldModel;
import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatatypeTableExporter extends AbstractOpenlTableExporter<DatatypeModel> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatatypeTableExporter.class);

    public static final String DATATYPES_SHEET = "Datatypes";

    public static final String DATATYPE_NAME = "\\{datatype.name}";

    @Override
    protected void exportTables(Collection<DatatypeModel> models, Sheet sheet) {
        Cursor endPosition = null;
        TableStyle style = getTableStyle();
        for (DatatypeModel model : models) {
            Cursor startPosition = nextFreePosition(endPosition);
            endPosition = exportTable(model, startPosition, style, sheet);
        }
    }

    @Override
    protected Cursor exportTable(DatatypeModel model, Cursor startPosition, TableStyle defaultStyle, Sheet sheet) {
        DataTypeTableStyle style = (DataTypeTableStyle) defaultStyle;
        RichTextString headerTemplate = style.getHeaderTemplate();
        CellRangeSettings headerSettings = style.getHeaderSizeSettings();
        CellStyle headerStyle = style.getHeaderStyle();

        CellStyle dateStyle = style.getDateStyle();
        CellStyle dateTimeStyle = style.getDateTimeStyle();

        String dtHeaderText = headerTemplate.getString().replaceAll(DATATYPE_NAME, model.getName());
        if (StringUtils.isNotBlank(model.getParent())) {
            dtHeaderText += " extends " + model.getParent();
        }

        addMergedHeader(sheet, startPosition, headerStyle, headerSettings);

        Cell topLeftCell = PoiExcelHelper.getOrCreateCell(startPosition.getColumn(), startPosition.getRow(), sheet);
        RichTextString dtHeader = new XSSFRichTextString(dtHeaderText);
        dtHeader.applyFont(style.getHeaderFont());
        topLeftCell.setCellValue(dtHeader);
        startPosition = startPosition.moveDown(headerSettings.getHeight());

        Cursor endPosition = startPosition;

        Iterator<FieldModel> iterator = model.getFields().iterator();
        while (iterator.hasNext()) {
            boolean lastRow = false;
            FieldModel field = iterator.next();
            if (!iterator.hasNext()) {
                lastRow = true;
            }
            Cursor next = endPosition.moveDown(1);
            Cell typeCell = PoiExcelHelper.getOrCreateCell(next.getColumn(), next.getRow(), sheet);
            String type = field.getType();
            typeCell.setCellValue(type);
            typeCell
                .setCellStyle(lastRow ? style.getLastRowStyle().getTypeStyle() : style.getRowStyle().getTypeStyle());
            next = next.moveRight(1);

            Cell nameCell = PoiExcelHelper.getOrCreateCell(next.getColumn(), next.getRow(), sheet);
            nameCell.setCellValue(field.getName());
            nameCell
                .setCellStyle(lastRow ? style.getLastRowStyle().getNameStyle() : style.getRowStyle().getNameStyle());
            next = next.moveRight(1);

            Cell valueCell = PoiExcelHelper.getOrCreateCell(next.getColumn(), next.getRow(), sheet);
            writeDefaultValueToCell(model, field, valueCell, dateStyle, dateTimeStyle);
            CellStyle styleAfterWrite = valueCell.getCellStyle();
            if (styleAfterWrite.getDataFormat() == 0) {
                valueCell.setCellStyle(
                    lastRow ? style.getLastRowStyle().getValueStyle() : style.getRowStyle().getValueStyle());
            }

            endPosition = next.moveLeft(2);
        }

        return new Cursor(endPosition.getColumn(), endPosition.getRow());
    }

    private void writeDefaultValueToCell(DatatypeModel model,
            FieldModel field,
            Cell valueCell,
            CellStyle dateStyle,
            CellStyle dateTimeStyle) {
        if (field.getDefaultValue() == null) {
            valueCell.setCellValue("");
        } else {
            try {
                setDefaultValue(field, valueCell, dateStyle, dateTimeStyle);
            } catch (ParseException e) {
                LOGGER
                    .error("Error is occurred on writing field: {}, model: {} .", field.getName(), model.getName(), e);
            }
        }
    }

    @Override
    protected String getExcelSheetName() {
        return DATATYPES_SHEET;
    }

    private static void setDefaultValue(FieldModel model,
            Cell valueCell,
            CellStyle dateStyle,
            CellStyle dateTimeStyle) throws ParseException {
        Object defaultValue = model.getDefaultValue();
        String valueAsString = defaultValue.toString();
        switch (model.getType()) {
            case "Integer":
            case "BigInteger":
                Number casted = NumberFormat.getInstance().parse(valueAsString);
                if (casted.longValue() <= Integer.MAX_VALUE) {
                    valueCell.setCellValue(Integer.parseInt(valueAsString));
                } else {
                    valueCell.setCellValue(Long.parseLong(valueAsString));
                }
                break;
            case "Long":
                valueCell.setCellValue(Long.parseLong(valueAsString));
                break;
            case "Double":
                valueCell.setCellValue(Double.parseDouble(valueAsString));
                break;
            case "Float":
                valueCell.setCellValue(new BigDecimal(valueAsString).doubleValue());
                break;
            case "BigDecimal":
                valueCell.setCellValue(valueAsString);
                break;
            case "String":
                if (StringUtils.isBlank(valueAsString)) {
                    valueCell.setCellValue(DEFAULT_STRING_VALUE);
                } else {
                    valueCell.setCellValue(valueAsString);
                }
                break;
            case "Boolean":
                valueCell.setCellValue(Boolean.parseBoolean(valueAsString));
                break;
            case "Date":
                if (defaultValue instanceof Date) {
                    Date dateValue = (Date) defaultValue;
                    valueCell.setCellValue(dateValue);
                    valueCell.setCellStyle(dateStyle);
                } else {
                    OffsetDateTime dateValue = (OffsetDateTime) defaultValue;
                    valueCell.setCellValue(new Date((dateValue).toInstant().toEpochMilli()));
                    valueCell.setCellStyle(dateTimeStyle);
                }
                break;
            default:
                valueCell.setCellValue("");
                break;
        }
    }

}