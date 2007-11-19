package org.openl.rules.webstudio.util;

import org.openl.util.TreeIterator;
import org.openl.util.OpenIterator;

import java.util.Iterator;
import java.io.File;

public class WebstudioTreeIterator extends TreeIterator {
    public static final String PROPERTIES_FOLDER = ".studioProps";

    public WebstudioTreeIterator(Object treeRoot, int mode) {
        super(treeRoot, new TreeAdaptor(), mode);
    }

    static class TreeAdaptor implements TreeIterator.TreeAdaptor {
        public Iterator children(Object node) {
            File f = (File) node;
            if (!f.isDirectory() || f.getName().equals(PROPERTIES_FOLDER)) {
                return null;
            }
            return OpenIterator.fromArray(f.listFiles());
        }
    }

    public File nextFile()
	{
		return (File)next();
	}

}
