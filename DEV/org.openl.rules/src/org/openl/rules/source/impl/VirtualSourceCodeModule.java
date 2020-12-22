package org.openl.rules.source.impl;

import java.io.*;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.openl.source.impl.ASourceCodeModule;

@Deprecated
public class VirtualSourceCodeModule extends ASourceCodeModule {

    public static final String SOURCE_URI = "<virtual_uri>";
    private static final String VIRTUAL_SHEET_NAME = "$virtual_sheet$";

    private final Workbook workbook;

    public VirtualSourceCodeModule() {
        workbook = new HSSFWorkbook();
        workbook.createSheet(VIRTUAL_SHEET_NAME);
    }

    @Override
    protected String makeUri() {
        return SOURCE_URI;
    }

    @Override
    public InputStream getByteStream() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            return new ByteArrayInputStream(out.toByteArray());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Reader getCharacterStream() {
        return new InputStreamReader(getByteStream());
    }
}
