package org.cru.aemscraper.util;

import org.cru.aemscraper.model.Link;
import org.cru.aemscraper.model.PageEntity;

public class PageEntityUtil {
    public static String getUrl(final PageEntity pageEntity) {
        for (Link link : pageEntity.getLinks()) {
            for (String rel : link.getRel()) {
                if (rel.equals("content")) {
                    return useHtmlUrl(link.getHref());
                }
            }
        }
        return null;
    }

    private static String useHtmlUrl(final String url) {
        if (url.endsWith(".infinity.json")) {
            return url.replace(".infinity.json", ".html");
        }
        return url;
    }
}
