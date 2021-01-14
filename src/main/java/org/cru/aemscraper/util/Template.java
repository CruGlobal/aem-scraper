package org.cru.aemscraper.util;

public enum Template {
    SUMMER_MISSION("CruOrgApp/components/page/summermission"),
    INTERNATIONAL_INTERNSHIP("CruOrgApp/components/page/internationalinternship"),
    STATIC_ARTICLE("CruOrgApp/components/page/article"),
    ARTICLE_LONG_FORM("CruOrgApp/components/page/article-long-form"),
    CONTENT("CruOrgApp/components/page/content"),
    DAILY_CONTENT("CruOrgApp/components/page/daily-content"),
    MARKETING_CONTENT("CruOrgApp/components/page/marketing-content"),
    LANDING("CruOrgApp/components/page/editable/landing-page"),
    VIDEO_PLAYER("CruOrgApp/components/page/editable/videoplayer-page"),
    BLOG_POST("JesusFilmApp/components/page/blogpost"),
    DYNAMIC_ARTICLE("CruOrgApp/components/page/editable/article"),
    OTHER("");

    private final String templatePath;

    Template(final String templatePath) {
        this.templatePath = templatePath;
    }

    public String getPath() {
        return templatePath;
    }

    public static Template of(final String templatePath) {
        if (templatePath == null) {
            return null;
        }
        for (Template template : values()) {
            if (template.getPath().equalsIgnoreCase(templatePath)) {
                return template;
            }
        }

        return OTHER;
    }
}
