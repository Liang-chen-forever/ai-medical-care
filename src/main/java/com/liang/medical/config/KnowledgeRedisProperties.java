package com.liang.medical.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.knowledge.redis")
public class KnowledgeRedisProperties {

    private String host = "localhost";
    private int port = 6379;
    private String indexName = "xiaozhi-index";
    private String prefix = "langchain4j:vector:xiaozhi:";
    private int dimension = 1024;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public int getDimension() {
        return dimension;
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
    }
}
