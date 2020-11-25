package org.openl.rules.ui.tree;

import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.rules.ui.IProjectTypes;
import org.openl.util.StringUtils;

/**
 * Builder for module properties table.
 *
 * @author DLiauchuk
 *
 */
public class ModulePropertiesTableNodeBuilder extends BaseTableTreeNodeBuilder {

    private static final String FOLDER_NAME = "Module Properties";

    @Override
    public String[] getDisplayValue(Object nodeObject, int i) {
        return new String[] { FOLDER_NAME, FOLDER_NAME, FOLDER_NAME };
    }

    @Override
    public String getType(Object nodeObject) {
        return IProjectTypes.PT_TABLE_GROUP;
    }

    @Override
    public String getUrl(Object nodeObject) {
        TableSyntaxNode tableSyntaxNode = (TableSyntaxNode) nodeObject;
        return tableSyntaxNode.getUri();
    }

    @Override
    public boolean isBuilderApplicableForObject(TableSyntaxNode tableSyntaxNode) {
        return XlsNodeTypes.XLS_PROPERTIES.toString()
            .equals(tableSyntaxNode.getType()) && isModulePropertyTable(tableSyntaxNode);
    }

    @Override
    public ProjectTreeNode makeNode(TableSyntaxNode tableSyntaxNode, int i) {
        String folderName = FOLDER_NAME;
        return makeFolderNode(folderName);
    }

    public static boolean isModulePropertyTable(TableSyntaxNode tableSyntaxNode) {
        boolean result = false;
        ITableProperties tableProperties = tableSyntaxNode.getTableProperties();
        if (tableProperties != null) {
            String propValue = tableProperties.getScope();
            if (StringUtils.isNotEmpty(propValue) && InheritanceLevel.MODULE.getDisplayName().equals(propValue)) {
                result = true;
            }
        }
        return result;
    }

    private ProjectTreeNode makeFolderNode(String folderName) {
        return new ProjectTreeNode(new String[] { folderName, folderName, folderName },
            IProjectTypes.PT_FOLDER,
            null,
                null);
    }

}
