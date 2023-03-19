package com.paulhoang.Elasticsearch.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Dynamic;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.Setting;

/**
 * This entity tries to use as much features as possible from the ES dependency with auto index creation and mappings
 * (using the @Field annotation, multi fields and relationships)
 */
@Document(indexName = "companyauto", createIndex = true, dynamic = Dynamic.TRUE)
@Setting(shards = 1, replicas = 1, settingPath = "static/index_settings.json")
//@Mapping(mappingPath = "static") you can also use this to define the mapping of this doc instead of the annotations
public record CompanyAuto(@Id
                          String id,
                          @MultiField(
                              mainField = @Field(type = FieldType.Keyword),
                              otherFields = {
                                  @InnerField(suffix = "search", type = FieldType.Text, analyzer = "autocomplete_index", searchAnalyzer = "autocomplete_search")
                              }
                          )
                          String name,

                          @Field(type = FieldType.Keyword)
                          String telephone,

                          @MultiField(
                              mainField = @Field(type = FieldType.Text),
                              otherFields = {
                                  @InnerField(suffix = "kw", type = FieldType.Keyword)
                              }
                          )
                          String address,

                          @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/yyyy", timezone = "GMT")
                          @Field(type = FieldType.Date)
                          LocalDate dateOfIncorporation,

                          @Field(type = FieldType.Keyword)
                          List<String> services,

                          @Field(type = FieldType.Nested)
                          Owner owner, //todo build a relationship

                          @Field(type = FieldType.Keyword)
                          Status status
) {

}
