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

- Create new document
  - There are 2 versions, create with specified id or have ES generate the id for you
`curl -X PUT localhost:9200/<INDEX_NAME>/_doc/<_id>`
```json
{
  "id": "myId",  //if you don't put an id here, ES wont copy it from the url into the document, but if you use the @Id annotation, spring will copy it into the id property
  "name": "my name",
  "telephone": "101010101",
  "address": "the address of the company",
  "dateOfIncorporation": "2022-01-01",
  "services": ["cars", "hair", "cut"],
  "owner": "bob"
}
```

`curl -X POST localhost:9200/<INDEX_NAME>/_doc/`
```json
{
  "id": "myId",  //this will be overridden if its provided manually, so its not needed
  "name": "my name",
  "telephone": "101010101",
  "address": "the address of the company",
  "dateOfIncorporation": "2022-01-01",
  "services": ["cars", "hair", "cut"],
  "owner": "bob"
}
```

- Update document
  - You use the same endpoint that creates a new doc with an id
  - It's important to note a few things from the result
    - Check for any failed shards
    - Check the result to be 'updated'
    - The version should be incremented
    - The document is REPLACED by the new one, so it isn't PATCHED, this means if you forget to add all the existing properties, they won't be stored anymore. in other words they will be removed / set to null!
`curl -X PUT localhost:9200/<INDEX_NAME>/_doc/<_id>`
```json
{
  "id": "myId",
  "name": "my name",
  "telephone": "101010101",
  "address": "the address of the company",
  "dateOfIncorporation": "2022-01-01",
  "services": ["cars", "hair", "cut"],
  "owner": "bobbieeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"
}
```
- Delete document

- Update by query, this is effectively update where x=''
  - `ctx` is the current context document
  - following example decrements the stock counter of matched documents
`curl -X POST localhost:9200/<INDEX_NAME>/_update_by_query`
```json
{
  "script": {
    "source": "ctx._source.stock--"
  },
  "query": {
    "term": {
      "sku": "12322"
    }
  }
}
```

- Delete by query, this is effectively delete all where x=''
`curl -X POST localhost:9200/<INDEX_NAME>`
```json
{
  "query": {
    "match": {
      "sku": "12322"
    }
  }
}
```

### Mapping

- Mapping is ES's version of a DB schema, it defines the structure and types of fields in an index
- Mapping, there's two types of mapping. Dynamic and Explicit
  - Dynamic allows you to add new fields at the top level, object or nested by indexing a doc and it will guess the types
  - Explicit allows you to explicitly define what string fields are treated as full text fields etc.
  - You can use a mix of both explicit and dynamic
  - You'll need to use explicit if you want to customise your text fields with text analysers
  - There are many types to use during mapping
    - Text - for full text search
    - keyword - for exact matching, filtering and aggregation, this is just like Text but the analyzer is an NO OP analyser
    - completion - f;or type ahead/search as you type use cases
    - boolean
    - date / date_nanos
    - float
    - integer
    - Object - for json like objects, don't use this type if you wish to query against these are they will be flattened out and lose their relationships
    - Nested - like Object but should be used for querying as this nested object retains their structure
    - Join for parent child relationships within the same index
    - IP, GEO etc etc
  - Types can also be configured with parameters
  - Types can be coerced into other types whenever possible
    - e.g. "1" can become 1
    - When this happens, the original value is stored in the `_source` property
    - This can only happen if you've previously defined the mapping
    - coersion is enabled by default, if you want to disable it, you can do so at the property level in the mapping or the index settings level, `"coerce": false` or `"settings": { "index.mapping.coerce": false }`
  - To create a mapping, you'll need to use the `properties` parameter in the json to define the mapping structure
  - There is no special type for Arrays, this is because every type is flattened into an array of zero or more values
- Because mappings are dynamic, all properties in an index are optional, so you can have documents with no values in them. 
  - There is no need to define properties as nullable, this means ES doesn't provide any means of integrity like a relational database
  - During a search, you do not need to handle fields that are empty, ES will ignore documents with those missing fields automatically
