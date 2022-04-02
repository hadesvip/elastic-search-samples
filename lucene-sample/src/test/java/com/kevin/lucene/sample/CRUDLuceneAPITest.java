package com.kevin.lucene.sample;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * lucene API 增删改查测试
 *
 * @author kevin
 */
public class CRUDLuceneAPITest {

    private Directory directory;
    private IndexWriter indexWriter;

    /*========写入测试数据=========*/
    private final String[] idArray = {"1", "2"};
    private final String[] unIndexedArray = {"Netherlands", "Italy"};
    private final String[] unStoredArray = {"Amsterdam has lots of briges", "Venice has lots of canals"};
    private final String[] textArray = {"Amsterdam", "Venice"};


    @BeforeEach
    public void init() throws IOException {
        directory = new RAMDirectory();
        //按照空格分词
        indexWriter = buildIndexWriter();
    }

    @Test
    @DisplayName("检索测试")
    public void searchTest() throws IOException {
        addDocument();
        destroy();
        search("city", "Amsterdam");
    }

    @Test
    public void indexReaderTest() throws IOException {
        addDocument();
        destroy();
        IndexReader indexReader = DirectoryReader.open(directory);
        Logger.getGlobal().info(String.format("索引中管理的文档数量:%s,程序写入的文档数量:%s。",
                indexReader.maxDoc(), idArray.length));
        Logger.getGlobal().info(String.format("索引中管理的文档数量:%s,程序写入的文档数量:%s。",
                indexReader.numDocs(), idArray.length));
        indexReader.close();
    }


    @Test
    @DisplayName("删除文档")
    public void deleteDocumentTest() throws IOException {
        addDocument();
        indexWriter.deleteDocuments(new Term("id", "1"));
        indexWriter.commit();
        Logger.getGlobal().info("=========================");
        Logger.getGlobal().info(String.format("索引中是否包含删除标记:%s", indexWriter.hasDeletions()));
        Logger.getGlobal().info(String.format("索引中管理文档数量:%s,程序写入的文档数量:%s",
                indexWriter.maxDoc(), idArray.length));
        Logger.getGlobal().info(String.format("索引中管理文档数量:%s,程序写入的文档数量:%s",
                indexWriter.numDocs(), idArray.length));
        destroy();
    }


    @Test
    @DisplayName("删除文档并进行合并")
    public void deleteDocumentAndForceMergeTest() throws IOException {
        addDocument();
        indexWriter.deleteDocuments(new Term("id", "1"));
        //TODO
        indexWriter.forceMerge(1);
        indexWriter.commit();
        Logger.getGlobal().info("=====索引合并完成=====");
        Logger.getGlobal().info("=====================");
        Logger.getGlobal().info(String.format("索引中是否包含删除标记:%s", indexWriter.hasDeletions()));
        Logger.getGlobal().info(String.format("索引中管理文档数量:%s,程序写入的文档数量:%s",
                indexWriter.maxDoc(), idArray.length));
        Logger.getGlobal().info(String.format("索引中管理文档数量:%s,程序写入的文档数量:%s",
                indexWriter.numDocs(), idArray.length));
        indexWriter.close();
    }

    @Test
    public void updateDocumentTest() throws IOException {
        addDocument();

//        indexWriter.updateDocument();




    }


    /*=====================非核心测试方法=========================*/

    public void destroy() throws IOException {
        String content = String.format("索引中管理文档数量:%s,程序写入的文档数量:%s",
                indexWriter.numDocs(), idArray.length);
        Logger.getGlobal().info(content);
        //关闭writer或者调用writer.commit()都会将缓冲区内的索引数据落盘
        indexWriter.close();
    }

    void addDocument() throws IOException {
        for (int i = 0; i < idArray.length; i++) {
            Document document = new Document();
            document.add(new StringField("id", idArray[i], Field.Store.YES));
            document.add(new StoredField("country", unIndexedArray[i]));
            document.add(new TextField("contents", unStoredArray[i], Field.Store.NO));
            document.add(new TextField("city", textArray[i], Field.Store.YES));
            indexWriter.addDocument(document);
        }
    }

    IndexWriter buildIndexWriter() throws IOException {
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(new WhitespaceAnalyzer());
        return new IndexWriter(directory, indexWriterConfig);
    }

    void search(String fieldName, String value) throws IOException {
        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        Term term = new Term(fieldName, value);
        Query termQuery = new TermQuery(term);
        int hit = indexSearcher.search(termQuery, 1).totalHits;
        Logger.getGlobal().info(String.format("searcher hits:%s", hit));
    }

}
