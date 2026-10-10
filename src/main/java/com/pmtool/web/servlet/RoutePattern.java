package com.pmtool.web.servlet;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RoutePattern {

    private final String patternString;
    private final Pattern regex;
    private final List<String> parameterNames = new ArrayList<>();

    public RoutePattern(String patternString) {
        this.patternString = patternString;

        StringBuilder regexBuilder = new StringBuilder("^");
        Matcher m = Pattern.compile("\\{([A-Za-z0-9_]+)\\}").matcher(patternString);
        int lastEnd = 0;
        while (m.find()) {
            regexBuilder.append(Pattern.quote(patternString.substring(lastEnd, m.start())));
            regexBuilder.append("([^/]+)");
            parameterNames.add(m.group(1));
            lastEnd = m.end();
        }
        regexBuilder.append(Pattern.quote(patternString.substring(lastEnd)));
        regexBuilder.append("$");

        this.regex = Pattern.compile(regexBuilder.toString());
    }

    public boolean matches(String path, Map<String, String> extractedParams) {
        Matcher matcher = regex.matcher(path);
        if (!matcher.matches()) return false;
        if (extractedParams != null) {
            for (int i = 0; i < parameterNames.size(); i++) {
                extractedParams.put(parameterNames.get(i), matcher.group(i + 1));
            }
        }
        return true;
    }

    public String getPatternString() { return patternString; }
}
