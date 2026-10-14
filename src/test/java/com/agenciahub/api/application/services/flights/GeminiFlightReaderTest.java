package com.agenciahub.api.application.services.flights;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.net.http.*;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GeminiFlightReaderTest {
    private GeminiFlightReader reader(int status,String body) throws Exception {
        var client=mock(HttpClient.class);
        @SuppressWarnings("unchecked") HttpResponse<String> response=mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status); when(response.body()).thenReturn(body);
        when(client.send(any(HttpRequest.class),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(response);
        var config=new FlightImportProperties();config.setApiKey("fake-secret-never-display");
        return new GeminiFlightReader(new ObjectMapper(),config,client);
    }
    private final FlightDocumentReader.Document document=new FlightDocumentReader.Document("image/png",new byte[]{1},null,null);
    @Test void classifiesProviderFailuresWithoutExposingKeysOrRawMessages() throws Exception {
        for(var scenario:List.of(new Object[]{429,"limite de uso"},new Object[]{403,"não está autorizado"},new Object[]{404,"modelo"},new Object[]{400,"rejeitou"},new Object[]{503,"temporariamente indisponível"})) {
            var reader=reader((int)scenario[0],"{\"error\":{\"status\":\"UNAVAILABLE\",\"message\":\"fake-secret-never-display private-customer-content\"}}");
            assertThatThrownBy(()->reader.read(document)).isInstanceOf(IllegalStateException.class).hasMessageContaining((String)scenario[1])
                .hasMessageNotContaining("fake-secret").hasMessageNotContaining("private-customer");
        }
    }
    @Test void recognizesInvalidKeyEvenWhenProviderUsesHttp400() throws Exception {
        var reader=reader(400,"{\"error\":{\"status\":\"INVALID_ARGUMENT\",\"details\":[{\"reason\":\"API_KEY_INVALID\"}]}}");
        assertThatThrownBy(()->reader.read(document)).hasMessageContaining("configurado corretamente");
    }
    @Test void handlesNonJsonProviderOutageWithoutLeakingBody() throws Exception {
        var reader=reader(502,"<html>private-customer-content</html>");
        assertThatThrownBy(()->reader.read(document)).hasMessageContaining("temporariamente indisponível").hasMessageNotContaining("private-customer");
    }
    @Test void distinguishesTimeoutFromProviderRejection() throws Exception {
        var client=mock(HttpClient.class);
        when(client.send(any(HttpRequest.class),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenThrow(new HttpTimeoutException("request timed out"));
        var config=new FlightImportProperties();config.setApiKey("fake-key");
        var reader=new GeminiFlightReader(new ObjectMapper(),config,client);
        assertThatThrownBy(()->reader.read(document)).hasMessageContaining("demorou");
    }
    @Test void preservesSuccessfulExtraction() throws Exception {
        var reader=reader(200,"{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":[{\"text\":\"{\\\"offers\\\":[],\\\"warnings\\\":[]}\"}]}}],\"usageMetadata\":{\"promptTokenCount\":100,\"candidatesTokenCount\":20}}");
        var result=reader.read(document);assertThat(result.extraction().offers()).isEmpty();assertThat(result.inputTokens()).isEqualTo(100);
    }
}
