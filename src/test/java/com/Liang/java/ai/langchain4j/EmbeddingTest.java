package com.Liang.java.ai.langchain4j;


import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@ActiveProfiles("pinecone")
@SpringBootTest
@Tag("external")
public class EmbeddingTest {

    @Autowired
    private EmbeddingModel embeddingModel;
    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Test
    public void testPineconeEmbedding() {

        //将文本转换成向量
        TextSegment segment1 = TextSegment.from("我喜欢打羽毛球");
        Embedding embedding1 = embeddingModel.embed(segment1).content();
        //存入向量数据库
        embeddingStore.add(embedding1, segment1);

        TextSegment segment2 = TextSegment.from("今天天气很好");
        Embedding embedding2 = embeddingModel.embed(segment2).content();
        embeddingStore.add(embedding2, segment2);
    }

    @Test
    public void testUploadKnowledgeLibrary() {

        //使用FileSystemDocumentLoader读取指定目录下的知识库文件
        //并使用默认的文档解析器对文档进行解析

        Document document1 = FileSystemDocumentLoader.loadDocument("src/main/resources/knowledge/医院信息.md");
        Document document2 = FileSystemDocumentLoader.loadDocument("src/main/resources/knowledge/科室信息.md");
        Document document3 = FileSystemDocumentLoader.loadDocument("src/main/resources/knowledge/神经内科.md");

        List<Document> documents = List.of(document1, document2, document3);

        //文本向量化并存入向量数据库中，将每个片段进行向量化，得到一个嵌入向量
        EmbeddingStoreIngestor
                .builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .build()
                .ingest(documents);

    }
}
