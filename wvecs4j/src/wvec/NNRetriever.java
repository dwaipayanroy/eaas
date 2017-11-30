/*
 * TODO
 */
package wvec;

import java.io.File;
import java.util.HashMap;
import java.util.Properties;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author Debasis
 */
public class NNRetriever {

    String          indexPath;
    IndexReader     reader;
    IndexSearcher   searcher;
    public boolean  isIndexExists;
    HashMap<String, NNRetriever>   wvecsRetrieverMap;
    NNRetriever retriever;

    /**
     * Gets called for the case when there are multiple indices.
     * @param prop
     * @throws Exception 
     */
    public NNRetriever(Properties prop) throws Exception {

        String coll;
        String wvecsIndexBasedir = prop.getProperty("wvecsIndexBasedir");

        // +++
        File[] directories = new File(wvecsIndexBasedir).listFiles(File::isDirectory);
        wvecsRetrieverMap = new HashMap<>();
        for(File directory : directories) {
            coll = directory.getName();
            retriever = new NNRetriever(directory.getAbsolutePath());
            System.out.println("Opening Index: " + coll);
            wvecsRetrieverMap.put(coll, retriever);
        }

    }

    /**
     * Constructor: Gets called for individual index.
     * @param indexPath
     * @throws Exception 
     */
    public NNRetriever(String indexPath) throws Exception {

        this.indexPath = indexPath;
        Directory indexDir = FSDirectory.open(new File(indexPath).toPath());

        if (!DirectoryReader.indexExists(indexDir)) {
            System.err.println("Index doesn't exists in "+indexPath);
            isIndexExists = false;
//            System.exit(1);
        }
        else {
            isIndexExists = true;
            reader = DirectoryReader.open(FSDirectory.open(new File(indexPath).toPath()));
            searcher = new IndexSearcher(reader);
        }
    }

    public WordNN retrieve(String word) throws Exception {

        Query q = new TermQuery(new Term(WordNN.FIELD_WORD_NAME, word));
        TopDocs topDocs = searcher.search(q, 1);

        WordNN wnn;
        if(topDocs.totalHits != 0) {
            Document retrDoc = reader.document(topDocs.scoreDocs[0].doc);
            wnn = new WordNN(retrDoc);
        }
        else
            wnn = null;
        return wnn;
    }

    public void close() throws Exception {
        reader.close();
    }
}
