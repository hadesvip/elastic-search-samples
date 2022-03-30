package com.kevin.lucene.sample.application;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 * 索引建立
 *
 * args: index data
 * @author kevin
 */
public class IndexerApplication {


  private final IndexWriter indexWriter;

  public static void main(String[] args) throws IOException {
    if (args.length != 2) {
      String errorMessage = String
          .format("Usage: java %s <index dir> <data dir>", IndexerApplication.class.getName());
      throw new IllegalArgumentException(errorMessage);
    }
    String indexDirectory = args[0];
    String dataDirectory = args[1];
    long start = System.currentTimeMillis();
    IndexerApplication indexerApplication = new IndexerApplication(indexDirectory);
    int numIndexed = 0;
    try {
      numIndexed = indexerApplication.index(dataDirectory, new TextFileListFilter());
    } finally {
      indexerApplication.close();
    }
    long end = System.currentTimeMillis();
    Logger.getGlobal()
        .info(String.format("Indexing %s files took %s millseconds", numIndexed, end - start));

  }


  public IndexerApplication(String indexDirectory) throws IOException {
    Directory directory = FSDirectory.open(new File(indexDirectory).toPath());
    Analyzer analyzer = new StandardAnalyzer();
    IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
    indexWriter = new IndexWriter(directory, indexWriterConfig);
  }

  public int index(String dataDirectory, FileFilter filter) throws IOException {
    File[] fileArray = new File(dataDirectory).listFiles();

    for (File file : Objects.requireNonNull(fileArray)) {
      if (!file.isDirectory() && !file.isHidden() && file.exists()
          && file.canRead() && (filter == null || filter.accept(file))) {
        indexFile(file);
      }
    }
    return indexWriter.numDocs();
  }

  private void indexFile(File file) throws IOException {
    Logger.getGlobal().info(String.format("Indexing %s", file.getCanonicalPath()));
    Document doc = getDocument(file);
    indexWriter.addDocument(doc);
  }

  private Document getDocument(File file) throws IOException {
    String fileName = file.getName();
    String path = file.getPath();
    String fileContext = FileUtils.readFileToString(file, "utf-8");
    //创建Field
    //参数1:域的名称；参数2：域的内容；参数3：是否储存
    Field fieldName = new TextField("name", fileName, Field.Store.YES);
    Field fieldPath = new StoredField("path", path);
    Field fieldContext = new TextField("context", fileContext, Field.Store.YES);
    //构建Document
    Document document = new Document();
    document.add(fieldName);
    document.add(fieldPath);
    document.add(fieldContext);
    return document;
  }

  public void close() throws IOException {
    indexWriter.close();
  }

  private static class TextFileListFilter implements FileFilter {

    @Override
    public boolean accept(File pathname) {
      return pathname.getName().toLowerCase().endsWith(".txt");
    }
  }

}
