package com.paulhoang.Elasticsearch.repository;

import com.paulhoang.Elasticsearch.entity.CompanyAuto;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyAutoRepository extends ElasticsearchRepository<CompanyAuto, String> {

}
