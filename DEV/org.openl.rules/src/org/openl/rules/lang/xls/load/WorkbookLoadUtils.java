package org.openl.rules.lang.xls.load;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.openl.excel.parser.ExcelUtils;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.source.IOpenSourceCodeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Package scope util class
final class WorkbookLoadUtils {
    private WorkbookLoadUtils() {
    }

    static Workbook loadWorkbook(IOpenSourceCodeModule fileSource) {
        Logger log = LoggerFactory.getLogger(WorkbookLoadUtils.class);
        log.debug("Loading workbook '{}'...", fileSource.getUri());

        InputStream is = null;
        Workbook workbook;

        ExcelUtils.configureZipBombDetection();

        try {
            is = fileSource.getByteStream();
            workbook = createWorkbook(is);
        } catch (Exception e) {
            log.error("Error while preprocessing workbook", e);

            String message = "Cannot open source file or file is corrupted: " + ExceptionUtils.getRootCauseMessage(e);
            throw new OpenlNotCheckedException(message, e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (Exception e) {
                log.error("Error trying close input stream:", e);
            }
        }
        return workbook;
    }

    // EPBDS-9685 synchronized should be removed after issue fixed in poi
    static synchronized Workbook createWorkbook(InputStream is) throws IOException {
        return WorkbookFactory.create(is);
    }
}
