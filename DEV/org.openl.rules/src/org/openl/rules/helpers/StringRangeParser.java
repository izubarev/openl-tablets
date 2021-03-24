package org.openl.rules.helpers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringRangeParser extends ARangeParser<String> {

    public static final String MAX_VALUE = String.valueOf(Character.MAX_VALUE);
    public static final String MIN_VALUE = String.valueOf(Character.MIN_VALUE);

    private static class StringRangeParserHolder {
        private static final StringRangeParser INSTANCE = new StringRangeParser();
    }

    private final RangeParser[] parsers;
    private final Pattern[] patterns;

    private static final String BRACKETS_PATTERN = "\\s*([\\[(])\\s*(\\S*.{0,99}?[^\\\\.])\\s*(?:[-;…]|\\.{3}|\\.{2})\\s*(.{1,100})\\s*([])])\\s*";
    private static final String MIN_MAX_PATTERN = "\\s*(\\S*.{0,99}?[^\\\\.])\\s*([-…]|\\.{3}|\\.{2})\\s*(.{1,100})\\s*";
    private static final String VERBAL_PATTERN = "(.+)(\\+|(?<=\\s)and\\s+more|(?<=\\s)or\\s+less)\\s*";
    private static final String MORE_LESS_PATTERN = "\\s*(<=?|>=?|less\\s+than(?=\\s)|more\\s+than(?=\\s))(.+)";
    private static final String RANGE_MORE_LESS_PATTERN = "\\s*(<=?|>=?)\\s*(.{1,100})\\s*(<=?|>=?)\\s*(.{1,100})\\s*";
    private static final String SIMPLE_PATTERN = "(.+)";

    private StringRangeParser() {
        StringRangeBoundAdapter adapter = new StringRangeBoundAdapter();
        patterns = new Pattern[] { Pattern.compile(BRACKETS_PATTERN),
                Pattern.compile(MIN_MAX_PATTERN),
                Pattern.compile(VERBAL_PATTERN),
                Pattern.compile(RANGE_MORE_LESS_PATTERN),
                Pattern.compile(MORE_LESS_PATTERN),
                Pattern.compile(SIMPLE_PATTERN) };
        parsers = new RangeParser[] { new BracketsParser<>(patterns[0], adapter),
                new MinMaxParser<>(patterns[1], adapter),
                new VerbalParser<>(patterns[2], adapter),
                new RangeWithMoreLessParser<>(patterns[3], adapter),
                new MoreLessParser<>(patterns[4], adapter),
                new SimpleParser<>(patterns[5], adapter) };
    }

    public boolean isStringRange(String value) {
        for (Pattern pattern : patterns) {
            Matcher m = pattern.matcher(value);
            if (m.matches()) {
                return true;
            }
        }
        return false;
    }

    public boolean likelyRangeThanString(String value) {
        for (int i = 0; i < 5; i++) {
            Matcher m = patterns[i].matcher(value);
            if (m.matches()) {
                return true;
            }
        }
        return false;
    }

    public static StringRangeParser getInstance() {
        return StringRangeParserHolder.INSTANCE;
    }

    @Override
    RangeParser[] getRangeParsers() {
        return parsers;
    }

    private static final class StringRangeBoundAdapter implements RangeBoundAdapter<String> {

        @Override
        public String adaptValue(String s) {
            return s != null ? s.trim() : null;
        }

        @Override
        public String getMinLeftBound() {
            return MIN_VALUE;
        }

        @Override
        public String getMaxRightBound() {
            return MAX_VALUE;
        }
    }

}
