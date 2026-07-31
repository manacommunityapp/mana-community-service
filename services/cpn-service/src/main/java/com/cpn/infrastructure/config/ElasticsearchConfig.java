package com.cpn.infrastructure.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Unused for now — nothing in this service indexes/queries through
// Elasticsearch yet, and no ES instance is provisioned locally. Commented out
// (not deleted) rather than removing the dependency, so it's a one-line
// re-enable when search is actually needed. See pom.xml (elasticsearch-java)
// and application.yml (spring.elasticsearch) for the matching commented sections.
// @Configuration
public class ElasticsearchConfig {

//    @Value("${spring.elasticsearch.uris}")
//    private String elasticsearchUri;
//
//    @Bean
//    public RestClient restClient() {
//        return RestClient.builder(HttpHost.create(elasticsearchUri)).build();
//    }
//
//    @Bean
//    public ElasticsearchClient elasticsearchClient(RestClient restClient) {
//        ElasticsearchTransport transport = new RestClientTransport(
//                restClient, new JacksonJsonpMapper());
//        return new ElasticsearchClient(transport);
//    }
}
