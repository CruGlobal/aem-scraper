package org.cru.aemscraper.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.cru.aemscraper.model.Link;
import org.cru.aemscraper.model.PageEntity;
import org.cru.aemscraper.model.PageInfinityJson;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PageEntityUtil {
    private static final Logger LOG = LoggerFactory.getLogger(PageEntityUtil.class);

    private static final String INFINITY_EXTENSION = ".infinity.json";
    private static final String PUBLISHER_URI = "http://prodpub1.aws.cru.org:4503";
    private static final String URL_MAPPER_URI = PUBLISHER_URI + "/bin/cru/url/mapper.txt";

    public static Set<String> getPublicFacingUrls(final PageEntity pageEntity) {
        String publisherUrl = getUrl(pageEntity);
        if (publisherUrl == null) {
            return new HashSet<>();
        }
        String path = publisherUrl.replace(PUBLISHER_URI, "").replace(".html", "");
        Set<String> paths = new HashSet<>();
        paths.add(path);
        paths.addAll(getVanityPaths(pageEntity));

        Client client = ClientBuilder.newClient()
            .register(JacksonJsonProvider.class)
            .register(HttpAuthenticationFeature.basic(System.getProperty("username"), System.getProperty("password")));
        return getUrlsFromPaths(paths, client);
    }

    public static String getUrl(final PageEntity pageEntity) {
        String contentUrl = getContentUrl(pageEntity);
        if (contentUrl != null) {
            return useHtmlUrl(contentUrl);
        }
        return null;
    }

    private static String useHtmlUrl(final String url) {
        if (url.endsWith(INFINITY_EXTENSION)) {
            return url.replace(INFINITY_EXTENSION, ".html");
        }
        return url;
    }

    public static List<String> getVanityPaths(final PageEntity pageEntity) {
        String contentUrl = getContentUrl(pageEntity);
        if (contentUrl != null) {
            if (contentUrl.endsWith(INFINITY_EXTENSION)) {
                try {
                    try {
                        return getVanityPathsFromInfinityJson(getPageInfinityJson(contentUrl));
                    } catch (MismatchedInputException mismatchedInputException) {
                        /* In this case, the infinity.json most likely produced an output like
                         * [
                         *   "/content/site/us/en/some-page.0.json",
                         *   "/content/site/us/en/some-page.1.json",
                         *   "/content/site/us/en/some-page.2.json"
                         * ]
                         * which is for pagination purposes. The properties are on all but 0, and so we can use 1.
                         */
                        PageInfinityJson infinityJson = getPageInfinityJson(contentUrl.replace("infinity", "1"));
                        return getVanityPathsFromInfinityJson(infinityJson);
                    }
                } catch (Exception e) {
                    LOG.error("Failed to get infinity.json data", e);
                }
            }
        }
        return new ArrayList<>();
    }

    private static PageInfinityJson getPageInfinityJson(final String contentUrl)
        throws IOException, URISyntaxException {

        Client client = ClientBuilder.newClient().register(JacksonJsonProvider.class);
        Response response = client
            .target(new URI(contentUrl))
            .request()
            .accept(MediaType.APPLICATION_JSON)
            .get();

        JsonParser jsonParser = new ObjectMapper()
            .getFactory()
            .createParser(response.readEntity(String.class));

            return jsonParser.readValueAs(PageInfinityJson.class);
    }

    private static List<String> getVanityPathsFromInfinityJson(final PageInfinityJson infinityJson) {
        if (infinityJson == null) {
            return new ArrayList<>();
        }
        if (infinityJson.getJcrContent().isRedirect()) {
            // This means that when someone goes to the vanity URL, they get redirected to the non-vanity
            return new ArrayList<>();
        }
        return infinityJson.getJcrContent().getVanityPaths();
    }

    private static Set<String> getUrlsFromPaths(final Set<String> paths, final Client client) {
        WebTarget webTarget = client.target(URL_MAPPER_URI);

        for (String path : paths) {
            webTarget = webTarget.queryParam("path", path);
        }
        Response response = webTarget
            .request()
            .get();
        return response.readEntity(new GenericType<Set<String>>(){});
    }

    private static String getContentUrl(final PageEntity pageEntity) {
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
