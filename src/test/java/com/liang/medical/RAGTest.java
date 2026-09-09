package com.liang.medical;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Tag("external")
public class RAGTest {

    @Test
    public void testReadDocument(){
        //使用FileSystemDocumentLoader读取指定目录下的知识库文档
        //并使用默认的文档解析器TextDocumentParser进行解析

        Document document = FileSystemDocumentLoader.loadDocument(
                "src/test/resources/knowledge/测试.txt");
        System.out.println(document.text());
    }

    @Test
    public void testParsePDF(){
        Document document = FileSystemDocumentLoader.loadDocument(
                "src/test/resources/knowledge/医院信息.pdf", new ApachePdfBoxDocumentParser());
        System.out.println(document.metadata());
        System.out.println(document.text());
    }

    @Test
    public void testReadDocumentAndStore(){

        //使用FileSystemDocumentLoader读取指定目录下的知识库文档
        //并使用默认的文档解析器TextDocumentParser进行解析
        Document document = FileSystemDocumentLoader.loadDocument(
                "src/test/resources/knowledge/人工智能.md");

        //为了简单起见，我们暂时使用基于内存的向量存储
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        //ingest
        //1.分割文档：默认使用递归分割器，将文档分割为多个文本片段，每个片段不包含超过300个token，并且有30个token的重叠部分保证连贯性
        //DocumentByParagraphSplitter(DocumentByLineSplitter(DocumentBySentenceSplitter(DocumentByWordSplitter))
        //2.文本向量化：使用LangChain4j内置的轻量化向量化器，将每个文本片段转换为向量表示
        //3.将原始文本和向量表示存储到向量存储中（InMemoryEmbeddingStore）
        EmbeddingStoreIngestor.ingest(document,embeddingStore);
        //查看向量数据库中的内容
        System.out.println(embeddingStore);
    }
}
