package com.paulhoang.Elasticsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.paulhoang.Elasticsearch.entity.CompanyManual;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CompanyManualService {

  private final ElasticsearchClient elasticsearchClient;

  public Optional<CompanyManual> getCompanyById(String id) throws IOException {
    final GetResponse<CompanyManual> companyManualGetResponse = elasticsearchClient.get(
        GetRequest.of(builder -> builder.index("companymanual").id(id)), CompanyManual.class);

    if (companyManualGetResponse.found()){
      return Optional.empty();
    } else {
      return Optional.of(companyManualGetResponse.source());
    }
  }

  public List<CompanyManual> findByAddress(String address) throws IOException {
    final SearchResponse<CompanyManual> searchResponse = elasticsearchClient.search(s -> s
            .index("companymanual")
            .query(q -> q
                .match(t -> t
                    .field("address")
                    .query(address)
                )
            ),
        CompanyManual.class);


    final List<Hit<CompanyManual>> hits = searchResponse.hits().hits();
    return hits.stream().map(Hit::source).collect(Collectors.toList());
  }

  /**
   * Company names can have special characters so the es mapping for the name field has been changed to contain
   * an inner field that uses a custom text analyzer that strips special chars out
   * @param companyName
   * @return
   */
  public List<CompanyManual> findByCompanyName(String companyName) throws IOException {
    final SearchResponse<CompanyManual> searchResponse = elasticsearchClient.search(s -> s
            .index("companymanual")
            .query(q -> q
                .match(t -> t
                    .field("name.search")
                    .query(companyName)
                )
            ),
        CompanyManual.class);


    final List<Hit<CompanyManual>> hits = searchResponse.hits().hits();
    final List<CompanyManual> companies = hits.stream().map(Hit::source).collect(Collectors.toList());
    return companies;
  }

  public List<CompanyManual> findByCompanyNameFuzzy(String companyName) throws IOException {
    final SearchResponse<CompanyManual> searchResponse = elasticsearchClient.search(s -> s
            .index("companymanual")
            .query(q -> q
                .fuzzy(t -> t
                    .field("name.search")
                    .value(companyName)
                )
            ),
        CompanyManual.class);


    final List<Hit<CompanyManual>> hits = searchResponse.hits().hits();
    final List<CompanyManual> companies = hits.stream().map(Hit::source).collect(Collectors.toList());
    return companies;
  }

}
