package com.tbm.admin.model.view.rss;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@JacksonXmlRootElement(localName = "rss")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RssFeed {

    @JacksonXmlProperty(localName = "channel")
    private Channel channel;

    @Setter
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Channel {

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "item")
        private List<Item> items;

    }

    @Setter
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {

        @JacksonXmlProperty(localName = "title")
        private String title;

        @JacksonXmlProperty(localName = "pubDate")
        private String pubDate;

        @JacksonXmlProperty(localName = "guid")
        private String guid;

    }
}