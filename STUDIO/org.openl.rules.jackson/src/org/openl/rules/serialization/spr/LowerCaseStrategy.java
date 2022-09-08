package org.openl.rules.serialization.spr;

public class LowerCaseStrategy extends SpreadsheetResultBeanPropertyNamingStrategyBase {
    @Override
    public String transform(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        return name.toLowerCase();
    }

    @Override
    public String transform(String column, String row) {
        return transform(column + row);
    }
}
