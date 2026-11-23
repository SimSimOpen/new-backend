package net.jemsit.simsim.config;

import java.math.BigDecimal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.deser.std.StdScalarDeserializer;
import tools.jackson.databind.module.SimpleModule;

@Configuration
public class JacksonConfig {

    /**
     * Prices come from forms that group digits ("16 500 000", "1,300", or with the no-break spaces
     * that toLocaleString uses), so a BigDecimal sent as a string has its separators stripped
     * before it is parsed. Real JSON numbers are read as they are.
     */
    @Bean
    public JacksonModule lenientBigDecimalModule() {
        return new SimpleModule("lenient-big-decimal")
                .addDeserializer(BigDecimal.class, new LenientBigDecimalDeserializer());
    }

    static class LenientBigDecimalDeserializer extends StdScalarDeserializer<BigDecimal> {

        LenientBigDecimalDeserializer() {
            super(BigDecimal.class);
        }

        @Override
        public BigDecimal deserialize(JsonParser p, DeserializationContext ctxt) {
            JsonToken token = p.currentToken();
            if (token == JsonToken.VALUE_NUMBER_INT || token == JsonToken.VALUE_NUMBER_FLOAT) {
                return p.getDecimalValue();
            }
            if (token == JsonToken.VALUE_STRING) {
                String cleaned = p.getString().replaceAll("[\\s\\u00A0\\u2007\\u202F,_']", "");
                if (cleaned.isEmpty()) {
                    return null;
                }
                try {
                    return new BigDecimal(cleaned);
                } catch (NumberFormatException e) {
                    return (BigDecimal) ctxt.handleWeirdStringValue(
                            BigDecimal.class, p.getString(), "not a valid number");
                }
            }
            return (BigDecimal) ctxt.handleUnexpectedToken(BigDecimal.class, p);
        }

        @Override
        public BigDecimal getNullValue(DeserializationContext ctxt) {
            return null;
        }
    }
}
