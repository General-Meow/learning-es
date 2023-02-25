package com.paulhoang.Elasticsearch.repository;

import com.paulhoang.Elasticsearch.entity.CompanyAuto;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyAutoRepository extends ElasticsearchRepository<CompanyAuto, String> {

  Optional<CompanyAuto> findByTelephone(String telephone);

  Page<CompanyAuto> findAllByOwner(String owner, Pageable pageable);

}
