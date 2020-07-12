package com.example.business.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.util.Properties;

@Configuration
public class ClientConfigure {

    @Value("${es.cluster.name}")
    private String esClusterName;
    @Value("${es.host}")
    private String esHost;
    @Value("${es.port}")
    private int esPort;

    //将esclient注册成为一个bean
    @Bean("restHighLevelClient")
    public RestHighLevelClient getTransportClient(){
        RestClientBuilder builder= RestClient.builder(new HttpHost(esHost,esPort,"http"));
        RestHighLevelClient client = new RestHighLevelClient(builder);
        return client;
    }
}
