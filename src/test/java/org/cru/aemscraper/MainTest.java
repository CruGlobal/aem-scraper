package org.cru.aemscraper;

import org.cru.aemscraper.model.PageEntity;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class MainTest {
    @Test
    public void testGetContentScoreWithScoreProperty() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("score", 4);

        PageEntity page = new PageEntity()
            .withProperties(properties);

        String score = Main.getContentScore(page, new ArrayList<>());
        assertThat(score, is(equalTo("4")));
    }

    @Test
    public void testGetContentScoreWithTag() {
        List<String> tagList = new ArrayList<>();
        tagList.add("secular-topics:holidays/mothers-day");
        tagList.add("target-audience:scale-of-belief/6");

        Map<String, Object> properties = new HashMap<>();
        properties.put("cq:tags", tagList);

        PageEntity page = new PageEntity()
            .withProperties(properties);

        String score = Main.getContentScore(page, tagList);
        assertThat(score, is(equalTo("6")));
    }
}
