
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
import com.paulhoang.Elasticsearch.service.CompanyManualService;
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

  private final CompanyManualService companyManualService;


  @GetMapping("/{id}")
  public ResponseEntity<CompanyManual> getCompany(@PathVariable String id) throws IOException {
    return ResponseEntity.of(companyManualService.getCompanyById(id));
  }

  @GetMapping("/search/{term}")
  public ResponseEntity<List<CompanyManual>> search(@PathVariable String term) throws IOException {
    final List<CompanyManual> foundByAddress = companyManualService.findByAddress(term);
    return ResponseEntity.ok(foundByAddress);
  }

  @GetMapping("/autocomplete/search/{term}")
  public ResponseEntity<List<CompanyManual>> autocompleteSearch(@PathVariable String term) throws IOException {
    final List<CompanyManual> foundCompanies = companyManualService.findByCompanyName(term);
    return ResponseEntity.ok(foundCompanies);
  }

  @GetMapping("/autocomplete/fuzzy/search/{term}")
  public ResponseEntity<List<CompanyManual>> autocompleteFuzzySearch(@PathVariable String term) throws IOException {
    final List<CompanyManual> companies = companyManualService.findByCompanyNameFuzzy(term);
    return ResponseEntity.ok(companies);
  }
}
