package com.kevin.lucene.sample.application;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;

/**
 * 索引检索 args: index key(查询内容)
 *
 * @author kevin
 */
public class SearcherApplication {


  public static void main(String[] args) throws IOException, ParseException {
    if (args.length != 2) {
      String errorMessage = String
          .format("Usage: java %s <index dir> <query>", SearcherApplication.class.getName());
      throw new IllegalArgumentException(errorMessage);
    }
    String indexDirectory = args[0];
    String queryKey = args[1];
    search(indexDirectory, queryKey);
  }

  private static void search(String indexDirectory, String queryStr)
      throws IOException, ParseException {
    Directory directory = FSDirectory.open(new File(indexDirectory).toPath());

    IndexReader indexReader = DirectoryReader.open(directory);
    IndexSearcher indexSearcher = new IndexSearcher(indexReader);
    QueryParser queryParser = new QueryParser("context", new StandardAnalyzer());
    Query query = queryParser.parse(queryStr);
    long start = System.currentTimeMillis();
    TopDocs hits = indexSearcher.search(query, 10);
    long end = System.currentTimeMillis();
    Logger.getGlobal()
        .info(String.format("Found %s document(s) (in %s millseconds) that matched query %s",
            hits.totalHits, end - start, queryStr));
    for (ScoreDoc scoreDoc : hits.scoreDocs) {
      Document doc = indexSearcher.doc(scoreDoc.doc);
      String path = doc.get("path");
      Logger.getGlobal().info("" + path);
    }
  }

}
