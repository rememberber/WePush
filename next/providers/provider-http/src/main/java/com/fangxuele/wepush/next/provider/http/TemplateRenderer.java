package com.fangxuele.wepush.next.provider.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fangxuele.wepush.next.core.api.RecipientRecord;
import com.fangxuele.wepush.next.core.api.RecipientValue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TemplateRenderer {
    private static final Pattern VARIABLE = Pattern.compile("\\{\\{([A-Za-z0-9_.-]+)}}");
    private static final ObjectMapper JSON = new ObjectMapper();

    private TemplateRenderer() {
    }

    static String renderText(String template, RecipientRecord recipient) {
        return render(template, recipient, false);
    }

    static String renderJsonStringContent(String template, RecipientRecord recipient) {
        return render(template, recipient, true);
    }

    private static String render(String template, RecipientRecord recipient, boolean jsonEscape) {
        Matcher matcher = VARIABLE.matcher(template);
        StringBuilder rendered = new StringBuilder(template.length() + 32);
        while (matcher.find()) {
            RecipientValue value = recipient.fields().get(matcher.group(1));
            if (value == null) {
                throw new IllegalArgumentException("Recipient field is missing: " + matcher.group(1));
            }
            String replacement = rawValue(value);
            if (jsonEscape) {
                replacement = jsonStringContent(replacement);
            }
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    private static String rawValue(RecipientValue value) {
        return switch (value) {
            case RecipientValue.TextValue text -> text.value();
            case RecipientValue.NumberValue number -> number.value().toPlainString();
            case RecipientValue.BooleanValue bool -> Boolean.toString(bool.value());
            case RecipientValue.NullValue ignored -> "null";
            case RecipientValue.BinaryRefValue binary -> binary.artifact().artifactId();
        };
    }

    private static String jsonStringContent(String value) {
        try {
            String quoted = JSON.writeValueAsString(value);
            return quoted.substring(1, quoted.length() - 1);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Recipient field cannot be JSON encoded", exception);
        }
    }
}
