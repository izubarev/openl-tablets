package org.openl.rules.webstudio.web.test;

import javax.servlet.http.HttpSession;

import org.openl.base.INamedThing;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.rules.data.IDataBase;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

import java.util.Collection;

public final class Utils {
    private Utils() {
    }

    private static final String INPUT_ARGS_PARAMETER = "inputArgsParam";

    static boolean isCollection(IOpenClass openClass) {
        return openClass.getAggregateInfo() != null && openClass.getAggregateInfo().isAggregate(openClass);
    }

    public static String displayNameForCollection(IOpenClass collectionType, boolean isEmpty) {
        StringBuilder builder = new StringBuilder();
        if (isEmpty) {
            builder.append("Empty ");
        }
        builder.append("Collection of ");
        builder.append(collectionType.getComponentClass().getDisplayName(INamedThing.SHORT));
        return builder.toString();
    }

    static TestUnitsResults[] runTests(String id, String testRanges, HttpSession session) {
        TestUnitsResults[] results;
        ProjectModel model = WebStudioUtils.getWebStudio(session).getModel();

        IOpenLTable table = model.getTableById(id);
        if (table != null) {
            String uri = table.getUri();
            IOpenMethod method = model.getMethod(uri);

            if (method instanceof TestSuiteMethod) {
                TestSuiteMethod testSuiteMethod = (TestSuiteMethod) method;

                TestSuite testSuite;
                if (testRanges == null) {
                    // Run all test cases of selected test suite
                    testSuite = new TestSuite(testSuiteMethod);
                } else {
                    // Run only selected test cases of selected test suite
                    int[] indices = testSuiteMethod.getIndices(testRanges);
                    testSuite = new TestSuite(testSuiteMethod, indices);
                }
                // Concrete test with cases
                results = new TestUnitsResults[1];
                results[0] = model.runTest(testSuite);
            } else {
                // All tests for table
                IOpenMethod[] tests = model.getTestMethods(uri);
                results = runAllTests(model, tests);
            }
        } else {
            // All tests for project
            IOpenMethod[] tests = model.getAllTestMethods();
            results = runAllTests(model, tests);
        }
        return results;
    }

    private static TestUnitsResults[] runAllTests(ProjectModel model, IOpenMethod[] tests) {
        Collection<OpenLMessage> messages = model.getModuleMessages();
        Collection<OpenLMessage> errors = OpenLMessagesUtils.filterMessagesBySeverity(messages, Severity.ERROR);
        if (tests != null) {
            TestUnitsResults[] results = new TestUnitsResults[tests.length];
            for (int i = 0; i < tests.length; i++) {
                TestSuiteMethod testSuiteMethod = (TestSuiteMethod) tests[i];
                TestSuite testSuite = new TestSuite(testSuiteMethod);
                TestUnitsResults testUnitsResults;
                if (hasError(errors, testSuiteMethod)) {
                    testUnitsResults = new TestUnitsResults(testSuite);
                    testUnitsResults.setTestedRulesHaveErrors(true);
                } else {
                    testUnitsResults = model.runTest(testSuite);
                }
                results[i] = testUnitsResults;
            }
            return results;
        }
        return new TestUnitsResults[0];
    }

    private static boolean hasError(Collection<OpenLMessage> errors, TestSuiteMethod testSuiteMethod) {
        IOpenMethod testedMethod = testSuiteMethod.getTestedMethod();
        IMemberMetaInfo metaInfo = testedMethod.getInfo();
        XlsUrlParser testedMethodUrl = new XlsUrlParser(metaInfo.getSourceUrl());

        for (OpenLMessage msg : errors) {
            String sourceLocation = msg.getSourceLocation();
            if (sourceLocation != null && new XlsUrlParser(sourceLocation).intersects(testedMethodUrl)) {
                return true;
            }
        }
        return false;
    }

    public static IDataBase getDb(ProjectModel model) {
        if (model == null) {
            return null;
        }

        IOpenClass moduleClass = model.getCompiledOpenClass().getOpenClassWithErrors();
        if (moduleClass instanceof XlsModuleOpenClass) {
            return ((XlsModuleOpenClass) moduleClass).getDataBase();
        }

        return null;
    }

    public static void saveTestToSession(HttpSession session, TestSuite testSuite) {
        session.setAttribute(Utils.INPUT_ARGS_PARAMETER, testSuite);
    }

    public static TestSuite pollTestFromSession(HttpSession session) {
        TestSuite suite = (TestSuite) session.getAttribute(Utils.INPUT_ARGS_PARAMETER);
        session.removeAttribute(Utils.INPUT_ARGS_PARAMETER);
        return suite;
    }
}
