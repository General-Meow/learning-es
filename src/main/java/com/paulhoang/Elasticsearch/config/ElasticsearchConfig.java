package com.paulhoang.Elasticsearch.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.DateProperty;
import co.elastic.clients.elasticsearch._types.mapping.KeywordProperty;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TextProperty;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.CountRequest.Builder;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.indices.CloseIndexResponse;
import co.elastic.clients.elasticsearch.indices.OpenResponse;
import co.elastic.clients.elasticsearch.indices.PutIndicesSettingsResponse;
import co.elastic.clients.elasticsearch.indices.PutMappingRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.paulhoang.Elasticsearch.entity.CompanyAuto;
import com.paulhoang.Elasticsearch.entity.CompanyManual;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.elasticsearch.BulkFailureException;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery.OpType;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@EnableElasticsearchRepositories
@Configuration
@Slf4j
public class ElasticsearchConfig extends ElasticsearchConfiguration {

  @Value("${es.hostAndPort}")
  private String esUrl;

  @Value("${es.forceCreateData}")
  private boolean forceCreate;


  @Override
  public ClientConfiguration clientConfiguration() {
    return ClientConfiguration.builder().connectedTo(esUrl).build();
  }


  enum CompanyType {
    MANUAL("companymanual", CompanyManual.class), AUTO("companyauto", CompanyAuto.class);

    private final String indexName;
    private final Class<?> clazz;

    CompanyType(String indexName, Class<?> clazz) {
      this.indexName = indexName;
      this.clazz = clazz;
    }

    public String getIndexName() {
      return indexName;
    }

    public Class<?> getClazz() {
      return clazz;
    }
  }

  @Bean
  public CommandLineRunner setupCompanyManualIndex(ElasticsearchOperations elasticsearchOperations,
      ElasticsearchClient elasticsearchClient) throws IOException {
    return (args) -> {
      log.info("Checking if companymanual index exists");
      final boolean exists = elasticsearchOperations.indexOps(IndexCoordinates.of(CompanyType.MANUAL.getIndexName()))
          .exists();
      if (exists) {
        log.info("CompanyManual Index exists");
        createCompanyData(elasticsearchOperations, elasticsearchClient, CompanyType.MANUAL);
      } else {
        log.info("Index does not exist so creating index...");
        final boolean successfullyCreated = createCompanyIndex(elasticsearchOperations, elasticsearchClient,
            CompanyType.MANUAL);
        if (successfullyCreated) {
          log.info("Index created so creating index settings");
          createSettings(elasticsearchClient);
          createManualMapping(elasticsearchClient);
          createCompanyData(elasticsearchOperations, elasticsearchClient, CompanyType.MANUAL);
        }
      }
    };
  }

  @Bean
  public CommandLineRunner setupCompanyAutoIndex(ElasticsearchOperations elasticsearchOperations,
      ElasticsearchClient elasticsearchClient) throws IOException {
    return (args) -> {
      log.info("Checking if companyauto index exists");
      final boolean exists = elasticsearchOperations.indexOps(CompanyAuto.class).exists();

      if (exists) {
        log.info("CompanyAuto Index exists");
        createCompanyData(elasticsearchOperations, elasticsearchClient, CompanyType.AUTO);
      } else {
        log.info("Index does not exist so creating index...");
        final boolean successfullyCreated = createCompanyIndex(elasticsearchOperations, elasticsearchClient,
            CompanyType.AUTO);
        if (successfullyCreated) {
          createCompanyData(elasticsearchOperations, elasticsearchClient, CompanyType.AUTO);
        }
      }
    };
  }

  private void createSettings(ElasticsearchClient elasticsearchClient) throws IOException {
    File datafile = new ClassPathResource("static/index_settings.json").getFile();
    final FileInputStream fileInputStream = new FileInputStream(datafile);

    final CloseIndexResponse closeIndexResponse = elasticsearchClient.indices()
        .close(builder -> builder.index(CompanyType.MANUAL.getIndexName()));
    log.info("closed companymanual {}", closeIndexResponse.acknowledged());

    final PutIndicesSettingsResponse putIndicesSettingsResponse = elasticsearchClient.indices()
        .putSettings(builder -> builder.index(CompanyType.MANUAL.getIndexName()).withJson(fileInputStream));
    log.info("settings result {}", putIndicesSettingsResponse.acknowledged());

    final OpenResponse openResponse = elasticsearchClient.indices()
        .open(builder -> builder.index(CompanyType.MANUAL.getIndexName()));
    log.info("open companymanual {}", openResponse.acknowledged());
  }

