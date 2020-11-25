package org.openl.rules.ui.tree;

import org.openl.rules.lang.xls.TableSyntaxNodeUtils;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.ui.IProjectTypes;
import org.openl.rules.webstudio.WebStudioFormats;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.MethodKey;

/**
 * Builds tree node for table instance.
 *
 */
public class TableInstanceTreeNodeBuilder extends OpenMethodsGroupTreeNodeBuilder {

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getDisplayValue(Object sorterObject, int i) {

        TableSyntaxNode tsn = (TableSyntaxNode) sorterObject;

        return TableSyntaxNodeUtils.getTableDisplayValue(tsn, i, getOpenMethodGroupsDictionary(),
            WebStudioFormats.getInstance());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType(Object sorterObject) {

        TableSyntaxNode tsn = (TableSyntaxNode) sorterObject;

        return String.format("%s.%s", IProjectTypes.PT_TABLE, tsn.getType()).intern();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUrl(Object sorterObject) {

        TableSyntaxNode tsn = (TableSyntaxNode) sorterObject;

        return tsn.getUri();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUnique(TableSyntaxNode tsn) {
        return XlsNodeTypes.XLS_PROPERTIES.toString().equals(tsn.getType()) || XlsNodeTypes.XLS_DATATYPE.toString()
            .equals(tsn.getType()) || XlsNodeTypes.XLS_DATA.toString()
                .equals(tsn.getType()) || XlsNodeTypes.XLS_TEST_METHOD.toString()
                    .equals(tsn.getType()) || XlsNodeTypes.XLS_ENVIRONMENT.toString()
                        .equals(tsn.getType()) || XlsNodeTypes.XLS_OTHER.toString().equals(tsn.getType()) // These
        // tables
        // don't have
        // versions
        // and cannot
        // be grouped
                || tsn.getMember() == null; // When table contains syntax errors and cannot be grouped with other
                                            // tables.
    }

    @Override
    public Comparable<?> makeKey(TableSyntaxNode tableSyntaxNode) {
        if (tableSyntaxNode.getMember() instanceof IOpenMethod) {

            MethodKey methodKey = new MethodKey((IOpenMethod) tableSyntaxNode.getMember());

            String keyString = methodKey.toString();

            Object nodeObject = makeObject(tableSyntaxNode);

            String[] displayNames = getDisplayValue(tableSyntaxNode, 0);
            for (int i = 0; i < displayNames.length; i++) {
                displayNames[i] += keyString;
            }
            return new NodeKey(getWeight(nodeObject), displayNames);
        } else {
            return super.makeKey(tableSyntaxNode);
        }
    }

    @Override
    public ProjectTreeNode makeNode(TableSyntaxNode tableSyntaxNode, int i) {
        Object nodeObject = makeObject(tableSyntaxNode);
        String[] displayNames = getDisplayValue(nodeObject, 0);
        // it seems we need to process only those tables that have properties that are using for version sorting.
        // in other case return original tableSyntaxNode.
        // ???
        // author: DLiauchuk
        return new VersionedTreeNode(displayNames, tableSyntaxNode);
    }
}
