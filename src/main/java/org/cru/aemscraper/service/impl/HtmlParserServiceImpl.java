package org.cru.aemscraper.service.impl;

import org.cru.aemscraper.model.PageEntity;
import org.cru.aemscraper.service.HtmlParserService;
import org.cru.aemscraper.util.PageEntityUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HtmlParserServiceImpl implements HtmlParserService {
    private static final Logger LOG = LoggerFactory.getLogger(HtmlParserServiceImpl.class);

    @Override
    public String parsePage(final PageEntity pageEntity) {
        if (pageEntity.getLinks() == null || pageEntity.getLinks().isEmpty()) {
            return "";
        }

        String url = PageEntityUtil.getUrl(pageEntity);
        if (url == null) {
            return "";
        }

        // Skip any redirects
        if (isRedirectPage(pageEntity)) {
            LOG.info("Skipping redirect: " + url + " -> " + determineRedirectTarget(pageEntity));
            return "";
        }

        try {
            Document document = Jsoup.connect(url).get();

            Element body = document.body();

            String navigationText = getNavigationText(body);
            String bodyText = document.body().text();

            return bodyText.replace(navigationText, "");
        } catch (Exception e) {
            LOG.error("Error handling URL: " + url, e);
            return "";
        }
    }

    private boolean isRedirectPage(final PageEntity pageEntity) {
        return determineRedirectTarget(pageEntity) != null ||
            (pageEntity.getProperties().get("cq:template") != null &&
                pageEntity.getProperties().get("cq:template").equals("/apps/CruOrgApp/templates/redirect"));
    }

    private String determineRedirectTarget(final PageEntity pageEntity) {
        return pageEntity.getProperties().get("redirectTarget") != null ?
            (String) pageEntity.getProperties().get("redirectTarget") :
            (String) pageEntity.getProperties().get("cq:redirectTarget");
    }

    private String getNavigationText(final Element body) {
        if (body != null && body.getElementById("cru-header-nav") != null) {
            return body.getElementById("cru-header-nav").text();
        }
        return "";
    }
}