- To get a mapping `curl localhost:9200/<INDEX_NAME>/_mapping`
- To create a mapping
`curl -X PUT http://localhost:9200/myIndex/_mapping`
```json
{
  "mapping": {
    "properties": {
      "french_company_names": {
        "type": "text",
        "analyzer": "my_custom_analyzer",
        "search_analyzer": "my_custom_analyzer"
      }
    } 
  }
}
```
- To get a mapping for a specific field `curl localhost:9200/<INDEX_NAME>/_mapping/field/<FIELD_NAME>`
  - You can also get specific nested field mappings using the dot notation `curl localhost:9200/my_index/_mapping/field/owner.name`
- You can create mappings using dot notation for nested objects too, rather than using another `property` key
```json
{
  "mappings": {
    "properties": {
      "door_number": { "type": "integer" },
      "owners.name": { "type": "keyword" },
      "owners.age": { "type": "integer" }
    }
  }
}
```
- Updating mappings - almost the same as create but you miss out the `mapping` property
- You cannot change the type of a field once it has been created in the mapping
  - The only exception to this is SOME field mappings can be changed/updated
  - This is because you may already have documents indexed and analysed already and changing it will invalidate that and it may change the underlying datastore type e.g. from BKD to inverted index
- You cannot remove a field mapping too
- If need to make a change to the mapping that is not allowed, you will then need to use the Reindex API
- Updates are appended and not replaced, this means you can just add the new field to the payload and it will be added to the existing mapping
`curl -X PUT localhost:9200/<INDEX_NAME>/_mapping/`
```json
{
  "properties": {
    "name": {
      "type" : "text"
    }
  }
}

```
### Keyword and Text types

- Depending on the data type, different data structures are used to internally store them (handled by lucine)
  - This is so that searching can be done efficiently
  - Text and keywords use an inverted index
    - Inverted indexes are terms (tokens) on the X axis and the document id on the Y axis, so all the individual tokens that get generated from the anaylisis is there on the X
    - Terms are stored in alphabet order
    - The inverted index makes it extremely easy and fast to find documents that contain a word
    - Once a match is found, it is then scored, the more matches the higher the score
  - Multiple properties don't share the inverted index, one inverted index per property, this means that if you have a mapping with multiple text/keyword types, each of those properties will have their own inverted index

Example of 2 sentences indexed
a dog barks at you
a cat meows at you

        Doc 1     Doc 2
a        x          x
at       x          x
barks    x
cat                 x
dog      x
meows               x
you      x          x

       

### Date types

- There are 3 formats which can be accepted
  - ISO date format where its yyyy-MM-dd
  - Date with time where yyyy-MM-ddThh:mm:ssZ : where Z is the UTC timezone. or hh:mm:ss+01:00 with a timezone offset
  - Milliseconds since epoc
- Note, do not use unix time as that is seconds since epoc, so you should multiply by 1000 to make it milliseconds
- The dates are stored internally as a long in UTC so whatever you give is converted and stored. So whenever you do a search, the search term is converted too before searching
- Date formats can be provided in the mapping to define the formats used during parsing, multiple can be provided with a double pipe separator
  - Other formats include
    - `dd/MM/yyyy`
    - `epoc_second` 
```json
{
  "property": {
    "createdDate": {
      "type": "date",
      "format": "strict_date_optional_time||epoch_millis" //this is the default
    }
  }
}
```


### Objects vs Nested

- To create a mapping file for Object and Nested fields, simply use the `properties` key in the mapping json
- Objects are implied when you use `properties`, unless you define it as the `nested` type
```json
{
  "properties" : {
    "owner": {
      "type": "nested",
      "properties": {
        "name": { "type":  "keyword" }
      }
    }
  } 
}
```

```json
{
  "car": "Honda Jazz",
  "previousOwners": [
    { "name": "Paul", "age": 20 },
    { "name": "Bob", "age":  30 }
  ]
}
```
```json
{
  "car": "Honda Civic",
  "previousOwners": [
    { "name": "Paul", "age": 50 },
    { "name": "Dan", "age":  30 }
  ]
}
```

