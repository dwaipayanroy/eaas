/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wvec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author Debasis
 */
public class WordVecsIndexer {

    IndexWriter writer;
    String vecFile;
    String indexPath;
    
    public WordVecsIndexer(String vecFile, String indexPath) throws Exception {
        this.vecFile = vecFile;
        this.indexPath = indexPath;
    }

    public WordVecsIndexer(Properties prop) {

        String indexBasedir = prop.getProperty("wvecsIndexBasedir");
        String collName = prop.getProperty("coll");
        this.vecFile = prop.getProperty("wvecPath");
        this.indexPath = indexBasedir + "/" +collName;
    }

    public WordVecsIndexer(String propFilePath) throws IOException {

        Properties prop = new Properties();
        prop.load(new FileReader(propFilePath));
        String indexBasedir = prop.getProperty("wvecsIndexBasedir");
        String collName = prop.getProperty("coll");
        this.vecFile = prop.getProperty("wvecPath");
        this.indexPath = indexBasedir + "/" +collName;
    }

    /**
     * Read the output .vec file of the C wordvec output and write
     * out an index in Lucene.
     * @throws Exception 
     */
    public void writeIndex() throws Exception {
        
        IndexWriterConfig iwcfg = new IndexWriterConfig(new WhitespaceAnalyzer());
        iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        writer = new IndexWriter(FSDirectory.open(new File(indexPath).toPath()), iwcfg);        
        
        indexFile(new File(vecFile));
        writer.close();
        
    }
    
    void indexFile(File file) throws Exception {

        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
        String line;
        
        final int batchSize = 10000;
        int count = 0;
        
        // Each line is word vector
        while ((line = br.readLine()) != null) {
            
            WordVec wvec = new WordVec(line);            
            Document luceneDoc = wvec.constructDoc();
            
            if (count%batchSize == 0) {
                System.out.println("Added " + count + " words...");
            }
            
            writer.addDocument(luceneDoc);
            count++;
        }
        br.close();
        fr.close();
    }

    public static void main(String[] args) throws Exception {

        if( args.length != 1) {
            System.err.println("WordVecsIndexer Usage: \n"
                + "java WordVecsIndexer <properties>");
//            System.exit(0);
            args = new String[3];
            args[0] = "/home/dwaipayan/Dropbox/programs/wvec-reproducibility/wvecs4j/init.properties";
        }
        WordVecsIndexer wvecsIndexer ;

        wvecsIndexer = new WordVecsIndexer(args[0]);

        wvecsIndexer.writeIndex();
    }
}
