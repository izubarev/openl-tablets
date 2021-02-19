package org.openl.rules.table.properties.def;

import org.openl.message.Severity;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.table.constraints.Constraints;
import org.openl.rules.table.properties.def.TablePropertyDefinition.SystemValuePolicy;
import org.openl.rules.table.properties.expressions.match.MatchingExpression;
import org.openl.rules.table.properties.inherit.InheritanceLevel;

/**
 * Definitions of supported properties.
 *
 * @author snshor Created Jul 21, 2009
 *
 */
public final class DefaultPropertyDefinitions {
    private static final TablePropertyDefinition[] definitions;

    private DefaultPropertyDefinitions() {
    }

    static {
        // <<< INSERT TablePropertiesDefinition >>>
        definitions = new TablePropertyDefinition[41];

        definitions[0] = new TablePropertyDefinition();
        definitions[0].setConstraints(new Constraints("unique in:module"));
        definitions[0].setDeprecation("removed");
        definitions[0].setDescription("Deprecated. The name of the table displayed in OpenL Tablets");
        definitions[0].setDimensional(false);
        definitions[0].setDisplayName("Name");
        definitions[0].setErrorSeverity(Severity.WARN);
        definitions[0].setGroup("Info");
        definitions[0].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.TABLE });
        definitions[0].setName("name");
        definitions[0].setPrimaryKey(false);
        definitions[0].setSecurityFilter("no");
        definitions[0].setSystem(false);
        definitions[0].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));

        definitions[1] = new TablePropertyDefinition();
        definitions[1].setConstraints(new Constraints("no"));
        definitions[1].setDescription("The category of the table. For a two-level category use the <category>-<subcategory> format.");
        definitions[1].setDimensional(false);
        definitions[1].setDisplayName("Category");
        definitions[1].setGroup("Info");
        definitions[1].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.CATEGORY, InheritanceLevel.TABLE });
        definitions[1].setName("category");
        definitions[1].setPrimaryKey(false);
        definitions[1].setSecurityFilter("yes (coma separated filter specification by user role: category/role pairs)");
        definitions[1].setSystem(false);
        definitions[1].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));

        definitions[2] = new TablePropertyDefinition();
        definitions[2].setConstraints(new Constraints("no"));
        definitions[2].setDescription("A name of a user created the table in OpenL Tablets WebStudio");
        definitions[2].setDimensional(false);
        definitions[2].setDisplayName("Created By");
        definitions[2].setGroup("Info");
        definitions[2].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.TABLE });
        definitions[2].setName("createdBy");
        definitions[2].setPrimaryKey(false);
        definitions[2].setSecurityFilter("no");
        definitions[2].setSystem(true);
        definitions[2].setSystemValueDescriptor("currentUser");
        definitions[2].setSystemValuePolicy(SystemValuePolicy.IF_BLANK_ONLY);
        definitions[2].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));

        definitions[3] = new TablePropertyDefinition();
        definitions[3].setConstraints(new Constraints("no"));
        definitions[3].setDescription("Date of the table creation in OpenL Tablets WebStudio");
        definitions[3].setDimensional(false);
        definitions[3].setDisplayName("Created On");
        definitions[3].setFormat("MM/dd/yyyy hh:mm a");
        definitions[3].setGroup("Info");
        definitions[3].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.TABLE });
        definitions[3].setName("createdOn");
        definitions[3].setPrimaryKey(false);
        definitions[3].setSecurityFilter("no");
        definitions[3].setSystem(true);
        definitions[3].setSystemValueDescriptor("currentDate");
        definitions[3].setSystemValuePolicy(SystemValuePolicy.IF_BLANK_ONLY);
        definitions[3].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.util.Date.class));

        definitions[4] = new TablePropertyDefinition();
        definitions[4].setConstraints(new Constraints("no"));
        definitions[4].setDescription("A name of a user last modified the table in OpenL Tablets WebStudio");
        definitions[4].setDimensional(false);
        definitions[4].setDisplayName("Modified By");
        definitions[4].setGroup("Info");
        definitions[4].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.TABLE });
        definitions[4].setName("modifiedBy");
        definitions[4].setPrimaryKey(false);
        definitions[4].setSecurityFilter("no");
        definitions[4].setSystem(true);
        definitions[4].setSystemValueDescriptor("currentUser");
        definitions[4].setSystemValuePolicy(SystemValuePolicy.ON_EACH_EDIT);
        definitions[4].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));

        definitions[5] = new TablePropertyDefinition();
        definitions[5].setConstraints(new Constraints("no"));
        definitions[5].setDescription("The date of the last table modification in OpenL Tablets WebStudio");
        definitions[5].setDimensional(false);
        definitions[5].setDisplayName("Modified On");
        definitions[5].setFormat("MM/dd/yyyy hh:mm a");
        definitions[5].setGroup("Info");
        definitions[5].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.TABLE });
        definitions[5].setName("modifiedOn");
        definitions[5].setPrimaryKey(false);
        definitions[5].setSecurityFilter("no");
        definitions[5].setSystem(true);
        definitions[5].setSystemValueDescriptor("currentDate");
        definitions[5].setSystemValuePolicy(SystemValuePolicy.ON_EACH_EDIT);
        definitions[5].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.util.Date.class));

        definitions[6] = new TablePropertyDefinition();
        definitions[6].setConstraints(new Constraints("no"));
        definitions[6].setDescription("Any additional information to clarify the use of the table");
        definitions[6].setDimensional(false);
        definitions[6].setDisplayName("Description");
        definitions[6].setGroup("Info");
        definitions[6].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.TABLE });
        definitions[6].setName("description");
        definitions[6].setPrimaryKey(false);
        definitions[6].setSecurityFilter("no");
        definitions[6].setSystem(false);
        definitions[6].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));

        definitions[7] = new TablePropertyDefinition();
        definitions[7].setConstraints(new Constraints("no"));
        definitions[7].setDescription("Comma separated tags to be used for search, navigation, etc");
        definitions[7].setDimensional(false);
        definitions[7].setDisplayName("Tags");
        definitions[7].setGroup("Info");
        definitions[7].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.TABLE });
        definitions[7].setName("tags");
        definitions[7].setPrimaryKey(false);
        definitions[7].setSecurityFilter("no");
        definitions[7].setSystem(false);
        definitions[7].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String[].class));

        definitions[8] = new TablePropertyDefinition();
        definitions[8].setConstraints(new Constraints("< expirationDate"));
        definitions[8].setDescription("The starting date of the time interval within which the rule table is active");
        definitions[8].setDimensional(true);
        definitions[8].setDisplayName("Effective Date");
        definitions[8].setExpression(new MatchingExpression("le(currentDate)"));
        definitions[8].setFormat("MM/dd/yyyy hh:mm a");
        definitions[8].setGroup("Business Dimension");
        definitions[8].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE });
        definitions[8].setName("effectiveDate");
        definitions[8].setPrimaryKey(true);
        definitions[8].setSecurityFilter("no");
        definitions[8].setSystem(false);
        definitions[8].setTableType(new XlsNodeTypes[] { XlsNodeTypes.XLS_DT, XlsNodeTypes.XLS_SPREADSHEET, XlsNodeTypes.XLS_TBASIC, XlsNodeTypes.XLS_COLUMN_MATCH, XlsNodeTypes.XLS_METHOD, XlsNodeTypes.XLS_PROPERTIES });
        definitions[8].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.util.Date.class));

        definitions[9] = new TablePropertyDefinition();
        definitions[9].setConstraints(new Constraints("> effectiveDate"));
        definitions[9].setDescription("The end date after which the rule table becomes inactive");
        definitions[9].setDimensional(true);
        definitions[9].setDisplayName("Expiration Date");
        definitions[9].setExpression(new MatchingExpression("ge(currentDate)"));
        definitions[9].setFormat("MM/dd/yyyy hh:mm a");
        definitions[9].setGroup("Business Dimension");
        definitions[9].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE });
        definitions[9].setName("expirationDate");
        definitions[9].setPrimaryKey(false);
        definitions[9].setSecurityFilter("no");
        definitions[9].setSystem(false);
        definitions[9].setTableType(new XlsNodeTypes[] { XlsNodeTypes.XLS_DT, XlsNodeTypes.XLS_SPREADSHEET, XlsNodeTypes.XLS_TBASIC, XlsNodeTypes.XLS_COLUMN_MATCH, XlsNodeTypes.XLS_METHOD, XlsNodeTypes.XLS_PROPERTIES });
        definitions[9].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.util.Date.class));

        definitions[10] = new TablePropertyDefinition();
        definitions[10].setConstraints(new Constraints("< endRequestDate"));
        definitions[10].setDescription("The starting date when rules are available for usage in production");
        definitions[10].setDimensional(true);
        definitions[10].setDisplayName("Start Request Date");
        definitions[10].setExpression(new MatchingExpression("le(requestDate)"));
        definitions[10].setFormat("MM/dd/yyyy hh:mm a");
        definitions[10].setGroup("Business Dimension");
        definitions[10].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE });
        definitions[10].setName("startRequestDate");
        definitions[10].setPrimaryKey(true);
        definitions[10].setSecurityFilter("no");
        definitions[10].setSystem(false);
        definitions[10].setTableType(new XlsNodeTypes[] { XlsNodeTypes.XLS_DT, XlsNodeTypes.XLS_SPREADSHEET, XlsNodeTypes.XLS_TBASIC, XlsNodeTypes.XLS_COLUMN_MATCH, XlsNodeTypes.XLS_METHOD, XlsNodeTypes.XLS_PROPERTIES });
        definitions[10].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.util.Date.class));

        definitions[11] = new TablePropertyDefinition();
        definitions[11].setConstraints(new Constraints("> startRequestDate"));
        definitions[11].setDescription("The last date when rules are available for usage in production");
        definitions[11].setDimensional(true);
        definitions[11].setDisplayName("End Request Date");
        definitions[11].setExpression(new MatchingExpression("ge(requestDate)"));
        definitions[11].setFormat("MM/dd/yyyy hh:mm a");
        definitions[11].setGroup("Business Dimension");
        definitions[11].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE });
        definitions[11].setName("endRequestDate");
        definitions[11].setPrimaryKey(false);
        definitions[11].setSecurityFilter("no");
        definitions[11].setSystem(false);
        definitions[11].setTableType(new XlsNodeTypes[] { XlsNodeTypes.XLS_DT, XlsNodeTypes.XLS_SPREADSHEET, XlsNodeTypes.XLS_TBASIC, XlsNodeTypes.XLS_COLUMN_MATCH, XlsNodeTypes.XLS_METHOD, XlsNodeTypes.XLS_PROPERTIES });
        definitions[11].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.util.Date.class));

        definitions[12] = new TablePropertyDefinition();
        definitions[12].setDescription("Canada region(s) of operation for which the table should be used");
        definitions[12].setDimensional(true);
        definitions[12].setDisplayName("Canada Region");
        definitions[12].setExpression(new MatchingExpression("contains(caRegion)"));
        definitions[12].setGroup("Business Dimension");
        definitions[12].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE });
        definitions[12].setName("caRegions");
        definitions[12].setPrimaryKey(false);
        definitions[12].setSecurityFilter("no");
        definitions[12].setSystem(false);
        definitions[12].setTableType(new XlsNodeTypes[] { XlsNodeTypes.XLS_DT, XlsNodeTypes.XLS_SPREADSHEET, XlsNodeTypes.XLS_TBASIC, XlsNodeTypes.XLS_COLUMN_MATCH, XlsNodeTypes.XLS_METHOD, XlsNodeTypes.XLS_PROPERTIES });
        definitions[12].setType(org.openl.types.java.JavaOpenClass.getOpenClass(org.openl.rules.enumeration.CaRegionsEnum[].class));

        definitions[13] = new TablePropertyDefinition();
        definitions[13].setDescription("Canada province for which the table should be used");
        definitions[13].setDimensional(true);
        definitions[13].setDisplayName("Canada Province");
        definitions[13].setExpression(new MatchingExpression("contains(caProvince)"));
        definitions[13].setGroup("Business Dimension");
        definitions[13].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE });
        definitions[13].setName("caProvinces");
        definitions[13].setPrimaryKey(false);
        definitions[13].setSecurityFilter("no");
        definitions[13].setSystem(false);
        definitions[13].setTableType(new XlsNodeTypes[] { XlsNodeTypes.XLS_DT, XlsNodeTypes.XLS_SPREADSHEET, XlsNodeTypes.XLS_TBASIC, XlsNodeTypes.XLS_COLUMN_MATCH, XlsNodeTypes.XLS_METHOD, XlsNodeTypes.XLS_PROPERTIES });
        definitions[13].setType(org.openl.types.java.JavaOpenClass.getOpenClass(org.openl.rules.enumeration.CaProvincesEnum[].class));

        definitions[14] = new TablePropertyDefinition();
        definitions[14].setDescription("Countrie(s) for which the table works and should be used");
        definitions[14].setDimensional(true);
        definitions[14].setDisplayName("Countries");
        definitions[14].setExpression(new MatchingExpression("contains(country)"));
        definitions[14].setGroup("Business Dimension");
        definitions[14].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE });
        definitions[14].setName("country");
        definitions[14].setPrimaryKey(false);
        definitions[14].setSecurityFilter("yes (coma separated filter specification by user role: category/role pairs)");
        definitions[14].setSystem(false);
        definitions[14].setTableType(new XlsNodeTypes[] { XlsNodeTypes.XLS_DT, XlsNodeTypes.XLS_SPREADSHEET, XlsNodeTypes.XLS_TBASIC, XlsNodeTypes.XLS_COLUMN_MATCH, XlsNodeTypes.XLS_METHOD, XlsNodeTypes.XLS_PROPERTIES });
        definitions[14].setType(org.openl.types.java.JavaOpenClass.getOpenClass(org.openl.rules.enumeration.CountriesEnum[].class));

        definitions[15] = new TablePropertyDefinition();
        definitions[15].setDescription("Economic Region(s) for which the table works and should be used");
        definitions[15].setDimensional(true);
        definitions[15].setDisplayName("Region");
        definitions[15].setExpression(new MatchingExpression("contains(region)"));
        definitions[15].setGroup("Business Dimension");
        definitions[15].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE });
        definitions[15].setName("region");
        definitions[15].setPrimaryKey(false);
        definitions[15].setSecurityFilter("yes (coma separated filter specification by user role: category/role pairs)");
        definitions[15].setSystem(false);
        definitions[15].setTableType(new XlsNodeTypes[] { XlsNodeTypes.XLS_DT, XlsNodeTypes.XLS_SPREADSHEET, XlsNodeTypes.XLS_TBASIC, XlsNodeTypes.XLS_COLUMN_MATCH, XlsNodeTypes.XLS_METHOD, XlsNodeTypes.XLS_PROPERTIES });
        definitions[15].setType(org.openl.types.java.JavaOpenClass.getOpenClass(org.openl.rules.enumeration.RegionsEnum[].class));

        definitions[16] = new TablePropertyDefinition();
        definitions[16].setDescription("Currencie(s) for which the table works and should be used");
        definitions[16].setDimensional(true);
        definitions[16].setDisplayName("Currency");
        definitions[16].setExpression(new MatchingExpression("contains(currency)"));
        definitions[16].setGroup("Business Dimension");
        definitions[16].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE });
        definitions[16].setName("currency");
        definitions[16].setPrimaryKey(false);
        definitions[16].setSecurityFilter("yes (coma separated filter specification by user role: category/role pairs)");
        definitions[16].setSystem(false);
        definitions[16].setTableType(new XlsNodeTypes[] { XlsNodeTypes.XLS_DT, XlsNodeTypes.XLS_SPREADSHEET, XlsNodeTypes.XLS_TBASIC, XlsNodeTypes.XLS_COLUMN_MATCH, XlsNodeTypes.XLS_METHOD, XlsNodeTypes.XLS_PROPERTIES });
        definitions[16].setType(org.openl.types.java.JavaOpenClass.getOpenClass(org.openl.rules.enumeration.CurrenciesEnum[].class));

        definitions[17] = new TablePropertyDefinition();
        definitions[17].setDescription("Language(s) for which this table works and should be used");
        definitions[17].setDimensional(true);
        definitions[17].setDisplayName("Language");
        definitions[17].setExpression(new MatchingExpression("contains(lang)"));
        definitions[17].setGroup("Business Dimension");
        definitions[17].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE });
        definitions[17].setName("lang");
        definitions[17].setPrimaryKey(false);
        definitions[17].setSecurityFilter("yes (coma separated filter specification by user role: category/role pairs)");
        definitions[17].setSystem(false);
        definitions[17].setTableType(new XlsNodeTypes[] { XlsNodeTypes.XLS_DT, XlsNodeTypes.XLS_SPREADSHEET, XlsNodeTypes.XLS_TBASIC, XlsNodeTypes.XLS_COLUMN_MATCH, XlsNodeTypes.XLS_METHOD, XlsNodeTypes.XLS_PROPERTIES });
        definitions[17].setType(org.openl.types.java.JavaOpenClass.getOpenClass(org.openl.rules.enumeration.LanguagesEnum[].class));

        definitions[18] = new TablePropertyDefinition();
        definitions[18].setConstraints(new Constraints("list: Defined by method getLob()"));
        definitions[18].setDescription("LOB (line of business) for which this table works and should be used");
        definitions[18].setDimensional(true);
        definitions[18].setDisplayName("LOB");
        definitions[18].setExpression(new MatchingExpression("contains(lob)"));
        definitions[18].setGroup("Business Dimension");
        definitions[18].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE });
        definitions[18].setName("lob");
        definitions[18].setPrimaryKey(false);
        definitions[18].setSecurityFilter("yes (coma separated filter specification by user role: category/role pairs)");
        definitions[18].setSystem(false);
        definitions[18].setTableType(new XlsNodeTypes[] { XlsNodeTypes.XLS_DT, XlsNodeTypes.XLS_SPREADSHEET, XlsNodeTypes.XLS_TBASIC, XlsNodeTypes.XLS_COLUMN_MATCH, XlsNodeTypes.XLS_METHOD, XlsNodeTypes.XLS_PROPERTIES });
        definitions[18].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String[].class));

        definitions[19] = new TablePropertyDefinition();
        definitions[19].setDescription("Indicates origin of the rules to allow hierarchy of more generic and more specific rules");
        definitions[19].setDimensional(true);
        definitions[19].setDisplayName("Origin");
        definitions[19].setGroup("Business Dimension");
        definitions[19].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE });
        definitions[19].setName("origin");
        definitions[19].setPrimaryKey(false);
        definitions[19].setSecurityFilter("no");
        definitions[19].setSystem(false);
        definitions[19].setTableType(new XlsNodeTypes[] { XlsNodeTypes.XLS_DT, XlsNodeTypes.XLS_SPREADSHEET, XlsNodeTypes.XLS_TBASIC, XlsNodeTypes.XLS_COLUMN_MATCH, XlsNodeTypes.XLS_METHOD, XlsNodeTypes.XLS_PROPERTIES });
        definitions[19].setType(org.openl.types.java.JavaOpenClass.getOpenClass(org.openl.rules.enumeration.OriginsEnum.class));

        definitions[20] = new TablePropertyDefinition();
        definitions[20].setDescription("US region(s) for which the table works and should be used");
        definitions[20].setDimensional(true);
        definitions[20].setDisplayName("US Region");
        definitions[20].setExpression(new MatchingExpression("contains(usRegion)"));
        definitions[20].setGroup("Business Dimension");
        definitions[20].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE });
        definitions[20].setName("usregion");
        definitions[20].setPrimaryKey(false);
        definitions[20].setSecurityFilter("yes (coma separated filter specification by user role: category/role pairs)");
        definitions[20].setSystem(false);
        definitions[20].setTableType(new XlsNodeTypes[] { XlsNodeTypes.XLS_DT, XlsNodeTypes.XLS_SPREADSHEET, XlsNodeTypes.XLS_TBASIC, XlsNodeTypes.XLS_COLUMN_MATCH, XlsNodeTypes.XLS_METHOD, XlsNodeTypes.XLS_PROPERTIES });
        definitions[20].setType(org.openl.types.java.JavaOpenClass.getOpenClass(org.openl.rules.enumeration.UsRegionsEnum[].class));

        definitions[21] = new TablePropertyDefinition();
        definitions[21].setDescription("US State(s) for which this table works and should be used");
        definitions[21].setDimensional(true);
        definitions[21].setDisplayName("US States");
        definitions[21].setExpression(new MatchingExpression("contains(usState)"));
        definitions[21].setGroup("Business Dimension");
        definitions[21].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE });
        definitions[21].setName("state");
        definitions[21].setPrimaryKey(false);
        definitions[21].setSecurityFilter("yes (coma separated filter specification by user role: category/role pairs)");
        definitions[21].setSystem(false);
        definitions[21].setTableType(new XlsNodeTypes[] { XlsNodeTypes.XLS_DT, XlsNodeTypes.XLS_SPREADSHEET, XlsNodeTypes.XLS_TBASIC, XlsNodeTypes.XLS_COLUMN_MATCH, XlsNodeTypes.XLS_METHOD, XlsNodeTypes.XLS_PROPERTIES });
        definitions[21].setType(org.openl.types.java.JavaOpenClass.getOpenClass(org.openl.rules.enumeration.UsStatesEnum[].class));

        definitions[22] = new TablePropertyDefinition();
        definitions[22].setConstraints(new Constraints("NN.NN[.NN]"));
        definitions[22].setDescription("Defines a version of this table. The “version” should be different for each table with the same signature and business dimensional properties values");
        definitions[22].setDimensional(false);
        definitions[22].setDisplayName("Version");
        definitions[22].setGroup("Version");
        definitions[22].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.TABLE });
        definitions[22].setName("version");
        definitions[22].setPrimaryKey(false);
        definitions[22].setSystem(false);
        definitions[22].setTableType(new XlsNodeTypes[] { XlsNodeTypes.XLS_DT, XlsNodeTypes.XLS_SPREADSHEET, XlsNodeTypes.XLS_TBASIC, XlsNodeTypes.XLS_COLUMN_MATCH, XlsNodeTypes.XLS_METHOD });
        definitions[22].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));

        definitions[23] = new TablePropertyDefinition();
        definitions[23].setConstraints(new Constraints("unique in:TableGroup"));
        definitions[23].setDefaultValue("true");
        definitions[23].setDescription("Indicates if the current table version is active or not");
        definitions[23].setDimensional(false);
        definitions[23].setDisplayName("Active");
        definitions[23].setGroup("Version");
        definitions[23].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.TABLE });
        definitions[23].setName("active");
        definitions[23].setPrimaryKey(false);
        definitions[23].setSystem(false);
        definitions[23].setTableType(new XlsNodeTypes[] { XlsNodeTypes.XLS_DT, XlsNodeTypes.XLS_SPREADSHEET, XlsNodeTypes.XLS_TBASIC, XlsNodeTypes.XLS_COLUMN_MATCH, XlsNodeTypes.XLS_METHOD });
        definitions[23].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.Boolean.class));

        definitions[24] = new TablePropertyDefinition();
        definitions[24].setConstraints(new Constraints("unique in:module&regexp:([a-zA-Z_][a-zA-Z0-9_]*)"));
        definitions[24].setDescription("Unique ID to be used for calling the rule table");
        definitions[24].setDimensional(false);
        definitions[24].setDisplayName("ID");
        definitions[24].setErrorSeverity(Severity.ERROR);
        definitions[24].setGroup("Dev");
        definitions[24].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.TABLE });
        definitions[24].setName("id");
        definitions[24].setPrimaryKey(false);
        definitions[24].setSecurityFilter("no");
        definitions[24].setSystem(false);
        definitions[24].setTableType(new XlsNodeTypes[] { XlsNodeTypes.XLS_DT, XlsNodeTypes.XLS_SPREADSHEET, XlsNodeTypes.XLS_TBASIC, XlsNodeTypes.XLS_COLUMN_MATCH, XlsNodeTypes.XLS_METHOD });
        definitions[24].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));

        definitions[25] = new TablePropertyDefinition();
        definitions[25].setConstraints(new Constraints("one of: common, vocabulary[N], main[N]"));
        definitions[25].setDescription("The property to be used for managing dependencies between build phases");
        definitions[25].setDimensional(false);
        definitions[25].setDisplayName("Build Phase");
        definitions[25].setGroup("Dev");
        definitions[25].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE });
        definitions[25].setName("buildPhase");
        definitions[25].setPrimaryKey(false);
        definitions[25].setSecurityFilter("no");
        definitions[25].setSystem(false);
        definitions[25].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));

        definitions[26] = new TablePropertyDefinition();
        definitions[26].setDescription("On/Off validation mode for the rule table");
        definitions[26].setDimensional(false);
        definitions[26].setDisplayName("Validate DT");
        definitions[26].setGroup("Dev");
        definitions[26].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE });
        definitions[26].setName("validateDT");
        definitions[26].setPrimaryKey(false);
        definitions[26].setSecurityFilter("no");
        definitions[26].setSystem(false);
        definitions[26].setTableType(new XlsNodeTypes[] { XlsNodeTypes.XLS_DT, XlsNodeTypes.XLS_PROPERTIES });
        definitions[26].setType(org.openl.types.java.JavaOpenClass.getOpenClass(org.openl.rules.enumeration.ValidateDTEnum.class));

        definitions[27] = new TablePropertyDefinition();
        definitions[27].setDefaultValue("false");
        definitions[27].setDescription("Defines whether to raise an error in case no rules are matched");
        definitions[27].setDimensional(false);
        definitions[27].setDisplayName("Fail On Miss");
        definitions[27].setGroup("Dev");
        definitions[27].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE });
        definitions[27].setName("failOnMiss");
        definitions[27].setPrimaryKey(false);
        definitions[27].setSystem(false);
        definitions[27].setTableType(new XlsNodeTypes[] { XlsNodeTypes.XLS_DT, XlsNodeTypes.XLS_PROPERTIES });
        definitions[27].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.Boolean.class));

        definitions[28] = new TablePropertyDefinition();
        definitions[28].setConstraints(new Constraints("Worksheet, Workbook, Module"));
        definitions[28].setDescription("The scope for a properties table");
        definitions[28].setDimensional(false);
        definitions[28].setDisplayName("Scope");
        definitions[28].setGroup("Dev");
        definitions[28].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.GLOBAL, InheritanceLevel.MODULE, InheritanceLevel.CATEGORY });
        definitions[28].setName("scope");
        definitions[28].setPrimaryKey(false);
        definitions[28].setSystem(false);
        definitions[28].setTableType(new XlsNodeTypes[] { XlsNodeTypes.XLS_PROPERTIES });
        definitions[28].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));

        definitions[29] = new TablePropertyDefinition();
        definitions[29].setConstraints(new Constraints("Worksheet, Workbook, Module"));
        definitions[29].setDefaultValue("0");
        definitions[29].setDescription("The priority for for global properties");
        definitions[29].setDimensional(false);
        definitions[29].setDisplayName("Priority");
        definitions[29].setGroup("Dev");
        definitions[29].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.GLOBAL });
        definitions[29].setName("priority");
        definitions[29].setPrimaryKey(false);
        definitions[29].setSystem(false);
        definitions[29].setTableType(new XlsNodeTypes[] { XlsNodeTypes.XLS_PROPERTIES });
        definitions[29].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.Integer.class));

        definitions[30] = new TablePropertyDefinition();
        definitions[30].setConstraints(new Constraints("regexp:([a-zA-Z_]{1}[a-zA-Z0-9_]*(\\.[a-zA-Z_]{1}[a-zA-Z0-9_]*)*)"));
        definitions[30].setDefaultValue("org.openl.generated.beans");
        definitions[30].setDescription("The name of the package for datatype generation");
        definitions[30].setDimensional(false);
        definitions[30].setDisplayName("Datatype Package");
        definitions[30].setGroup("Dev");
        definitions[30].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.MODULE, InheritanceLevel.TABLE });
        definitions[30].setName("datatypePackage");
        definitions[30].setPrimaryKey(false);
        definitions[30].setSystem(false);
        definitions[30].setTableType(new XlsNodeTypes[] { XlsNodeTypes.XLS_DATATYPE, XlsNodeTypes.XLS_PROPERTIES });
        definitions[30].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));

        definitions[31] = new TablePropertyDefinition();
        definitions[31].setConstraints(new Constraints("regexp:([a-zA-Z_]{1}[a-zA-Z0-9_]*(\\.[a-zA-Z_]{1}[a-zA-Z0-9_]*)*)"));
        definitions[31].setDefaultValue("org.openl.generated.spreadsheetresults");
        definitions[31].setDescription("The name of the package for spreadsheet result beans generation");
        definitions[31].setDimensional(false);
        definitions[31].setDisplayName("Spreadsheet Result Package");
        definitions[31].setGroup("Dev");
        definitions[31].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.GLOBAL });
        definitions[31].setName("spreadsheetResultPackage");
        definitions[31].setPrimaryKey(false);
        definitions[31].setSystem(false);
        definitions[31].setTableType(new XlsNodeTypes[] { XlsNodeTypes.XLS_PROPERTIES });
        definitions[31].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));

        definitions[32] = new TablePropertyDefinition();
        definitions[32].setDescription("Defines whether or not to use cache while recalculating the table for a variation, depending on the rule input");
        definitions[32].setDimensional(false);
        definitions[32].setDisplayName("Cacheable");
        definitions[32].setGroup("Dev");
        definitions[32].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE });
        definitions[32].setName("cacheable");
        definitions[32].setPrimaryKey(false);
        definitions[32].setSystem(false);
        definitions[32].setTableType(new XlsNodeTypes[] { XlsNodeTypes.XLS_DT, XlsNodeTypes.XLS_SPREADSHEET, XlsNodeTypes.XLS_TBASIC, XlsNodeTypes.XLS_COLUMN_MATCH, XlsNodeTypes.XLS_METHOD, XlsNodeTypes.XLS_PROPERTIES });
        definitions[32].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.Boolean.class));

        definitions[33] = new TablePropertyDefinition();
        definitions[33].setDescription("The way of recalculation of the table for a variation - slightly varied input parameter(s)");
        definitions[33].setDimensional(false);
        definitions[33].setDisplayName("Recalculate");
        definitions[33].setGroup("Dev");
        definitions[33].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE });
        definitions[33].setName("recalculate");
        definitions[33].setPrimaryKey(false);
        definitions[33].setSystem(false);
        definitions[33].setTableType(new XlsNodeTypes[] { XlsNodeTypes.XLS_DT, XlsNodeTypes.XLS_SPREADSHEET, XlsNodeTypes.XLS_TBASIC, XlsNodeTypes.XLS_COLUMN_MATCH, XlsNodeTypes.XLS_METHOD, XlsNodeTypes.XLS_PROPERTIES });
        definitions[33].setType(org.openl.types.java.JavaOpenClass.getOpenClass(org.openl.rules.enumeration.RecalculateEnum.class));

        definitions[34] = new TablePropertyDefinition();
        definitions[34].setDefaultValue("SKIP");
        definitions[34].setDescription("The way of processing result calculation of the DT table");
        definitions[34].setDimensional(false);
        definitions[34].setDisplayName("Empty Result Processing");
        definitions[34].setGroup("Dev");
        definitions[34].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE });
        definitions[34].setName("emptyResultProcessing");
        definitions[34].setPrimaryKey(false);
        definitions[34].setSystem(false);
        definitions[34].setTableType(new XlsNodeTypes[] { XlsNodeTypes.XLS_DT, XlsNodeTypes.XLS_PROPERTIES });
        definitions[34].setType(org.openl.types.java.JavaOpenClass.getOpenClass(org.openl.rules.enumeration.DTEmptyResultProcessingEnum.class));

        definitions[35] = new TablePropertyDefinition();
        definitions[35].setConstraints(new Constraints("regexp:(-?[0-9]+)"));
        definitions[35].setDescription("Precision of comparing the returned results with the expected ones");
        definitions[35].setDimensional(false);
        definitions[35].setDisplayName("Precision");
        definitions[35].setGroup("Dev");
        definitions[35].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE });
        definitions[35].setName("precision");
        definitions[35].setPrimaryKey(false);
        definitions[35].setSystem(false);
        definitions[35].setTableType(new XlsNodeTypes[] { XlsNodeTypes.XLS_TEST_METHOD, XlsNodeTypes.XLS_PROPERTIES });
        definitions[35].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));

        definitions[36] = new TablePropertyDefinition();
        definitions[36].setDefaultValue("false");
        definitions[36].setDescription("Controls generation additional properties with table structure details in an output model");
        definitions[36].setDimensional(false);
        definitions[36].setDisplayName("Table Structure Details");
        definitions[36].setGroup("Dev");
        definitions[36].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE });
        definitions[36].setName("tableStructureDetails");
        definitions[36].setPrimaryKey(false);
        definitions[36].setSecurityFilter("no");
        definitions[36].setSystem(false);
        definitions[36].setTableType(new XlsNodeTypes[] { XlsNodeTypes.XLS_SPREADSHEET, XlsNodeTypes.XLS_PROPERTIES });
        definitions[36].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.Boolean.class));

        definitions[37] = new TablePropertyDefinition();
        definitions[37].setDefaultValue("true");
        definitions[37].setDescription("Controls new Spreadsheet Auto Type Discovery feature.");
        definitions[37].setDimensional(false);
        definitions[37].setDisplayName("Auto Type Discovery");
        definitions[37].setGroup("Dev");
        definitions[37].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE });
        definitions[37].setName("autoType");
        definitions[37].setPrimaryKey(false);
        definitions[37].setSecurityFilter("no");
        definitions[37].setSystem(false);
        definitions[37].setTableType(new XlsNodeTypes[] { XlsNodeTypes.XLS_SPREADSHEET, XlsNodeTypes.XLS_PROPERTIES });
        definitions[37].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.Boolean.class));

        definitions[38] = new TablePropertyDefinition();
        definitions[38].setDefaultValue("true");
        definitions[38].setDescription("If true calculates all cells in the Spreadsheet, otherwise calculates only cells these are requred for a result. By default = true.");
        definitions[38].setDimensional(false);
        definitions[38].setDisplayName("Calculate All Cells");
        definitions[38].setGroup("Dev");
        definitions[38].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE });
        definitions[38].setName("calculateAllCells");
        definitions[38].setPrimaryKey(false);
        definitions[38].setSecurityFilter("no");
        definitions[38].setSystem(false);
        definitions[38].setTableType(new XlsNodeTypes[] { XlsNodeTypes.XLS_SPREADSHEET, XlsNodeTypes.XLS_PROPERTIES });
        definitions[38].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.Boolean.class));

        definitions[39] = new TablePropertyDefinition();
        definitions[39].setDefaultValue("false");
        definitions[39].setDescription("Controls parallel execution feature. By default = false.");
        definitions[39].setDimensional(false);
        definitions[39].setDisplayName("Concurrent Execution");
        definitions[39].setGroup("Dev");
        definitions[39].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE });
        definitions[39].setName("parallel");
        definitions[39].setPrimaryKey(false);
        definitions[39].setSecurityFilter("no");
        definitions[39].setSystem(false);
        definitions[39].setTableType(new XlsNodeTypes[] { XlsNodeTypes.XLS_DT, XlsNodeTypes.XLS_SPREADSHEET, XlsNodeTypes.XLS_TBASIC, XlsNodeTypes.XLS_COLUMN_MATCH, XlsNodeTypes.XLS_METHOD, XlsNodeTypes.XLS_PROPERTIES });
        definitions[39].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.Boolean.class));

        definitions[40] = new TablePropertyDefinition();
        definitions[40].setConstraints(new Constraints("list: Defined by method getNature()"));
        definitions[40].setDescription("Nature (type) for which this table works and should be used");
        definitions[40].setDimensional(true);
        definitions[40].setDisplayName("Nature");
        definitions[40].setExpression(new MatchingExpression("eq(nature)"));
        definitions[40].setGroup("Business Dimension");
        definitions[40].setInheritanceLevel(new InheritanceLevel[] { InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE });
        definitions[40].setName("nature");
        definitions[40].setPrimaryKey(false);
        definitions[40].setSecurityFilter("yes (coma separated filter specification by user role: category/role pairs)");
        definitions[40].setSystem(false);
        definitions[40].setTableType(new XlsNodeTypes[] { XlsNodeTypes.XLS_DT, XlsNodeTypes.XLS_SPREADSHEET, XlsNodeTypes.XLS_TBASIC, XlsNodeTypes.XLS_COLUMN_MATCH, XlsNodeTypes.XLS_METHOD, XlsNodeTypes.XLS_PROPERTIES });
        definitions[40].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));
        // <<< END INSERT TablePropertiesDefinition >>>
    }

    public static TablePropertyDefinition[] getDefaultDefinitions() {
        return definitions;
    }

}
