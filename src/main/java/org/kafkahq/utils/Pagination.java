package org.kafkahq.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.codehaus.httpcache4j.uri.URIBuilder;

@AllArgsConstructor
@Getter
public class Pagination{
    private Integer pageSize;
    private URIBuilder uri;
    private Integer currentPage;

    public Pagination(Integer pageSize, Integer currentPage){
        this.pageSize=pageSize;
        this.currentPage=currentPage;
    }
}
