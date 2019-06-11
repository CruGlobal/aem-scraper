package org.cru.aemscraper.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cru.aemscraper.model.Link;
import org.cru.aemscraper.model.PageEntity;
import org.cru.aemscraper.service.AemScraperService;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AemScraperServiceImpl implements AemScraperService {
    private Client client;

    public AemScraperServiceImpl() {
        this(ClientBuilder.newClient());
    }

    AemScraperServiceImpl(final Client client) {
        this.client = client;
    }

    @Override
    public PageEntity scrape(final String pageUrl) throws IOException {
        PageEntity rootEntity = getPageEntity(pageUrl);

        handlePagination(rootEntity);

        if (rootEntity.getChildren() != null) {
            for (PageEntity child : rootEntity.getChildren()) {
                for (Link link : child.getLinks()) {
                    for (String rel : link.getRel()) {
                        if (rel.equals("self")) {
                            PageEntity realChild = scrape(link.getHref());
                            child = child
                                .withChildren(realChild.getChildren())
                                .withLinks(realChild.getLinks())
                                .withProperties(realChild.getProperties());
                        }
                    }
                }
            }
        }
        return rootEntity;
    }

    @Override
    public PageEntity removeNonPages(final PageEntity pageEntity) {
        PageEntity filtered = new PageEntity()
            .withChildren(new ArrayList<>())
            .withClassType(pageEntity.getClassType())
            .withLinks(pageEntity.getLinks())
            .withProperties(pageEntity.getProperties());

        if (pageEntity.getChildren() == null) {
            return filtered;
        }

        for (PageEntity child : pageEntity.getChildren()) {
            if (child.getChildren() != null && !child.getChildren().isEmpty()) {
                child = removeNonPages(child);
            }
            for (String classType : child.getClassType()) {
                if (classType.equals("content/page")) {
                    filtered.getChildren().add(child);
                }
            }
        }
        return filtered;
    }

    private void handlePagination(final PageEntity page) throws IOException {
        List<PageEntity> children = page.getChildren();
        PageEntity paginated = page;

        while (hasMorePages(paginated)) {
            paginated = getNextPage(paginated);
            children.addAll(paginated.getChildren());
        }
    }

    private PageEntity getNextPage(final PageEntity page) throws IOException {
        for (Link link : page.getLinks()) {
            for (String rel : link.getRel()) {
                if (rel.equals("next")) {
                    return getPageEntity(link.getHref());
                }
            }
        }
        throw new IllegalArgumentException("Called getNextPage() on a page without a next page.");
    }

    PageEntity getPageEntity(final String jsonUrl) throws IOException {
        Response response = client.target(jsonUrl).request().get();
        String json = response.readEntity(String.class);

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, PageEntity.class);
    }

    boolean hasMorePages(final PageEntity page) {
        if (page.getLinks() != null && !page.getLinks().isEmpty()) {
            for (Link link : page.getLinks()) {
                for (String rel : link.getRel()) {
                    if (rel.equals("next")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
