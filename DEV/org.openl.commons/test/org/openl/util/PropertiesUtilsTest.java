package org.openl.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

public class PropertiesUtilsTest {

    @Test
    public void loadEmpty() throws IOException {
        ArrayList<String> result = new ArrayList<>();
        PropertiesUtils.load(new StringReader(""), (k, v) -> result.add(k + "=" + v));
        Assert.assertEquals(Arrays.asList(), result);
    }

    @Test
    public void loadEmpty2() throws IOException {
        ArrayList<String> result = new ArrayList<>();
        PropertiesUtils.load(new StringReader("\n    asdad  asdsa \n asd\r asdssa\r\n  #x:y\r\n\r\n sas  sad"), (k, v) -> result.add(k + "=" + v));
        Assert.assertEquals(Arrays.asList(), result);
    }

    @Test
    public void loadComments() throws IOException {
        ArrayList<String> result = new ArrayList<>();
        PropertiesUtils.load(new StringReader("#com=1\\\nx = 2\\\r\n#34 "), (k, v) -> result.add(k + "=" + v));
        Assert.assertEquals(Arrays.asList("x=2#34 "), result);
    }

    @Test
    public void loadTheSame() throws IOException {
        ArrayList<String> result = new ArrayList<>();
        PropertiesUtils.load(new StringReader("x=1\nx : 2 : 3 = 4 \r   \t\f\n\r \t\fx=3\n  \u1111:\u2222\\"), (k, v) -> result.add(k + "=" + v));
        Assert.assertEquals(Arrays.asList("x=1", "x=2 : 3 = 4 ", "x=3", "\u1111=\u2222"), result);
    }

    @Test
    public void loadSpecSymbols() throws IOException {
        ArrayList<String> result = new ArrayList<>();
        PropertiesUtils.load(new StringReader("\tx\t\f \\r\\n\\u0035y#$.,%^&@!+-*/w\\:t+-*/+-_)(*&^%$#@!~`<>,.{}[] = \t\fx\t\f\\t\\f\\n\\r\\u0034+-*/=+-_)(*&^%$#@!~`<>,.{}[]1"), (k, v) -> result.add(k + "=" + v));
        Assert.assertEquals(Arrays.asList("x\t\f \r\n5y#$.,%^&@!+-*/w:t+-*/+-_)(*&^%$#@!~`<>,.{}[]=x\t\f\t\f\n\r\u0034+-*/=+-_)(*&^%$#@!~`<>,.{}[]1"), result);
    }

    @Test
    public void load() throws IOException {
        ArrayList<String> result = new ArrayList<>();
        PropertiesUtils.load(new StringReader("x=1\n\ry=2\r\nz=3 \\"), (k, v) -> result.add(k + "=" + v));
        Assert.assertEquals(Arrays.asList("x=1", "y=2", "z=3 "), result);
    }

    @Test(expected = EOFException.class)
    public void failNotFullUnicode() throws IOException {
        ArrayList<String> result = new ArrayList<>();
        PropertiesUtils.load(new StringReader("x=1\n\ry=\\u123"), (k, v) -> result.add(k + "=" + v));
    }

    @Test
    public void loadStream() throws IOException {
        ArrayList<String> result = new ArrayList<>();
        try (var file = Thread.currentThread().getContextClassLoader().getResourceAsStream("test-utf8.properties")) {
            PropertiesUtils.load(file, (k, v) -> result.add(k + "=" + v));
        }
        Assert.assertEquals(Arrays.asList("Привет! Это проверка=Пройдено!", "#=#", "hello!=passed ! \r  # not a comment"), result);
    }
}
