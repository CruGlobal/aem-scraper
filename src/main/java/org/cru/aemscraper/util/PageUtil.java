package org.cru.aemscraper.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cru.aemscraper.model.Link;
import org.cru.aemscraper.model.PageEntity;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.io.IOException;

public class PageUtil {
    public static void populateJcrContent(final PageEntity pageEntity, final Client client) throws IOException {
        if (client == null) {
            return;
        }

        String contentUrl = getContentUrl(pageEntity);

        if (contentUrl != null && contentUrl.endsWith(".json")) {
            Response response = client.target(contentUrl).request().get();
            String contentJson = response.readEntity(String.class);

            if (contentJson == null || contentJson.trim().isEmpty()) {
                return;
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readValue(contentJson, JsonNode.class);

            if (jsonNode != null) {
                pageEntity.setJcrContent(jsonNode.get("jcr:content"));
            }
        }
    }

    public static String getContentUrl(final PageEntity pageEntity) {
        for (Link link : pageEntity.getLinks()) {
            for (String rel : link.getRel()) {
                if (rel.equals("content")) {
                    return link.getHref();
                }
            }
        }
        return null;
    }
}
