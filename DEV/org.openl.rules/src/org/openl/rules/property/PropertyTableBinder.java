package org.openl.rules.property;

import org.openl.OpenL;
import org.openl.binding.IMemberBoundNode;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.data.DataNodeBinder;
import org.openl.rules.data.ITable;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.meta.PropertyTableMetaInfoReader;
import org.openl.rules.property.exception.DuplicatedPropertiesTableException;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.properties.TableProperties;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.rules.table.properties.inherit.PropertiesChecker;
import org.openl.util.TableNameChecker;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

/**
 * Binder for property table.
 *
 * @author DLiauchuk
 *
 */
public class PropertyTableBinder extends DataNodeBinder {

    private static final String DEFAULT_TABLE_NAME_PREFIX = "InheritedProperties: ";
    private static final String SCOPE_PROPERTY_NAME = "scope";

    @Override
    public IMemberBoundNode preBind(TableSyntaxNode tsn,
            OpenL openl,
            RulesModuleBindingContext bindingContext,
            XlsModuleOpenClass module) throws Exception {

        PropertyTableBoundNode propertyNode = (PropertyTableBoundNode) makeNode(tsn, module, bindingContext);

        IdentifierNode identifierNode = parseHeader(tsn);
        String tableName = identifierNode == null ? null : identifierNode.getIdentifier();
        propertyNode.setTableName(tableName);
        if (identifierNode == null) {
            tableName = DEFAULT_TABLE_NAME_PREFIX + tsn.getUri();
        } else {
            tableName = identifierNode.getIdentifier();
            if (TableNameChecker.isInvalidJavaIdentifier(tableName)) {
                String message = "Property table " + tableName + TableNameChecker.NAME_ERROR_MESSAGE;
                bindingContext.addMessage(OpenLMessagesUtils.newWarnMessage(message, identifierNode));
            }
        }
        ITable propertyTable = module.getDataBase().registerTable(tableName, tsn);
        IOpenClass propertiesClass = JavaOpenClass.getOpenClass(TableProperties.class);
        ILogicalTable propTableBody = getTableBody(tsn);

        processTable(module, propertyTable, propTableBody, tableName, propertiesClass, bindingContext, openl, false);

        TableProperties propertiesInstance = ((TableProperties[]) propertyTable.getDataArray())[0];
        propertiesInstance.setPropertiesSection(tsn.getTable().getRows(1)); // Skip header
        propertiesInstance.setCurrentTableType(tsn.getType());
        PropertiesChecker.checkProperties(bindingContext,
            propertiesInstance.getAllProperties().keySet(),
            tsn,
            InheritanceLevel.getEnumByValue(propertiesInstance.getPropertyValueAsString(SCOPE_PROPERTY_NAME)));
        tsn.setTableProperties(propertiesInstance);

        analysePropertiesNode(tsn, propertiesInstance, bindingContext);

        propertyNode.setPropertiesInstance(propertiesInstance);

        return propertyNode;
    }

    /**
     * Parses table header. Consider that second token is the name of the table. <br>
     * <b>e.g.: Properties [tableName].</b>
     *
     * @param tsn <code>{@link TableSyntaxNode}</code>
     * @return identifier node with name if exists.
     */
    private IdentifierNode parseHeader(TableSyntaxNode tsn) throws Exception {
        IOpenSourceCodeModule src = tsn.getHeader().getModule();

        IdentifierNode[] parsedHeader = Tokenizer.tokenize(src, " \n\r");

        if (parsedHeader.length > 1) {
            return parsedHeader[1];
        }

        return null;
    }

