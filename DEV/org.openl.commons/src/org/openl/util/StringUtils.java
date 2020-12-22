package org.openl.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utils to manipulate with strings.
 *
 * @author Yury Molchan
 */
public class StringUtils {

    public static final String[] EMPTY_STRING_ARRAY = new String[0];
    public static final String EMPTY = "";

    /**
     * <p>
     * Splits the provided string into an array of trimmed strings, separator specified. The separator is not included
     * in the returned String array. Adjacent separators are treated as one separator.
     * </p>
     * <p>
     * A {@code null} input String returns {@code null}.
     * </p>
     *
     * <pre>
     * StringUtils.split(null, *)         = null
     * StringUtils.split("", *)           = []
     * StringUtils.split("a.b.c", '.')    = ["a", "b", "c"]
     * StringUtils.split("a..b.c", '.')   = ["a", "b", "c"]
     * StringUtils.split(" a b:c ", '.')  = ["a b:c"]
     * StringUtils.split("a b c", ' ')    = ["a", "b", "c"]
     * StringUtils.split(" a, b, c", ',') = ["a", "b", "c"]
     * </pre>
     *
     * @param str the String to parse, may be null
     * @param separator the character used as the delimiter
     * @return an array of parsed Strings, {@code null} if null String input
     */
    public static String[] split(final String str, final char separator) {
        return splitWorker(str, ch -> ch == separator, true);
    }

    /**
     * <p>
     * Splits the provided text into an array, using whitespace as the separator. Whitespace is defined by
     * {@link Character#isWhitespace(char)}.
     * </p>
     * <p>
     * The separator is not included in the returned String array. Adjacent separators are treated as one separator. For
     * more control over the split use the StrTokenizer class.
     * </p>
     * <p>
     * A {@code null} input String returns {@code null}.
     * </p>
     *
     * <pre>
     * StringUtils.split(null)         = null
     * StringUtils.split("")           = []
     * StringUtils.split("abc def")    = ["abc", "def"]
     * StringUtils.split("abc  def")   = ["abc", "def"]
     * StringUtils.split(" abc ")      = ["abc"]
     * StringUtils.split(" abc def ")  = ["abc", "def"]
     * </pre>
     *
     * @param str the String to parse, may be null
     * @return an array of parsed Strings, {@code null} if null String input
     */
    public static String[] split(final String str) {
        return splitWorker(str, Character::isWhitespace, false);
    }

    private static String[] splitWorker(final String str, final Predicate<Character> tester, boolean trim) {
        if (str == null) {
            return null;
        }
        final int len = str.length();
        if (len == 0) {
            return EMPTY_STRING_ARRAY;
        }
        final List<String> list = new ArrayList<>();
        int i = 0, start = 0, end = 0;
        boolean match = false;
        while (i < len) {
            char ch = str.charAt(i++);
            if (tester.test(ch)) {
                if (match) {
                    list.add(str.substring(start, end));
                    match = false;
                }
                start = i;
            } else if (trim && Character.isWhitespace(ch)) {
                if (!match) {
                    start = i;
                }
            } else {
                match = true;
                end = i;
            }
        }
        if (match) {
            list.add(str.substring(start, end));
        }
        return list.toArray(new String[0]);
    }

    /**
     * <p>
     * Splits the provided string into an array of trimmed strings, separated by new line character codes: {@code '\n'}
     * and {@code '\r'}. Blank lines are omitted from the result.
     * </p>
     * <p>
     * A blank or {@code null} input String returns {@code null}.
     * </p>
     *
     * <pre>
     * StringUtils.toLines(null)                 = null
     * StringUtils.toLines("")                   = null
     * StringUtils.toLines("   ")                = null
     * StringUtils.toLines("  abc  ")            = ["abc"]
     * StringUtils.toLines("a b c")              = ["a b c"]
     * StringUtils.toLines("a\rb\nc")            = ["a", "b", "c"]
     * StringUtils.toLines("\r a\n\t\r \n c \n") = ["a", "c"]
     * </pre>
     *
     * @param text the String to parse, may be null
     * @return an array of parsed Strings, {@code null} if blank String input
     */
    public static String[] toLines(final String text) {
        if (isBlank(text)) {
            return null;
        }
        // Trim and split by one of the
        return text.trim().split("\\s*[\r\n]\\s*");
    }

