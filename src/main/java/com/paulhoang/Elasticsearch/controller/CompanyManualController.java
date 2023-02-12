
package com.paulhoang.Elasticsearch.controller;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import com.paulhoang.Elasticsearch.entity.CompanyAuto;
import com.paulhoang.Elasticsearch.entity.CompanyManual;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/company/manual")
@RequiredArgsConstructor
public class CompanyManualController {

  private final ElasticsearchClient elasticsearchClient;

  @GetMapping("/{id}")
  public ResponseEntity<CompanyManual> getCompany(@PathVariable String id) throws IOException {
    final GetResponse<CompanyManual> companyManualGetResponse = elasticsearchClient.get(
        GetRequest.of(builder -> builder.index("companymanual").id(id)), CompanyManual.class);
    if (companyManualGetResponse.found()) {
      return ResponseEntity.ok(companyManualGetResponse.source());
    }
    return ResponseEntity.badRequest().build();
  }

  @GetMapping("/search/{term}")
  public ResponseEntity<List<CompanyManual>> search(@PathVariable String term) throws IOException {

    final SearchResponse<CompanyManual> searchResponse = elasticsearchClient.search(s -> s
        .index("companymanual")
        .query(q -> q
            .match(t -> t
                .field("address")
                .query(term)
            )
        ),
        CompanyManual.class);


    final List<Hit<CompanyManual>> hits = searchResponse.hits().hits();
    final List<CompanyManual> companies = hits.stream().map(Hit::source).collect(Collectors.toList());

    return ResponseEntity.ok(companies);
  }

  @GetMapping("/autocomplete/search/{term}")
  public ResponseEntity<List<CompanyManual>> autocompleteSearch(@PathVariable String term) throws IOException {
    final SearchResponse<CompanyManual> searchResponse = elasticsearchClient.search(s -> s
            .index("companymanual")
            .query(q -> q
                .match(t -> t
                    .field("name.search")
                    .query(term)
                )
            ),
        CompanyManual.class);


    final List<Hit<CompanyManual>> hits = searchResponse.hits().hits();
    final List<CompanyManual> companies = hits.stream().map(Hit::source).collect(Collectors.toList());
    return ResponseEntity.ok(companies);
  }

  @GetMapping("/autocomplete/fuzzy/search/{term}")
  public ResponseEntity<List<CompanyManual>> autocompleteFuzzySearch(@PathVariable String term) throws IOException {

    final SearchResponse<CompanyManual> searchResponse = elasticsearchClient.search(s -> s
            .index("companymanual")
            .query(q -> q
                .fuzzy(t -> t
                    .field("name.search")
                    .value(term)
                )
            ),
        CompanyManual.class);


    final List<Hit<CompanyManual>> hits = searchResponse.hits().hits();
    final List<CompanyManual> companies = hits.stream().map(Hit::source).collect(Collectors.toList());
    return ResponseEntity.ok(companies);
  }
}
