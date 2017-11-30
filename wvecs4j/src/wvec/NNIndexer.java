/*
 * TODO
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
public class NNIndexer {

    IndexWriter writer;
    int k;
    String coll;
    String nnPath;
    String nnIndexPath;
    
    public NNIndexer(Properties prop) {

        coll = prop.getProperty("coll");
        String nnBasedir = prop.getProperty("nnBasedir");
        nnPath = nnBasedir + coll + "/" + coll + "." + k + "nn";
        String nnIndexBasedir = prop.getProperty("nnIndexBasedir");
        nnIndexPath = nnIndexBasedir + coll;
    }

    public NNIndexer(String propFilePath) throws IOException {

        Properties prop = new Properties();
        prop.load(new FileReader(propFilePath));

        k = Integer.parseInt(prop.getProperty("k"));
        coll = prop.getProperty("coll");
        nnPath = prop.getProperty("nnPath");
        String nnIndexBasedir = prop.getProperty("nnIndexBasedir");
        nnIndexPath = nnIndexBasedir + coll;
    }

    /**
     * Read the output .vec file of the C wordvec output and write
     * out an index in Lucene.
     * @throws Exception 
     */
    public void writeIndex() throws Exception {
        
        IndexWriterConfig iwcfg = new IndexWriterConfig(new WhitespaceAnalyzer());
        iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        writer = new IndexWriter(FSDirectory.open(new File(nnIndexPath).toPath()), iwcfg);        
        
        indexFile(new File(nnPath));

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
            
            WordNN wnn = new WordNN(line);
            Document luceneDoc = wnn.constructDoc();
            
            if (count%batchSize == 0) {
                System.out.println("Added " + count + " NN words...");
            }
            
            writer.addDocument(luceneDoc);
            count++;
        }
        br.close();
        fr.close();
    }

    public static void main(String[] args) throws Exception {

        if( args.length != 1) {
            System.err.println("NNIndexer Usage: \n"
                + "java NNIndexed <properties>");
//            System.exit(0);
            args = new String[1];
            args[0] = "/home/dwaipayan/Dropbox/programs/wvec-reproducibility/wvecs4j/nn-index-init.properties";
        }
        NNIndexer nnIndexer;

        nnIndexer = new NNIndexer(args[0]);

        nnIndexer.writeIndex();
    }
}
