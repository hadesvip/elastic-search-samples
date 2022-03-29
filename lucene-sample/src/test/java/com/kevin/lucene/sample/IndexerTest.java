package com.kevin.lucene.sample;

import java.util.logging.Logger;
import org.apache.lucene.index.IndexWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 索引
 *
 * @author kevin
 */
public class IndexerTest {


  private IndexWriter writer;


  @ParameterizedTest
  @DisplayName("索引建立")
  @ValueSource(strings = {"data", "index"})
  public void indexerTest(String args) {
    Logger.getGlobal().info("args:" + args);
  }

}
