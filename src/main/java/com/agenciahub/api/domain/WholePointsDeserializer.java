package com.agenciahub.api.domain;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import java.io.IOException;

/** Prevent Jackson from silently truncating fractional mileage quantities. */
public class WholePointsDeserializer extends JsonDeserializer<Long> {
    @Override public Long deserialize(JsonParser parser,DeserializationContext context) throws IOException {
        if(!parser.currentToken().isNumeric())throw JsonMappingException.from(parser,"Informe uma quantidade inteira.");
        try{return parser.getDecimalValue().longValueExact();}
        catch(ArithmeticException ex){throw JsonMappingException.from(parser,"Informe uma quantidade inteira dentro do limite permitido.",ex);}
    }
}
