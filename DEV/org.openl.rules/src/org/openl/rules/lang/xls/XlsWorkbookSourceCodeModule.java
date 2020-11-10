package org.openl.rules.lang.xls;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Workbook;
import org.openl.rules.lang.xls.load.WorkbookLoader;
import org.openl.rules.lang.xls.load.WorkbookLoaders;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.openl.util.StringTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XlsWorkbookSourceCodeModule implements IOpenSourceCodeModule {

    private final Logger log = LoggerFactory.getLogger(XlsWorkbookSourceCodeModule.class);

    protected IOpenSourceCodeModule src;

    private final WorkbookLoader workbookLoader;

    private final Set<Short> wbColors = new TreeSet<>();

    private final Collection<XlsWorkbookListener> listeners = new ArrayList<>();

    private Map<String, Object> params;

    public XlsWorkbookSourceCodeModule(IOpenSourceCodeModule src) {
        this(src, WorkbookLoaders.getWorkbookLoader(src));
    }

    public XlsWorkbookSourceCodeModule(IOpenSourceCodeModule src, WorkbookLoader workbookLoader) {
        this.src = src;
        this.workbookLoader = workbookLoader;
    }

    private void initWorkbookColors() {
        if (!(getWorkbook() instanceof HSSFWorkbook)) {
            return;
        }
        Workbook workbook = getWorkbook();
        int numStyles = workbook.getNumCellStyles();
        for (int i = 0; i < numStyles; i++) {
            CellStyle cellStyle = workbook.getCellStyleAt(i);

            wbColors.add(cellStyle.getFillForegroundColor());
            wbColors.add(cellStyle.getFillBackgroundColor());
            wbColors.add(cellStyle.getTopBorderColor());
            wbColors.add(cellStyle.getBottomBorderColor());
            wbColors.add(cellStyle.getLeftBorderColor());
            wbColors.add(cellStyle.getRightBorderColor());
        }

        int numFonts = workbook.getNumberOfFontsAsInt();
        for (int i = 0; i < numFonts; i++) {
            Font font = workbook.getFontAt(i);
            wbColors.add(font.getColor());
        }
    }

    public void addListener(XlsWorkbookListener listener) {
        listeners.add(listener);
    }

    public Collection<XlsWorkbookListener> getListeners() {
        return listeners;
    }

    public String getDisplayName() {
        String uri = StringTool.decodeURL(src.getUri());
        return FileUtils.getName(uri);
    }

    @Override
    public String getUri() {
        return src.getUri();
    }

    public Workbook getWorkbook() {
        return workbookLoader.getWorkbook();
    }

    public WorkbookLoader getWorkbookLoader() {
        return workbookLoader;
    }

    /**
     * Synch object for file accessing. It is necessary to prevent getting isModified info before save operation will be
     * finished.
     */
    private final Object fileAccessLock = new Object();

    public File getSourceFile() {
        synchronized (fileAccessLock) {
            File sourceFile = null;
            try {
                URI uri = new URI(getUri());
                sourceFile = new File(uri);
            } catch (URISyntaxException me) {
                log.debug("Cannot get source file", me);
            }
            return sourceFile;
        }
    }

    public void save() throws IOException {
        File sourceFile = getSourceFile();
        String fileName = sourceFile.getCanonicalPath();
        synchronized (fileAccessLock) {
            for (XlsWorkbookListener wl : listeners) {
                wl.beforeSave(this);
            }

            OutputStream fileOut = new DeferredCreateFileOutputStream(fileName);
            getWorkbook().write(fileOut);
            fileOut.close();

            for (XlsWorkbookListener wl : listeners) {
                wl.afterSave(this);
            }
        }
    }

    public IOpenSourceCodeModule getSource() {
        return src;
    }

    public Set<Short> getWorkbookColors() {
        if (wbColors.isEmpty()) {
            initWorkbookColors();
        }
        return wbColors;
    }

    @Override
    public InputStream getByteStream() {
        return src.getByteStream();
    }

    @Override
    public Reader getCharacterStream() {
        return src.getCharacterStream();
    }

    @Override
    public String getCode() {
        return src.getCode();
    }

    @Override
    public int getStartPosition() {
        return src.getStartPosition();
    }

    @Override
    public Map<String, Object> getParams() {
        return params;
    }

    @Override
    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    /**
     * Avoids rewriting the file before actual write operation is occurred. For example if OutOfMemoryError is thrown
     * before actual write operation begins, the file should not be corrupted.
     *
     * @author NSamatov
     */
    private static final class DeferredCreateFileOutputStream extends OutputStream {
        private final String fileName;

        /**
         * Create deferred output stream.
         *
         * @param fileName the system-dependent file name
         * @throws FileNotFoundException if the file exists but is a directory rather than a regular file, does not
         *             exist but cannot be created, or cannot be opened for any other reason.
         */
        private DeferredCreateFileOutputStream(String fileName) throws FileNotFoundException {
            this.fileName = fileName;
            throwExceptionIfNotWritable(fileName);
        }

        /**
         * Check that file is writable. File should not be rewritten in this method.
         *
         * @param fileName the checking file
         * @throws FileNotFoundException if the file exists but is a directory rather than a regular file, does not
         *             exist but cannot be created, or cannot be opened for any other reason.
         */
        private void throwExceptionIfNotWritable(String fileName) throws FileNotFoundException {
            FileOutputStream os = null;
            try {
                os = new FileOutputStream(fileName, true);
            } finally {
                IOUtils.closeQuietly(os);
            }
        }

        private OutputStream stream;

        private OutputStream getStream() throws FileNotFoundException {
            if (stream == null) {
                stream = new FileOutputStream(fileName);
            }
            return stream;
        }

        /**
         * Invokes the delegate's <code>write(int)</code> method.
         *
         * @param idx the byte to write
         * @throws IOException if an I/O error occurs
         */
        @Override
        public void write(int idx) throws IOException {
            getStream().write(idx);
        }

        /**
         * Invokes the delegate's <code>write(byte[])</code> method.
         *
         * @param bts the bytes to write
         * @throws IOException if an I/O error occurs
         */
        @Override
        public void write(byte[] bts) throws IOException {
            getStream().write(bts);
        }

        /**
         * Invokes the delegate's <code>write(byte[])</code> method.
         *
         * @param bts the bytes to write
         * @param st The start offset
         * @param end The number of bytes to write
         * @throws IOException if an I/O error occurs
         */
        @Override
        public void write(byte[] bts, int st, int end) throws IOException {
            getStream().write(bts, st, end);
        }

        @Override
        public void flush() throws IOException {
            if (stream != null) {
                stream.flush();
            }
        }

        @Override
        public void close() throws IOException {
            if (stream != null) {
                stream.close();
            }
        }
    }
}