    /**
     * Checks if current property table is a module level property or a category level. Adds it to
     * <code>{@link RulesModuleBindingContext}</code>. <br>
     * If module level properties already exists, or there are properties for the category with the same name throws an
     * <code>{@link DuplicatedPropertiesTableException}</code>.
     *
     * @param tableSyntaxNode <code>{@link TableSyntaxNode}</code>.
     * @param propertiesInstance <code>{@link TableProperties}</code>.
     * @param bindingContext <code>{@link RulesModuleBindingContext}</code>.
     * @throws DuplicatedPropertiesTableException if module level properties already exists, or there are properties for
     *             the category with the same name.
     */
    private void analysePropertiesNode(TableSyntaxNode tableSyntaxNode,
            TableProperties propertiesInstance,
            RulesModuleBindingContext bindingContext) throws SyntaxNodeException {

        String scope = propertiesInstance.getScope();

        if (scope != null) {
            if (isModuleProperties(scope)) {
                processModuleProperties(tableSyntaxNode, bindingContext);
            } else if (isCategoryProperties(scope)) {
                processCategoryProperties(tableSyntaxNode, propertiesInstance, bindingContext);
            } else if (isGlobalProperties(scope)) {
                processGlobalProperties(tableSyntaxNode, bindingContext);
            } else {
                String message = String.format("Value of the property '%s' is neither '%s', '%s' or '%s'.",
                    SCOPE_PROPERTY_NAME,
                    InheritanceLevel.GLOBAL.getDisplayName(),
                    InheritanceLevel.MODULE.getDisplayName(),
                    InheritanceLevel.CATEGORY.getDisplayName());

                throw SyntaxNodeExceptionUtils.createError(message, tableSyntaxNode);
            }
        } else {
            String message = String.format("There is no obligatory property '%s'.", SCOPE_PROPERTY_NAME);

            throw SyntaxNodeExceptionUtils.createError(message, tableSyntaxNode);
        }
    }

    private void processCategoryProperties(TableSyntaxNode tableSyntaxNode,
            TableProperties propertiesInstance,
            RulesModuleBindingContext bindingContext) throws SyntaxNodeException {

        String category = getCategoryToApplyProperties(tableSyntaxNode, propertiesInstance);
        String key = RulesModuleBindingContext.CATEGORY_PROPERTIES_KEY + category;

        if (!bindingContext.isTableSyntaxNodePresented(key)) {
            bindingContext.registerTableSyntaxNode(key, tableSyntaxNode);
        } else {
            String message = String.format("Properties for category '%s' already exists.", category);

            throw new DuplicatedPropertiesTableException(message, null, tableSyntaxNode);
        }
    }

    private void processGlobalProperties(TableSyntaxNode tableSyntaxNode,
            RulesModuleBindingContext bindingContext) throws SyntaxNodeException {
        String key = RulesModuleBindingContext.GLOBAL_PROPERTIES_KEY;
        if (!bindingContext.isTableSyntaxNodePresented(key)) {
            bindingContext.registerTableSyntaxNode(key, tableSyntaxNode);
        } else {
            throw new DuplicatedPropertiesTableException("Global properties already exist.", null, tableSyntaxNode);
        }
    }

    private void processModuleProperties(TableSyntaxNode tableSyntaxNode,
            RulesModuleBindingContext bindingContext) throws SyntaxNodeException {

        String key = RulesModuleBindingContext.MODULE_PROPERTIES_KEY;

        if (!bindingContext.isTableSyntaxNodePresented(key)) {
            bindingContext.registerTableSyntaxNode(key, tableSyntaxNode);
        } else {
            XlsWorkbookSourceCodeModule module = ((XlsSheetSourceCodeModule) tableSyntaxNode.getModule())
                .getWorkbookSource();
            String moduleName = module.getDisplayName();
            String message = String.format("Properties for module '%s' already exists", moduleName);

            throw new DuplicatedPropertiesTableException(message, null, tableSyntaxNode);
        }
    }

    /**
     * Find out the name of the category to apply properties for.
     *
     * @param tsn <code>{@link TableSyntaxNode}</code>
     * @param properties <code>{@link TableProperties}</code>
     * @return the name of the category to apply properties for.
     */
    private String getCategoryToApplyProperties(TableSyntaxNode tsn, TableProperties properties) {

        String category = properties.getCategory();

        if (category != null) {
            return category;
        } else {
            return ((XlsSheetSourceCodeModule) tsn.getModule()).getSheetName();
        }
    }

    private boolean isModuleProperties(String scope) {
        return InheritanceLevel.MODULE.getDisplayName().equals(scope);
    }

    private boolean isCategoryProperties(String scope) {
        return InheritanceLevel.CATEGORY.getDisplayName().equals(scope);
    }

    private boolean isGlobalProperties(String scope) {
        return InheritanceLevel.GLOBAL.getDisplayName().equals(scope);
    }

    @Override
    protected ATableBoundNode makeNode(TableSyntaxNode tsn,
            XlsModuleOpenClass module,
            RulesModuleBindingContext bindingContext) {
        PropertyTableBoundNode boundNode = new PropertyTableBoundNode(tsn);

        if (!bindingContext.isExecutionMode()) {
            tsn.setMetaInfoReader(new PropertyTableMetaInfoReader(boundNode));
        }

        return boundNode;
    }

}
