## Elasticsearch

Project to demo some basic usage of ES with spring boot, using spring boot 3, ES 8.5.3 in docker compose

## Requirements

- Java 17
- docker with docker compose

To get started, use `docker-compose up -d` then `./gradlew bootRun`

## Things to look at:

- The `CompanyAuto` entity uses as much features from ES as possible using dependencies. It will create the index at startup if it doesn't exist and will create the mappings using the metadata from the field annotations
- The `CompanyManual` is the opposite of `CompanyAuto` in that we try to do things manually

## Setup

Locally in docker you should run with the options

- discovery.type=single-node
- xpack.security.enabled=false

## Elasticsearch endpoints

### Cluster Admin endpoints

- Cluster info: `curl http://localhost:9200`
- Node info: `curl http://localhost:9200/_cat/nodes?v`
- Cluster health: `curl http://localhost:9200/_cat/health?v`
- List indices: `curl http://localhost:9200/_cat/indices?v`
- Settings for index: `curl http://localhost:9200/<INDEX_NAME>/_settings` There might also be a cluster/system wide one too
- Get mapping for index: `curl http://localhost:9200/<INDEX_NAME>/_mapping`

### CRUD

- Create index: `curl -X PUT http://localhost:9200/<INDEX_NAME>`
- Delete index: `curl -X DELETE http://localhost:9200/<INDEX_NAME>`
- Create mapping: `curl -X PUT http://localhost:9200/<INDEX_NAME>`

```json
{
  "properties": {
    "<PROP1>": {
      "type": "keyword"
    },
    "<PROP2>": {
      "type": "text",
      "fields": { //any additional fields to index this as, typically used to index data as different type
        "<ALT_PROP2>": {
          "type": "keyword" //the name of the new field. this will be accessed as <PROP_2>.<ALT.PROP2>
        }
      }
    }
  }
}
```


- Mapping, there's two types of mapping. Dynamic and Explicit
  - Dynamic allows you to add new fields at the top level, object or nested by indexing a doc and it will guess the types etc
  - Explicit allows you to explicitly define what string fields are treated as full text fields etc.

#### Text analyzers

- Text analyzers are used to apply text anaylsis on fields, there are many out of the box analyzers: standard, simple, whitespace, stop etc or your own custom
  - See more: https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-analyzers.html
  - They are made of 3 parts
    - Character filters: add, remove or edit individual characters before passing to a tokenizer (these are optional) 
    - Tokenizers: chops up the text into individual tokens (must have only one)
    - Token filters: adds, removes or edits the tokens (again optional)
  - They can run at two points when using ES
    - During the indexing of a document, which processes the data and stores it as well as the source data (the index analyzer)
    - During search time, so that you can search using the same rules (the search analyzer)
    - Most of the time, you'll use the same analyzer on a certain field
- Text analysis, to test an analyzer or custom one, you can use the `_analyze` endpoint to see how ES will process text

`curl -X POST http://localhost:9200/_analyze -H 'Content-Type: application/json' -d '{json content}'` or use httpie
`http POST http://localhost:9200/_analyze key=value key=value` or just use postman/cocoa rest client

```json built in analyzer
{
  "analyzer": "standard",
  "text": "The quick brown fox."
}
```
- The above will generate the following that if used as an index analyzer will be stored in ES

```[The, quick, brown, fox.]```

```json custom analyzer
{
  "char_filter": [], //optional, can be: html_strip, mapping, pattern_replace see: https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-charfilters.html
  "tokenizer": "standard", //can only be 1 of: standard, letter, lowercase, whitespace, uax_url_email etc see: https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-classic-tokenizer.html
  "filter": ["lowercase", "asciifolding"], //optional, can be: apostrophe, asciifolding, lowercase, ngram, stemmer see: https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-tokenfilters.html
  "text": "hello, its dé da vú"
}
```

- The above will generate the following that if used as an index analyzer will be stored in ES

```[hello, its, de, da, vu]```

- If a custom analyzer has already been inserted into ES, you can also reference it in the `_analyzer` endpoint

```json Custom analyzer
{
  "analyzer": "mycustomone"
  "text": "The quick brown fox."
}
```

- By default, when you index a field using type `text` it will use the `standard` analyzer
- To create a custom analyzer you first have to ensure that the index thats going to use it doesn't exist, then use the endpoint create endpoint with the settings payload

`curl -X PUT http://localhost:9200/myIndex`

```json
{
  "settings": {
    "analysis": {
      "analyzer": {
        "my_custom_analyzer": {
          //the name of the new analyzer
          "type": "custom",
          //this is always custom
          "tokenizer": "standard",
          "char_filter": [
            "html_strip"
          ],
          "filter": [
            "lowercase",
            "asciifolding"
          ]
        }
      }
    }
  }
}
```
`curl -X PUT http://localhost:9200/myIndex/_mapping`
```json
{
  "properties": {
    "french_company_names": {
      "type": "text",
      "analyzer": "my_custom_analyzer",
      "search_analyzer": "my_custom_analyzer"
    }
  }
}
```
- Once created you can start testing the new analyzer using the `/myIndex/_analyze` endpoint
- Specifying an analyzer happens at a number of places, 
  - on a `text` type field, on an `index` or a query
  - for a `text` type, you just do like above and set it in the mappings setting
  - for index, you can define the default anaylzer for all text types with the following
  - for query time, by default a list of 4 things are checked to see which analyzer to use
    1. the `analyzer` defined in the query
    2. the `search_analyzer` in the mapping for that field
    3. the `analysis.analyzer.default_search` property in the index settings
    4. the `analyzer` in the mappings for that field
  - with the above known, its always better/safe to define the same analyzer on the `search_analyzer` field

//setting default analyzer for an index
```json
{
  "settings": {
    "analysis": {
      "analyzer": {
        "default": {
          "type": "simple"
        }
      }
    }
  }
}
```

- Search analyzer to use during query
`curl /myIndex/_search`
```json
{
  "query": {
    "match": {
      "message": {
        "query": "Quick foxes",
        "analyzer": "stop"
      }
    }
  }
}
```

- the `search analyzer` defined in mappings for a field
```json
{
  "mappings": {
    "properties": {
      "title": {
        "type": "text",
        "analyzer": "whitespace",
        "search_analyzer": "simple"
      }
    }
  }
}
```


- You can also customise/configure the out of the box analyzers if you wished

#### Stemming

#### Token Graphs


### Advanced Search
8

### General Notes

- Text vs Keyword property types, Text is for general text search that allows processing while Keyword is for SQL == type of searches