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
        //基于内存索引目录，程序停止后消失。
        directory = new RAMDirectory();
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


    /**
     * IndexWriter提供删除文档API:
     * 1. IndexWriter.deleteDocuments(Term term) 负责删除包含项的所有文档。
     * 2. IndexWriter.deleteDocuments(Term[] terms) 负责删除包含项数组任一元素的所有文档。
     * 3. IndexWriter.deleteDocuments(Query query) 负责删除匹配查询语句的所有文档。
     * 4. IndexWriter.deleteDocuments(Query[] queries) 负责删除匹配查询语句数组任一元素的所有文档。
     * 5. IndexWriter.deleteAll() 负责删除索引中全部文档。
     * ================================================
     * Tip:
     *
     * 如果需要通过Term删除单个文档(IndexWriter.deleteDocuments(Term term))，需要确认文档在索引时索引了指定Field字段，
     * 还需要确认该Field域字段值都是唯一的，才可以将该文档单独定位出来并删除。
     * 概念类似于数据库中的主键，需要注意该域需要设置为“不分析”的域，保证分析器不会将它分解成语汇单元。
     *
     * IndexWriter.deleteDocuments(Term term)
     * 注意：调用该方法时一定要谨慎，如果不小心创建了错误的Term对象（被分词的域，可能关联很多文档），那么Lucene
     * 将删除索引中与该Term命中的全部文档。
     * 删除操作并不会立即执行，而是会存放在缓冲区内，与加入文档类似，最后Lucene会通过周期性刷新文档目录来执行该操作。
     * 也可以通过writer.commit() 或者 writer.close() 来立即生效。即使删除操作已经完成，物理磁盘上的文件也不会立即
     * 删除，Lucene只是将被删除的文档标记为“删除”，待索引段合并时会进行真正的物理删除。
     *
     */
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

        //maxDoc：正常文档+删除文档
        //numDocs：正常文档
        Logger.getGlobal().info(String.format("索引中是否包含删除标记:%s", indexWriter.hasDeletions()));
        Logger.getGlobal().info(String.format("索引中管理文档数量:%s,程序写入的文档数量:%s",
                indexWriter.maxDoc(), idArray.length));
        Logger.getGlobal().info(String.format("索引中管理文档数量:%s,程序写入的文档数量:%s",
                indexWriter.numDocs(), idArray.length));
        indexWriter.close();
    }

    /**
     * 更新已索引的文档这个需求很常见，比如你的搜索系统依赖的源数据进行的更新，那么相对应的索引就必须进行更新，否则搜索系统的搜索准确度会降低。
     * 某些情况下，仅仅是文档的某个域需要更新，如产品名称发生了变化，但是正文未变化，非常遗憾，尽管该需求很普遍，但是Lucene做不到。
     * Lucene只能删除整个过期文档，然后再向索引中添加新文档。这要求新文档必须包含旧文档的所有域，包括内容未发生改变的域。
     * IndexWriter 提供了1个API来更新索引中的文档:IndexWriter.updateDocument(Term term, Document newDoc)
     * 第一步 删除term匹配的文档，第二步 使用writer再添加文档。
     * 例：IndexWriter.updateDocument(new Term("id", documentId), newDocument)
     * tip：由于updateDocument方法在后台会调用deleteDocuments方法，一定要确定Term标识的唯一性。
     *
     */
    @Test
    @DisplayName("更新文档")
    public void updateDocumentTest() throws IOException {
        addDocument();

        Document document = new Document();
        document.add(new StringField("id", "1", Field.Store.YES));
        document.add(new StoredField("country", "Netherlands"));
        document.add(new TextField("contents", "Den Haag has a lot of museums", Field.Store.NO));
        document.add(new TextField("city", "Den Haag", Field.Store.YES));

        indexWriter.updateDocument(new Term("id", "1"), document);
        indexWriter.close();
        search("city", "Amsterdam");
        search("city", "Haag");
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
        //按照空格分词
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