  private boolean createCompanyIndex(ElasticsearchOperations elasticsearchOperations,
      ElasticsearchClient elasticsearchClient, CompanyType type)
      throws IOException {

    final boolean created = elasticsearchOperations.indexOps(IndexCoordinates.of(type.getIndexName())).create();
    if (created) {
      log.info("Successfully created {} index", type.getIndexName());
    } else {
      log.info("failed to create {} index", type.getIndexName());
    }
    return created;
  }

  private void createManualMapping(ElasticsearchClient elasticsearchClient) throws IOException {
    log.info("Creating mapping for manual index");
    final PutMappingRequest mappingRequest = PutMappingRequest.of(
        builder -> {
          builder.index(CompanyType.MANUAL.getIndexName());
          Map<String, Property> propertyMap = new HashMap<>();

          propertyMap.put("id", new Property(KeywordProperty.of(builder1 -> builder1)));
          propertyMap.put("name", new Property(KeywordProperty.of(builder1 -> {
            Map<String, Property> fields = new HashMap<>();
            fields.put("search", new Property(TextProperty.of(
              builder2 -> builder2.analyzer("autocomplete_index").searchAnalyzer("autocomplete_search"))));

            builder1.fields(fields);
            return builder1;
          })));
          propertyMap.put("telephone", new Property(KeywordProperty.of(builder1 -> builder1)));
          propertyMap.put("address", new Property(TextProperty.of(builder1 -> builder1)));
          propertyMap.put("dateOfIncorporation",
              new Property(DateProperty.of(builder1 -> builder1.format("date_optional_time||epoch_millis"))));
          propertyMap.put("services", new Property(KeywordProperty.of(builder1 -> builder1)));
          propertyMap.put("owner", new Property(KeywordProperty.of(builder1 -> builder1)));
          builder.properties(propertyMap);

          return builder;
        });
    elasticsearchClient.indices().putMapping(mappingRequest);
  }

  private void createCompanyData(ElasticsearchOperations elasticsearchOperations,
      ElasticsearchClient elasticsearchClient, CompanyType type) throws IOException {

    final Builder builder = new Builder();
    final CountRequest companyAutoCount = builder.index(List.of(type.getIndexName())).build();
    final CountResponse count = elasticsearchClient.count(companyAutoCount);

    if (count.count() == 0 || forceCreate) {

      try {
        File datafile = new ClassPathResource("static/MOCK_DATA.json").getFile();
        final FileInputStream fileInputStream = new FileInputStream(datafile);
        JsonMapper jsonMapper = new JsonMapper();
        jsonMapper.registerModule(new JavaTimeModule());
        List<IndexQuery> bulkDocs = new ArrayList<>();
        if (type.getClazz() == CompanyAuto.class) {
          final List<CompanyAuto> companyAutos = jsonMapper.readValue(fileInputStream,
              new TypeReference<List<CompanyAuto>>() {
              });
          for (CompanyAuto company : companyAutos) {
            IndexQuery iq = new IndexQuery();
            iq.setId(company.id());
            iq.setObject(company);
            iq.setOpType(OpType.CREATE);
            bulkDocs.add(iq);
          }
        } else {
          final List<CompanyManual> companyManuals = jsonMapper.readValue(fileInputStream,
              new TypeReference<List<CompanyManual>>() {
              });
          for (CompanyManual company : companyManuals) {
            IndexQuery iq = new IndexQuery();
            iq.setId(company.id());
            iq.setObject(company);
            iq.setOpType(OpType.CREATE);
            bulkDocs.add(iq);
          }
        }

        elasticsearchOperations.bulkIndex(bulkDocs, IndexCoordinates.of(type.getIndexName()));
      } catch (BulkFailureException e) {
        log.error("blah", e);
        final Map<String, String> failedDocuments = e.getFailedDocuments();

      }

    } else {
      log.info("NOT Creating data for CompanyAuto as theres data init'ed already");
    }

  }

}
