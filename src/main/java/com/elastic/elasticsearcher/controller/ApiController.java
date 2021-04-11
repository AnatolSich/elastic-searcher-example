package com.elastic.elasticsearcher.controller;

import com.elastic.elasticsearcher.service.EsService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final EsService esService;

    public ApiController(EsService esService) {
        this.esService = esService;
    }


    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    @GetMapping("/count")
    public Long getCount(@RequestParam("query") String query) throws IOException {
        return esService.getCount(query);
    }

    @PutMapping("/articles")
    public String addArticle(@RequestParam("title") String title, @RequestParam("text") String text) throws IOException {
        String id = UUID.randomUUID().toString();
        esService.updateArticles(id, title, text);
        return id;
    }

    @GetMapping("/search")
    public List<EsService.Article> search(@RequestParam("query") String query) throws Exception {
        return esService.search(query);
    }
}
