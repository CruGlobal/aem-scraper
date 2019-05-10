package org.cru.aemscraper.service.impl;

import org.cru.aemscraper.model.Link;
import org.cru.aemscraper.model.PageEntity;
import org.cru.aemscraper.service.HtmlParserService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class HtmlParserServiceImpl implements HtmlParserService {
    @Override
    public String parsePage(final PageEntity pageEntity) {
        if (pageEntity.getLinks() == null || pageEntity.getLinks().isEmpty()) {
            return "";
        }

        String url = getUrl(pageEntity);
        if (url == null) {
            return "";
        }

        // Skip any redirects
        if (isRedirectPage(pageEntity)) {
            System.out.println("Skipping redirect: " + url + " -> " + pageEntity.getProperties().get("redirectTarget"));
            return "";
        }

        try {
            Document document = Jsoup.connect(url).get();

            Element body = document.body();

            String navigationText = getNavigationText(body);
            String bodyText = document.body().text();

            return bodyText.replace(navigationText, "");
        } catch (Exception e) {
            System.out.println("Error handling URL: " + url);
            e.printStackTrace();
            return "";
        }
    }

    private boolean isRedirectPage(final PageEntity pageEntity) {
        return pageEntity.getProperties().get("redirectTarget") != null ||
            pageEntity.getProperties().get("cq:redirectTarget") != null ||
            (pageEntity.getProperties().get("cq:template") != null &&
                pageEntity.getProperties().get("cq:template").equals("/apps/CruOrgApp/templates/redirect"));
    }

    private String getUrl(final PageEntity pageEntity) {
        for (Link link : pageEntity.getLinks()) {
            for (String rel : link.getRel()) {
                if (rel.equals("content")) {
                    return useHtmlUrl(link.getHref());
                }
            }
        }
        return null;
    }

    private String useHtmlUrl(final String url) {
        if (url.endsWith(".infinity.json")) {
            return url.replace(".infinity.json", ".html");
        }
        return url;
    }

    private String getNavigationText(final Element body) {
        if (body != null && body.getElementById("cru-header-nav") != null) {
            return body.getElementById("cru-header-nav").text();
        }
        return "";
    }
}
