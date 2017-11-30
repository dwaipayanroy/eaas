/**
 * 
 */

package dumpindex;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;

/**
 *
 * @author dwaipayan
 */
public class DumpDoc {
    
    String      propPath;
    Properties  prop;               // prop of the init.properties file
    String      collPath;           // path of the collection
    String      collSpecPath;       // path of the collection spec file
    File        collDir;            // collection Directory
    String      stopFilePath;
    Boolean     toAnalyze;
    Analyzer    analyzer;           // the analyzer

    IndexWriter indexWriter;
    boolean     boolIndexFromSpec;  // true; false if indexing from collPath
    int         docProcessedCounter;  // document indexed counter
    boolean     boolDumpIndex;      // true if want ot dump the entire collection
    String      dumpPath;           // path of the file in which the dumping to be done

    /**
     * 
     * @param propPath
     * @throws Exception 
     */
    public DumpDoc(String propPath) throws Exception {

        this.propPath = propPath;
        prop = new Properties();
        try {
            prop.load(new FileReader(propPath));
        } catch (IOException ex) {
            System.err.println("Error: prop file missing at: "+propPath);
            System.exit(1);
        }
        // ----- properties file set

        stopFilePath = prop.getProperty("stopFilePath");

        toAnalyze = Boolean.parseBoolean(prop.getProperty("toAnalyze"));
        if(toAnalyze) {
//            // +++++ setting the analyzer with English Analyzer with Smart stopword list
//            common.EnglishAnalyzerWithSmartStopword engAnalyzer = new common.EnglishAnalyzerWithSmartStopword(stopFilePath);
//            analyzer = engAnalyzer.setAndGetEnglishAnalyzerWithSmartStopword();
//            // ----- analyzer set: analyzer

            analyzer = new common.WebDocAnalyzer(stopFilePath);
        }
        else
            analyzer = new SimpleAnalyzer();

        /* collection path setting */
        if(prop.containsKey("collSpec")) {
            boolIndexFromSpec = true;
            collSpecPath = prop.getProperty("collSpec");
        }
        else if(prop.containsKey("collPath")) {
            boolIndexFromSpec = false;
            collPath = prop.getProperty("collPath");
            collDir = new File(collPath);
            if (!collDir.exists() || !collDir.canRead()) {
                System.err.println("Collection directory '" +collDir.getAbsolutePath()+ "' does not exist or is not readable");
                System.exit(1);
            }
        }
        else {
            System.err.println("Neither collPath nor collSpec is present");
            System.exit(1);
        }
        /* collection path set */

        if(!prop.containsKey("textDumpPath"))
            System.out.println("Error: dumpPath not set in prop file");
        else
            dumpPath = prop.getProperty("textDumpPath");

    }

    /**
     * 
     * @param file 
     */
    private void processFile(File file) {

        try {

            DocIterator docs = new DocIterator(file, analyzer, this.dumpPath);

            Document doc;
            while (docs.hasNext()) {
                doc = docs.next();
                if (doc != null) {
                    System.out.println((++docProcessedCounter)+": Dumped doc: ");
                }
            }
        } catch (FileNotFoundException ex) {
            System.err.println("Error: '"+file.getAbsolutePath()+"' not found");
            ex.printStackTrace();
        }

    }

    private void processDirectory(File collDir) throws Exception {

        File[] files = collDir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                System.out.println("Indexing directory: " + file.getName());
                processDirectory(file);  // recurse
            }
            else {
                System.out.println("Indexing file: " + file.getAbsolutePath());
                processFile(file);
            }
        }
    }

    public void dumpCollection() throws Exception {

        System.out.println("Dumping started");

        if (boolIndexFromSpec) {
            /* if collectiomSpec is present, then index from the spec file */
            System.out.println("Reading from spec file at: " + collSpecPath);

            try (BufferedReader br = new BufferedReader(new FileReader(collSpecPath))) {
                String line;

                while ((line = br.readLine()) != null)
                    processFile(new File(line));

            }
        }
        else {
            if (collDir.isDirectory())
                processDirectory(collDir);
            else
                processFile(collDir);
        }

        System.out.println("Dumping ends\n"+docProcessedCounter + " documents dumped in: "+dumpPath);
    }

    public static void main(String[] args) throws Exception {

        DumpDoc dumper;

        String usage = "Usage: java Wt10gIndexer <init.properties>\n"
            + "Properties file must contain:\n"
            + "1. collSpec = path of the spec file containing the collection spec OR,\n"
            + "   collPath = path of location where the files of the collection are stored \n"
            + "2. textDumpPath = path of the file to dump the content\n"
            + "3. toAnalyze = true/false depending on whether to stop-stem or not\n"
            + "4. stopFilePath = path of the stopword list file\n";

        if(args.length == 0) 
        {
            System.out.println(usage);
//            System.exit(1);
            args = new String[3];
            args[0] = "/home/dwaipayan/Dropbox/programs/wvec-reproducibility/wvecs4j/init.properties";
        }

        System.out.println(args[0] + args[1]);
        dumper = new DumpDoc(args[0]);

        dumper.dumpCollection();
    }

}