- If using `Object` types, the above docs will be flatterned out into arrarys and internal objects will lose their relationships to their properties
e.g.
```json
[
{
  "car": "Honda Jazz",
  "previousOwners.name": ["Paul", "Bob"],
  "previousOwners.age": [20, 30]
},
{
  "car": "Honda Civic",
  "previousOwners.name": ["Paul", "Dan"],
  "previousOwners.age": [50, 30]
}
]
```

- So if I did a query where name = "Paul" and age >= 20, both documents will be returned because both have "Paul" and both have ages above 20
- To fix the above, you need to use the `nested` data type. If you use an array of objects, think `nested`
- Another difference between Objects and Nested is that nested documents will be indexed seperatly, meaning that if you have 2 nested object, 3 documents will be saved, 2 nested and 1 outer document that holds it all

#### Field configuration property parameters

- Most of these field parameters will save space and therefore will also speed up indexing time
- `doc_values` (doc values) are an on disk data structure that contains indexed fields (most fields are automatically indexed) that allows you to do document based searching against a field, as oppsosed to term based searches
  - These can end up taking quite a bit of disk space, so it is possible to disable it but you should always think about doing this as it will disable sorting, aggreations and access to fields in scripts
  - To set this value, you must reindex all of the documents
  - To disable doc values for a field, use the `doc_values` property e.g. `"name": { "type": "keyword", "doc_values": false }` 
- `norms` (normalization) is used to calculate relevance when doing searching
  - For fields where you just want to do sorting or filtering e.g. fields like "tags" where you either want a hit or not, its sometimes worth thinking about disabling norms as you don't need relevance 
  - Like doc values it uses a lot of disk space 
  - It can be disabled by using the `norms` property for a field e.g. `"name": { "type": "keyword", "norms": false }`
- `index` is used to indicate if a field property is indexed or not
  - When index is disabled, that field cannot be queried against
  - When disabled, the field is still stored within the `_source` field, so you will still get the data when retrieving a document
  - When disabled, the field can still be used in aggregations
  - To disable a field from being indexed do: `"name": { "type": "integer", "index": false }`
- Nulls
  - nulls are treated strangly in ES
  - null values are ignored in ES by default (they cannot be indexed), that includes null values, an array of null (an empty array does not count)
  - If you ever wanted to search for documents that have properties with null, you will need to use the `null_values` configuration property
  - `null_values` does a search and replace of null values, and replaces them with a term of your choice, this is so that you can then do searches for that term if you wish to search for null values
  - To enable null values, the replacement MUST be the same data type of the property when not null
  - `"nickname": { "type": "keyword", "null_value": "NULL" }` or a different type `"age": { "type": "integer", "null_value": -1 }`
- copy_to can be used to copy and combine other values into a new field
  - this uses the original value and not the terms that come out of text analyser
  - This is useful for fields like `fullname` where we index `firstname` and `lastname` and we configure both of those 2 fields to use copy_to
  - You can copy a vaule to multiple fields by specifying an array
  - Does not support copy traversal, so if the target field is configured with `copy_to` that field won't trigger a copy to another field
  - The new field is not stored in the `_source` field and therefore won't come back from document retrieval
  - The new field can be configured to have text analysers and can be queried against
  - The new field is also known as a runtime only field
- `ignore_above` is used to define the amount of characters a field can have before being ignored for indexing and storage
  - For arrays, it's applied to the individual elements

#### Text analyzers

- Text analyzers are used to apply text analysis on `text` type fields, there are many out of the box analyzers: standard, simple, whitespace, stop etc or your own custom
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

- Search with spring data ES can be done just like jpa, where you extend the `ElasticsearchRepository` and provide abstract methods with names like `findByX(String x)`
- You can also use the `@Query` annotation 

 
### General Notes

- Text vs Keyword property types, Text is for general text search that allows processing while Keyword is for SQL == type of searches