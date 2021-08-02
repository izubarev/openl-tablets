package org.openl.itest.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.util.StreamUtils;
import org.w3c.dom.Node;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.ComparisonResult;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Difference;
import org.xmlunit.diff.DifferenceEvaluator;
import org.xmlunit.diff.DifferenceEvaluators;
import org.xmlunit.diff.ElementSelectors;

import com.fasterxml.jackson.databind.JsonNode;

final class Comparators {

    private static final String CRLF = "\r\n";

    private Comparators() {
    }

    static void txt(String message, byte[] expected, byte[] actual) {
        String regExp = getRegExp(new String(expected, StandardCharsets.UTF_8));
        boolean matches = trimExtraSpaces(new String(actual, StandardCharsets.UTF_8)).matches(regExp);
        if (!matches) {
            fail(message);
        }
    }

    static void xml(String message, Object expected, Object actual) {
        DifferenceEvaluator evaluator = DifferenceEvaluators.chain(DifferenceEvaluators.Default, matchByPattern());
        Iterator<Difference> differences = DiffBuilder.compare(expected)
            .withTest(actual)
            .ignoreWhitespace()
            .checkForSimilar()
            .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndAllAttributes, ElementSelectors.byName))
            .withDifferenceEvaluator(evaluator)
            .build()
            .getDifferences()
            .iterator();
        if (differences.hasNext()) {
            fail(message + "\n" + differences.next());
        }
    }

    private static DifferenceEvaluator matchByPattern() {
        return (comparison, outcome) -> {
            if (outcome == ComparisonResult.DIFFERENT) {
                Node control = comparison.getControlDetails().getTarget();
                Node test = comparison.getTestDetails().getTarget();
                if (control != null && test != null) {
                    String controlValue = control.getNodeValue();
                    String testValue = test.getNodeValue();
                    if (controlValue != null && testValue != null) {
                        String regExp = getRegExp(controlValue);
                        String noSpaces = trimExtraSpaces(testValue);
                        if (noSpaces.equals(regExp) || Pattern.compile(regExp).matcher(noSpaces).matches()) {
                            return ComparisonResult.SIMILAR;
                        }
                    }
                }

                return outcome;
            }
            return outcome;
        };
    }

    private static String trimExtraSpaces(String testValue) {
        return testValue.trim().replaceAll("\\s+", " ");
    }

    private static String getRegExp(String text) {
        return trimExtraSpaces(text).replaceAll("\\\\", "\\\\\\\\")
            .replaceAll("#+", "\\\\d+")
            .replaceAll("@+", "[@\\\\w]+")
            .replaceAll("\\*+", "[^\uFFFF]*");
    }

    static void compareJsonObjects(JsonNode expectedJson, JsonNode actualJson, String path) {
        if (Objects.equals(expectedJson, actualJson)) {
            return;
        }
        if (expectedJson == null || actualJson == null) {
            failDiff(expectedJson, actualJson, path);
        } else if (expectedJson.isTextual()) {
            // try to compare by a pattern
            String regExp = expectedJson.asText()
                .replaceAll("\\[", "\\\\[")
                .replaceAll("]", "\\\\]")
                .replaceAll("#+", "[#\\\\d]+")
                .replaceAll("@+", "[@\\\\w]+")
                .replaceAll("\\*+", "[^\uFFFF]*");
            String actualText = actualJson.isTextual() ? actualJson.asText() : actualJson.toString();
            if (!Pattern.compile(regExp).matcher(actualText).matches()) {
                failDiff(expectedJson, actualJson, path);
            }
        } else if (expectedJson.isArray() && actualJson.isArray()) {
            for (int i = 0; i < expectedJson.size() || i < actualJson.size(); i++) {
                compareJsonObjects(expectedJson.get(i), actualJson.get(i), path + "[" + i + "]");
            }
        } else if (expectedJson.isObject() && actualJson.isObject()) {
            LinkedHashSet<String> names = new LinkedHashSet<>();
            expectedJson.fieldNames().forEachRemaining(names::add);
            actualJson.fieldNames().forEachRemaining(names::add);

            for (String name : names) {
                compareJsonObjects(expectedJson.get(name), actualJson.get(name), path + " > " + name);
            }
        } else {
            failDiff(expectedJson, actualJson, path);
        }
    }

    private static void failDiff(JsonNode expectedJson, JsonNode actualJson, String path) {
        assertEquals("Path: \\" + path, expectedJson, actualJson);
    }

    static void zip(byte[] expectedBytes, byte[] actualBytes) throws IOException {
        final Map<String, byte[]> expectedZipEntries = getZipEntries(expectedBytes);
        final Map<String, byte[]> actualZipEntries = getZipEntries(actualBytes);

        final Iterator<Map.Entry<String, byte[]>> actual = actualZipEntries.entrySet().iterator();
        while (actual.hasNext()) {
            final Map.Entry<String, byte[]> actualEntry = actual.next();
            if (expectedZipEntries.containsKey(actualEntry.getKey())) {
                assertArrayEquals(String.format("Zip entry [%s]: ", actualEntry.getKey()),
                    expectedZipEntries.remove(actualEntry.getKey()),
                    actualEntry.getValue());
                actual.remove();
            }
        }

        boolean failed = false;
        StringBuilder errorMessage = new StringBuilder();
        Function<String, String> tab = s -> "    " + s;
        if (!actualZipEntries.isEmpty()) {
            failed = true;
            errorMessage.append("UNEXPECTED entries:")
                .append(CRLF)
                .append(actualZipEntries.keySet().stream().map(tab).collect(Collectors.joining(CRLF)));
        }
        if (!expectedZipEntries.isEmpty()) {
            if (failed) {
                errorMessage.append(CRLF);
            } else {
                failed = true;
            }
            errorMessage.append("MISSED entries:")
                .append(CRLF)
                .append(expectedZipEntries.keySet().stream().map(tab).collect(Collectors.joining(CRLF)));
        }
        if (failed) {
            fail(errorMessage.toString());
        }
    }

    private static Map<String, byte[]> getZipEntries(byte[] src) throws IOException {
        Map<String, byte[]> dest = new HashMap<>();
        try (ZipInputStream actual = new ZipInputStream(new ByteArrayInputStream(src))) {
            ZipEntry actualEntry;
            while ((actualEntry = actual.getNextEntry()) != null) {
                if (actualEntry.getName().endsWith("/")) {
                    // skip folder
                    continue;
                }
                ByteArrayOutputStream target = new ByteArrayOutputStream();
                StreamUtils.copy(actual, target);
                dest.put(actualEntry.getName(), target.toByteArray());
            }
        }
        return dest;
    }
}