    /**
     * <p>
     * Joins the elements of the provided array into a single String containing the provided list of elements.
     * </p>
     * <p>
     * No delimiter is added before or after the list. A {@code null} separator is the same as an empty String ("").
     * Null objects or empty strings within the array are represented by empty strings.
     * </p>
     *
     * <pre>
     * StringUtils.join(null, *)                = null
     * StringUtils.join([], *)                  = ""
     * StringUtils.join([null], *)              = "null"
     * StringUtils.join(["a", "b", "c"], "--")  = "a--b--c"
     * StringUtils.join(["a", "b", "c"], "")    = "abc"
     * StringUtils.join([null, "", "a"], ',')   = "null,,a"
     * </pre>
     *
     * @param values the array of values to join together, may be null
     * @param separator the separator character to use, null treated as ""
     * @return the joined String, {@code null} if null array input
     */
    public static String join(Object[] values, String separator) {
        if (values == null) {
            return null;
        }
        return Arrays.stream(values).map(String::valueOf).collect(Collectors.joining(separator));
    }

    /**
     * <p>
     * Checks if a CharSequence is empty ("") or null.
     * </p>
     *
     * <pre>
     * StringUtils.isEmpty(null)      = true
     * StringUtils.isEmpty("")        = true
     * StringUtils.isEmpty(" ")       = false
     * StringUtils.isEmpty("boo")     = false
     * StringUtils.isEmpty("  boo  ") = false
     * </pre>
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is empty or null
     */
    public static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    /**
     * <p>
     * Checks if a CharSequence is not empty ("") and not null.
     * </p>
     *
     * <pre>
     * StringUtils.isNotEmpty(null)      = false
     * StringUtils.isNotEmpty("")        = false
     * StringUtils.isNotEmpty(" ")       = true
     * StringUtils.isNotEmpty("boo")     = true
     * StringUtils.isNotEmpty("  boo  ") = true
     * </pre>
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is not empty and not null
     */
    public static boolean isNotEmpty(final CharSequence cs) {
        return !isEmpty(cs);
    }

