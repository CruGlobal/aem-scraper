package org.cru.aemscraper.util;

import org.cru.aemscraper.model.Link;
import org.cru.aemscraper.model.PageEntity;

public class PageUtil {

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
