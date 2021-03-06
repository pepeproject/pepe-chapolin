package com.globo.pepe.chapolin.services;

import static com.globo.pepe.chapolin.services.RequestService.X_PEPE_TRIGGER_HEADER;
import static com.globo.pepe.chapolin.suites.PepeSuiteTests.mockApiServer;
import static com.globo.pepe.chapolin.suites.PepeSuiteTests.mockApiServerApiKeyCreated;
import static com.globo.pepe.common.util.Constants.PACK_NAME;
import static com.globo.pepe.common.util.Constants.TRIGGER_PREFIX;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.globo.pepe.chapolin.configuration.HttpClientConfiguration;
import com.globo.pepe.common.model.Event;
import com.globo.pepe.common.model.Metadata;
import com.globo.pepe.common.services.JsonLoggerService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;


@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = {
        "pepe.logging.tags=default",
        "pepe.chapolin.stackstorm.api=http://127.0.0.1:9101/api/v1",
        "pepe.chapolin.stackstorm.auth=http://127.0.0.1:9100/auth/v1",
        "pepe.chapolin.stackstorm.login=u_pepe",
        "pepe.chapolin.stackstorm.password=u_pepe",
        "pepe.chapolin.sleep_interval_on_fail=1"
})
@ContextConfiguration(classes = {
    StackstormService.class,
    RequestService.class,
    JsonLoggerService.class,
    ObjectMapper.class,
    JsonSchemaGeneratorService.class,
    HttpClientConfiguration.class,
    StackstormAuthService.class
}, loader = AnnotationConfigContextLoader.class)
public class RequestServiceTests {

    private static final String PROJECT = "pepe";
    private static final String TRIGGER_NAME_DUPLICATED = "triggerNameOK";
    private static final String TRIGGER_FULL_NAME_DUPLICATED =
        PACK_NAME + "." + TRIGGER_PREFIX + "." + PROJECT + "." + TRIGGER_NAME_DUPLICATED;

    @Autowired
    private RequestService requestService;

    @Autowired
    private JsonSchemaGeneratorService jsonSchemaGeneratorService;

    @Autowired
    private ObjectMapper mapper;

    public void mockSendTriggerDuplicated() throws IOException {
        mockApiServer.reset();

        mockApiServerApiKeyCreated();

        InputStream triggerDuplicated = StackstormServiceTests.class.getResourceAsStream("/trigger-duplicate.json");
        String bodyTriggerDuplicated = IOUtils.toString(triggerDuplicated, Charset.defaultCharset());
        mockApiServer.when(request().withMethod("POST").withPath("/api/v1/triggertypes").withHeader(X_PEPE_TRIGGER_HEADER,
            TRIGGER_FULL_NAME_DUPLICATED))
                .respond(response().withBody(bodyTriggerDuplicated).withHeader("Content-Type", APPLICATION_JSON_VALUE).withStatusCode(409));
    }

    @Test
    public void sendTriggerDuplicatedTest() throws Exception {
        Event event = new Event();
        event.setId("2");

        Metadata metadata = new Metadata();
        metadata.setTriggerName(TRIGGER_NAME_DUPLICATED);
        metadata.setProject(PROJECT);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("attribute1", "value1");

        event.setMetadata(metadata);
        event.setPayload(payload);

        mockSendTriggerDuplicated();

        JsonNode schema = jsonSchemaGeneratorService.extract(mapper.valueToTree(event));
        requestService.createTrigger(schema);
    }
}
