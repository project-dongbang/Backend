package com.dongbang.photo.presentation;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
class PhotoOpenApiIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    void photoCreateAndReplaceDescribeBinaryFileAndOptionalTitle() throws Exception {
        String document = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String create = "$.paths['/api/v1/organizations/{organizationId}/photos'].post.requestBody.content";
        assertMultipartSchema(document, create + "['multipart/form-data'].schema");
        Map<String, Object> jsonSchema = JsonPath.read(document, create + "['application/json'].schema");
        assertThat(jsonSchema.get("$ref")).isEqualTo("#/components/schemas/CreatePhotoRequest");

        String replace = "$.paths['/api/v1/organizations/{organizationId}/photos/{photoId}/image']"
                + ".patch.requestBody.content['multipart/form-data'].schema";
        assertMultipartSchema(document, replace);
    }

    @SuppressWarnings("unchecked")
    private void assertMultipartSchema(String document, String path) {
        Map<String, Object> schema = JsonPath.read(document, path);
        if (schema.containsKey("$ref")) {
            String ref = (String) schema.get("$ref");
            schema = JsonPath.read(document, "$.components.schemas." + ref.substring(ref.lastIndexOf('/') + 1));
        }
        Map<String, Map<String, Object>> properties = (Map<String, Map<String, Object>>) schema.get("properties");
        assertThat(schema.get("type")).isEqualTo("object");
        assertThat(properties.get("file")).containsEntry("type", "string").containsEntry("format", "binary");
        assertThat(properties.get("title")).containsEntry("type", "string");
        assertThat(properties.get("title").get("maxLength")).isEqualTo(200);
        assertThat((List<String>) schema.get("required")).contains("file").doesNotContain("title");
    }
}
