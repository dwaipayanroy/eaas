/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.UAX29URLEmailTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.FilteringTokenFilter;

/**
 *
 * @author Debasis
 * Modified by Dwaipayan
 */

// TREC Web-track document

public class WebDocAnalyzer extends Analyzer {

    CharArraySet stopSet;
    String      stopFilePath;
    
    public WebDocAnalyzer(Properties prop, CharArraySet stopSet) {
        this.stopSet = stopSet;
    }
    /**
     * Constructor:
     * Assumed that the smart-stopword file is present in the path:
     *      <a href=build/classes/resources/smart-stopwords>stopword-path</a>.
     */
    public WebDocAnalyzer() {

        String filePath = new File("").getAbsolutePath();
        System.out.println(filePath);

        if(!filePath.endsWith("/build/classes")) // This will be true when running from inside IDE (e.g. NetBeans)
            filePath += "/build/classes";

        this.stopFilePath = filePath+"/resources/smart-stopwords";
        this.setStopSet();
    }

    /**
     * Constructor: 
     * The path of the stopword file is passed as argument to the constructor
     * @param stopwordPath Path of the stopword file
     */
    public WebDocAnalyzer(String stopwordPath) {
        this.stopFilePath = stopwordPath;
        this.setStopSet();
    }

    public final void setStopSet() {

        List<String> stopwords = new ArrayList<>();

        String line;
        try {
            System.out.println("Stopword Path: "+stopFilePath);
            FileReader fr = new FileReader(stopFilePath);
            BufferedReader br = new BufferedReader(fr);
            while ( (line = br.readLine()) != null )
                stopwords.add(line.trim());

            br.close(); fr.close();
        } catch (FileNotFoundException ex) {
            System.err.println("Error: \n"
                + "StandardAnalyzerWithSmartStopword: setAnalyzer()\n"
                + "Stopword file not found in: "+stopFilePath);
            System.exit(1);
        } catch (IOException ex) {
            System.err.println("Error: \n"
                + "StandardAnalyzerWithSmartStopword: setAnalyzer()\n"
                + "IOException occurs");
            System.exit(1);
        }
        stopSet = StopFilter.makeStopSet(stopwords);
    }

    @Override
    protected TokenStreamComponents createComponents(String string) {

        final Tokenizer tokenizer = new UAX29URLEmailTokenizer();

        TokenStream tokenStream = new StandardFilter(tokenizer);
        tokenStream = new EnglishPossessiveFilter(tokenStream); // removes possessives (trailing 's) from words.
        tokenStream = new LowerCaseFilter(tokenStream);         // normalizes token text to lower case.
        tokenStream = new StopFilter(tokenStream, stopSet);     // removes stopwords from token stream.
        tokenStream = new URLFilter(tokenStream);               // removes URLs.
        tokenStream = new ValidWordFilter(tokenStream);         // removes words with any special characters.
        tokenStream = new PorterStemFilter(tokenStream);        // transforms the token stream as per the Porter stemming algorithm.
        
        return new Analyzer.TokenStreamComponents(tokenizer, tokenStream);
    }
}

// Removes tokens with any special characters.
class ValidWordFilter extends FilteringTokenFilter {

    CharTermAttribute termAttr = addAttribute(CharTermAttribute.class);

    public ValidWordFilter(TokenStream in) {
        super(in);
    }
    
    @Override
    protected boolean accept() throws IOException {
        String token = termAttr.toString();
        int len = token.length();
        for (int i=0; i < len; i++) {
            char ch = token.charAt(i);
            if (!Character.isAlphabetic(ch))
                return false;
        }
        return true;
    }    
}

// Removes tokens with any URLs
class URLFilter extends FilteringTokenFilter {

    TypeAttribute typeAttr = addAttribute(TypeAttribute.class);

    public URLFilter(TokenStream in) {
        super(in);
    }
    
    @Override
    protected boolean accept() throws IOException {
        boolean isURL = typeAttr.type() == UAX29URLEmailTokenizer.TOKEN_TYPES[UAX29URLEmailTokenizer.URL];
        return !isURL;
    }    
}