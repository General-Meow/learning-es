package com.paulhoang.Elasticsearch.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import java.util.List;
import org.springframework.data.annotation.Id;

/**
 * This entity tries to use as little features from spring data as possible, so no auto creation of the index and
 * mappings
 */
public record CompanyManual(

    @JsonProperty("_class")
    String clazz,
    @Id
    String id,
    String name,
    String telephone,
    String address,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/yyyy", timezone = "GMT")
    Date dateOfIncorporation,
    List<String> services,
    Owner owner, //todo build a relationship
    Status status
) {

}