    /**
     * <p>
     * Checks if a CharSequence is whitespace, empty ("") or null.
     * </p>
     *
     * <pre>
     * StringUtils.isBlank(null)      = true
     * StringUtils.isBlank("")        = true
     * StringUtils.isBlank(" ")       = true
     * StringUtils.isBlank("boo")     = false
     * StringUtils.isBlank("  boo  ") = false
     * </pre>
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is null, empty or whitespace
     */
    public static boolean isBlank(final CharSequence cs) {
        if (isEmpty(cs)) {
            return true;
        }
        int strLen = cs.length();
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>
     * Checks if a CharSequence is not empty (""), not null and not whitespace only.
     * </p>
     *
     * <pre>
     * StringUtils.isNotBlank(null)      = false
     * StringUtils.isNotBlank("")        = false
     * StringUtils.isNotBlank(" ")       = false
     * StringUtils.isNotBlank("boo")     = true
     * StringUtils.isNotBlank("  boo  ") = true
     * </pre>
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is not empty and not null and not whitespace
     */
    public static boolean isNotBlank(final CharSequence cs) {
        return !isBlank(cs);
    }

    /**
     * <p>
     * Checks if String contains a search String irrespective of case, handling {@code null}. Case-insensitivity is
     * defined as by {@link String#equalsIgnoreCase(String)}.
     * </p>
     * <p>
     * A {@code null} String will return {@code false}.
     * </p>
     *
     * <pre>
     * StringUtils.contains(null, *)     = false
     * StringUtils.contains(*, null)     = false
     * StringUtils.contains("", "")      = true
     * StringUtils.contains("abc", "")   = true
     * StringUtils.contains("abc", "a")  = true
     * StringUtils.contains("abc", "Bc") = true
     * StringUtils.contains("abc", "z")  = false
     * StringUtils.contains("abc", "Z")  = false
     * </pre>
     *
     * @param str the String to check, may be null
     * @param searchStr the String to find, may be null
     * @return true if the String contains the search String irrespective of case or false if not or {@code null} string
     *         input
     */
    public static boolean containsIgnoreCase(final String str, final String searchStr) {
        if (str == null || searchStr == null) {
            return false;
        }
        final int len = searchStr.length();
        final int end = str.length() - len;
        for (int i = 0; i <= end; i++) {
            if (str.regionMatches(true, i, searchStr, 0, len)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Do the same as {@link java.lang.String#matches(String)}
     *
     * @param regex a Pattern to which this string is to be matched
     * @param input an input string to match regexp Pattern
     * @return {@code true} if, and only if, this string matches the given regular expression
     */
    public static boolean matches(Pattern regex, CharSequence input) {
        Matcher m = regex.matcher(input);
        return m.matches();
    }

    /**
     * <p>
     * Removes control characters (char &lt;= 32 or (&gt;= 127 and &lt;= 160) from both ends of this String, handling
     * {@code null} by returning {@code null}.
     * </p>
     *
     * <pre>
     * StringUtils.trim(null)          = null
     * StringUtils.trim("")            = ""
     * StringUtils.trim("     ")       = ""
     * StringUtils.trim("boo")         = "boo"
     * StringUtils.trim("    boo    ") = "boo"
     * </pre>
     *
     * @param str the String to be trimmed, may be null
     * @return the trimmed string, {@code null} if null String input
     */
    public static String trim(final String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        int start = 0;
        while (start < str.length() && isSpaceOrControl(str.charAt(start))) {
            start++;
        }

        int end = str.length();
        while (start < end && isSpaceOrControl(str.charAt(end - 1))) {
            end--;
        }

        return (start > 0 || end < str.length()) ? str.substring(start, end) : str;
    }

    /**
     * Determines if the specified character is whitespace or control character (char &lt;= 32 or (&gt;= 127 and &lt;=
     * 160)
     *
     * @param ch the character to be tested
     * @return {@code true} if character is whitespace or control, otherwise {@code false}
     */
    public static boolean isSpaceOrControl(char ch) {
        return Character.isWhitespace(ch) || Character.isISOControl(ch) || Character.isSpaceChar(ch);
    }

    /**
     * <p>
     * Removes control characters (char &lt;= 32 or (&gt;= 127 and &lt;= 160) from both ends of this String returning
     * {@code null} if the String is empty ("") after the trim or if it is {@code null}.
     * </p>
     *
     * <pre>
     * StringUtils.trimToNull(null)          = null
     * StringUtils.trimToNull("")            = null
     * StringUtils.trimToNull("     ")       = null
     * StringUtils.trimToNull("boo")         = "boo"
     * StringUtils.trimToNull("    boo    ") = "boo"
     * </pre>
     *
     * @param str the String to be trimmed, may be null
     * @return the trimmed String, {@code null} if only chars &lt;= 32, empty or null String input
     */
    public static String trimToNull(final String str) {
        final String ts = trim(str);
        return isEmpty(ts) ? null : ts;
    }

    /**
     * <p>
     * Removes control characters (char &lt;= 32 or (&gt;= 127 and &lt;= 160) from both ends of this String returning an
     * empty String ("") if the String is empty ("") after the trim or if it is {@code null}.
     * </p>
     *
     * <pre>
     * StringUtils.trimToEmpty(null)          = ""
     * StringUtils.trimToEmpty("")            = ""
     * StringUtils.trimToEmpty("     ")       = ""
     * StringUtils.trimToEmpty("boo")         = "boo"
     * StringUtils.trimToEmpty("    boo    ") = "boo"
     * </pre>
     *
     * @param str the String to be trimmed, may be null
     * @return the trimmed String, or an empty String if {@code null} input
     */
    public static String trimToEmpty(final String str) {
        return str == null ? EMPTY : trim(str);
    }

    /**
     * <p>
     * Capitalizes a String changing the first letter to title case as per {@link Character#toTitleCase(char)}. No other
     * letters are changed.
     * </p>
     * <p>
     * A {@code null} input String returns {@code null}.
     * </p>
     *
     * <pre>
     * StringUtils.capitalize(null)  = null
     * StringUtils.capitalize("")    = ""
     * StringUtils.capitalize("foo") = "Foo"
     * StringUtils.capitalize("fOo") = "FOo"
     * </pre>
     *
     * @param str the String to capitalize, may be null
     * @return the capitalized String, {@code null} if null String input
     * @see #uncapitalize(String)
     */
    public static String capitalize(final String str) {
        if (isEmpty(str)) {
            return str;
        }

        char firstChar = str.charAt(0);
        if (Character.isTitleCase(firstChar)) {
            // already capitalized
            return str;
        }

        return Character.toTitleCase(firstChar) + str.substring(1);
    }

    /**
     * <p>
     * Uncapitalizes a String changing the first letter to title case as per {@link Character#toLowerCase(char)}. No
     * other letters are changed.
     * </p>
     * <p>
     * A {@code null} input String returns {@code null}.
     * </p>
     *
     * <pre>
     * StringUtils.uncapitalize(null)  = null
     * StringUtils.uncapitalize("")    = ""
     * StringUtils.uncapitalize("Foo") = "foo"
     * StringUtils.uncapitalize("FOO") = "fOO"
     * </pre>
     *
     * @param str the String to uncapitalize, may be null
     * @return the uncapitalized String, {@code null} if null String input
     * @see #capitalize(String)
     */
    public static String uncapitalize(final String str) {
        if (isEmpty(str)) {
            return str;
        }

        char firstChar = str.charAt(0);
        if (Character.isLowerCase(firstChar)) {
            // already uncapitalized
            return str;
        }

        return Character.toLowerCase(firstChar) + str.substring(1);
    }

    /**
     * Converts CamelCased string to kebab-cased. It is useful for converting Java's fields or methods to case
     * insensetive form, e.g. for properties keys, urls, MS Windows file names and e.t.c.
     *
     * <pre>
     * StringUtils.camelToKebab(null)        = null
     * StringUtils.camelToKebab("")          = ""
     * StringUtils.camelToKebab("FOO")       = "foo"
     * StringUtils.camelToKebab("Foo")       = "foo"
     * StringUtils.camelToKebab("foo")       = "foo"
     * StringUtils.camelToKebab("FooBar")    = "foo-bar"
     * StringUtils.camelToKebab("fooBar")    = "foo-bar"
     * StringUtils.camelToKebab("FOOBar")    = "foo-bar"
     * StringUtils.camelToKebab("ABar")      = "a-bar"
     * StringUtils.camelToKebab("aBar")      = "a-bar"
     * StringUtils.camelToKebab("aBAR")      = "a-bar"
     * </pre>
     *
     *
     * @param camel - CamelCased string
     * @return - kebab-cased string
     */
    public static String camelToKebab(String camel) {
        if (isEmpty(camel)) {
            return camel;
        }
        int length = camel.length();
        StringBuilder sb = new StringBuilder(length + (length / 4) + 1); // Every 4th symbol expected to be Upper cased
        for (int i = 0; i < length; i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0 && (i < (length - 1) && Character.isLowerCase(camel.charAt(i + 1)) || Character
                        .isLowerCase(camel.charAt(i - 1)))) {
                    sb.append("-");
                }
                c = Character.toLowerCase(c);
                sb.append(c);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
