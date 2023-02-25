package com.paulhoang.Elasticsearch.controller;

import com.paulhoang.Elasticsearch.entity.CompanyAuto;
import com.paulhoang.Elasticsearch.repository.CompanyAutoRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
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
@RequestMapping("/company/auto")
@RequiredArgsConstructor
public class CompanyAutoController {

  private final CompanyAutoRepository companyRepository;
  private final ElasticsearchTemplate elasticsearchTemplate;

  @GetMapping("/{id}")
  public ResponseEntity<CompanyAuto> getCompany(@PathVariable String id) {
    final Optional<CompanyAuto> optionalCompany = companyRepository.findById(id);
    return ResponseEntity.of(optionalCompany);
  }

  @GetMapping("/search/{term}")
  public ResponseEntity<List<CompanyAuto>> search(@PathVariable String term) {
    final CriteriaQuery criteriaQuery = CriteriaQuery.builder(Criteria.where("address").contains(term))
        .build();

    final SearchHits<CompanyAuto> searchResult = elasticsearchTemplate.search(criteriaQuery,
        CompanyAuto.class);
    return ResponseEntity.of(
        Optional.of(searchResult.getSearchHits().stream().map(SearchHit::getContent).collect(
            Collectors.toList())));
  }


  @GetMapping("/autocomplete/search/{term}")
  public ResponseEntity<List<CompanyAuto>> autocompleteSearch(@PathVariable String term) {
    final CriteriaQuery criteriaQuery = CriteriaQuery.builder(Criteria.where("name.search").is(term))
        .build();

    final SearchHits<CompanyAuto> searchResult = elasticsearchTemplate.search(criteriaQuery,
        CompanyAuto.class);
    return ResponseEntity.of(
        Optional.of(searchResult.getSearchHits().stream().map(SearchHit::getContent).collect(
            Collectors.toList())));
  }

  @GetMapping("/autocomplete/fuzzy/search/{term}")
  public ResponseEntity<List<CompanyAuto>> autocompleteFuzzySearch(@PathVariable String term) {
    final CriteriaQuery criteriaQuery = CriteriaQuery.builder(Criteria.where("name.search").fuzzy(term))
        .build();

    final SearchHits<CompanyAuto> searchResult = elasticsearchTemplate.search(criteriaQuery,
        CompanyAuto.class);
    return ResponseEntity.of(
        Optional.of(searchResult.getSearchHits().stream().map(SearchHit::getContent).collect(
            Collectors.toList())));
  }

  @GetMapping("/search/telephone/{term}")
  public ResponseEntity<CompanyAuto> telephoneSearch(@PathVariable String term) {

    final Optional<CompanyAuto> byTelephone = companyRepository.findByTelephone(term);

    return ResponseEntity.of(byTelephone);
  }

  @GetMapping("/search/owner/{term}")
  public ResponseEntity<Page<CompanyAuto>> ownerSearch(@PathVariable String term, Pageable pageable) {

    final Page<CompanyAuto> companies = companyRepository.findAllByOwner(term, pageable);

    return ResponseEntity.ok(companies);
  }
}
