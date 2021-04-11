package com.elastic.elasticsearcher.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class EsService {

    @Setter
    @Getter
    public static class Article {
        private String title;
        private String text;
    }

    private final static String INDEX_NAME = "articles";
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private final RestHighLevelClient restHighLevelClient;

    public EsService(RestHighLevelClient restHighLevelClient) {
        this.restHighLevelClient = restHighLevelClient;
    }

    public void updateArticles(String id, String title, String text) throws IOException {
        Article article = new Article();
        article.setTitle(title);
        article.setText(text);

        IndexRequest indexRequest = new IndexRequest(INDEX_NAME);
        indexRequest.id(id);
        indexRequest.source(mapper.writeValueAsString(article), XContentType.JSON);

        restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
    }

    // assess the number of found docs by this query
    public long getCount(String searchString) throws IOException {
        CountRequest countRequest = new CountRequest();
        countRequest.query(QueryBuilders
                .multiMatchQuery(searchString, "title", "text")
                .fuzziness("AUTO"));
        CountResponse countResponse = restHighLevelClient.count(countRequest, RequestOptions.DEFAULT);
        return countResponse.getCount();
    }

    public List<Article> search(String searchString) throws Exception {
        SearchRequest searchRequest = new SearchRequest(INDEX_NAME);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //  searchSourceBuilder.query(QueryBuilders.matchQuery("text", searchString));
        //search by full phrase
        //  searchSourceBuilder.query(QueryBuilders.matchPhraseQuery("text", searchString));
        //search by both properties
        searchSourceBuilder.query(QueryBuilders
                .multiMatchQuery(searchString, "title", "text")
                .fuzziness("AUTO"));  //help in case of fuzzy query

        //multy query search
       /* searchSourceBuilder.query(QueryBuilders.boolQuery()
                .must(QueryBuilders.multiMatchQuery(searchString, "title", "text"))
                .mustNot(QueryBuilders.multiMatchQuery("red", "title", "text"))); //exclude hits with word "red"*/

        // Highlight matches in results
        HighlightBuilder highlightBuilder = new HighlightBuilder()
                .field(new HighlightBuilder.Field("title"))     //property to highlight found words in
                .field(new HighlightBuilder.Field("text"));     //property to highlight found words in
        searchSourceBuilder.highlighter(highlightBuilder);

        //pagination
        searchSourceBuilder
        //        .from(1)           //skip 1 hit, start the search from 1 doc (Default = 0)
                .size(10);         //number of search hits to return (page)

        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        List<Article> articles = new ArrayList<>();
        for (SearchHit hit : searchResponse.getHits().getHits()) {
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            String title = (String) sourceAsMap.get("title");
            String text = (String) sourceAsMap.get("text");

            // Config highlight of matches fragments in results
            HighlightField highlightFieldTitle = hit.getHighlightFields().get("title");
            if (highlightFieldTitle != null && highlightFieldTitle.fragments().length > 0) {  //check all fragments where found words
                title = highlightFieldTitle.fragments()[0].toString();                         //highlight only first fragment
            }
            HighlightField highlightFieldText = hit.getHighlightFields().get("text");
            if (highlightFieldText != null && highlightFieldText.fragments().length > 0) {
                text = highlightFieldText.fragments()[0].toString();
            }

            Article article = new Article();
            article.setTitle(title);
            article.setText(text);
            articles.add(article);
        }

        return articles;
    }
}
