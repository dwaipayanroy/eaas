/**
 * Drops content of <DOCNO>, <DOCHDR>; 
 * Index contents, dropping all HTML-like tags and removing URLs; <p>
 * Removes ':', '_'
 * Tested for WT2G and GOV2 Collection.
 */

package dumpindex;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author dwaipayan
 */
public class DocIterator implements Iterator<Document> {

    protected BufferedReader rdr;
    protected boolean at_eof = false;
    Analyzer    analyzer;
    String      dumpPath;           // path to dump the analyzed content 
    boolean     toIndexRefinedContent; // whether to index the refined content; default - true

    public DocIterator(File file) throws FileNotFoundException {
        rdr = new BufferedReader(new FileReader(file));
//            System.out.println("Reading " + file.toString());
    }

    public DocIterator(File file, Analyzer analyzer, String dumpPath) throws FileNotFoundException{
        rdr = new BufferedReader(new FileReader(file));
        this.analyzer = analyzer;
        this.dumpPath = dumpPath;
    }

    @Override
    public boolean hasNext() {
        return !at_eof;
    }

    /**
     * Removes the HTML tags from 'str' and returns the resultant string
     * @param str
     * @return 
     */
    public String removeHTMLTags(String str) {

        String tagPatternStr = "<[^>\\n]*[>\\n]";
        Pattern tagPattern = Pattern.compile(tagPatternStr);

        Matcher m = tagPattern.matcher(str);
        return m.replaceAll(" ");
    }

    /**
     * Removes URLs from 'str' and returns the resultant string
     * @param str
     * @return 
     */
    public String removeURL(String str) {
        String urlPatternStr = "\\b((https?|ftp|file)://|www)[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
        Pattern urlPattern = Pattern.compile(urlPatternStr);

        Matcher m = urlPattern.matcher(str);
        return m.replaceAll(" ");
    }


    /**
     * Removes strings with digits and punctuation on them.
     * @param tokens
     * @return 
     */
    public String refineSpecialChars(String tokens) {

        if(tokens!=null)
            tokens = tokens.replaceAll("\\p{Punct}+", " ");

        return tokens;
    }

    /**
     * Returns the next document in the collection
     * @return 
     */
    @Override
    public Document next() {

        Document doc = new Document();
        StringBuffer refinedTxtSb = new StringBuffer();

        // +++ For replacing characters- ':','_'
        Map<String, String> replacements = new HashMap<String, String>() {{
            put(":", " ");
            put("_", " ");
        }};
        // create the pattern joining the keys with '|'
        String regExp = ":|_";
        Pattern p = Pattern.compile(regExp);
        // --- For replacing characters- ':','_'

        try {
            String line;

            boolean in_doc = false;
            String doc_no = null;

            while (true) {
                line = rdr.readLine();

                if (line == null) {
                    at_eof = true;
                    break;
                }
                else if (line.isEmpty())
                    continue;
                else
                    line = line.trim();

                // +++ <DOC>
                if (!in_doc) {
                    if (line.startsWith("<DOC>")) {
                        in_doc = true;
                        continue;
                    }
                }
                if (line.contains("</DOC>")) {
                    if(in_doc) {
                        in_doc = false;

                        refinedTxtSb.append(line);
                    }
                    break;
                }
                // --- </DOC>

                // +++ <DOCNO>
                if(line.startsWith("<DOCNO>")) {
                    doc_no = line;
                    while(!line.endsWith("</DOCNO>")) {
                        line = rdr.readLine().trim();
                        doc_no = doc_no + line;
                    }
                    doc_no = doc_no.replace("<DOCNO>", "").replace("</DOCNO>", "").trim();

                    // Field: FIELD_ID
                    // the unique document identifier
                    doc.add(new StringField("", doc_no, Field.Store.YES));
                    continue;   // start reading the next line
                }
                // --- </DOCNO>

                // +++ ignoring DOCOLDNO and DOCHDR
                // <DOCOLDNO>
                if(line.startsWith("<DOCOLDNO>")) {

                    while(!line.endsWith("</DOCOLDNO>"))
                        line = rdr.readLine().trim();
                    continue;   // start reading the next line
                } // </DOCOLDNO>

                // +++ <DOCHDR>
                if(line.startsWith("<DOCHDR>")) {

                    while(!line.endsWith("</DOCHDR>")) {
                        line = rdr.readLine().trim();
                    }
                    continue;   // start reading the next line
                } // --- </DOCHDR>
                // --- ignored DOCOLDNO and DOCHDR

                refinedTxtSb.append(line);
                refinedTxtSb.append(" ");
            }

            if (refinedTxtSb.length() > 0) {

                StringBuffer temp;
                Matcher m;
                StringBuffer tokenizedContentBuff;
                TokenStream stream;
                CharTermAttribute termAtt;


                // +++ Refined analyzed content 
                String refinedTxt = refinedTxtSb.toString();
                // +++ For replacing characters- ':','_'
                temp = new StringBuffer();
                m = p.matcher(refinedTxt);
                while (m.find()) {
                    String value = replacements.get(m.group(0));
                    if(value != null)
                        m.appendReplacement(temp, value);
                }
                m.appendTail(temp);
                refinedTxt = temp.toString();
                // --- For replacing characters- ':','_'

                refinedTxt = removeHTMLTags(refinedTxt); // remove all html-like tags (e.g. <xyz>)
                //refinedTxt = removeURL(refinedTxt);
                //refinedTxt = refineSpecialChars(refinedTxt);

                tokenizedContentBuff = new StringBuffer();

                stream = analyzer.tokenStream("", refinedTxt);
                termAtt = stream.addAttribute(CharTermAttribute.class);
                stream.reset();

                while (stream.incrementToken()) {
                    String term = termAtt.toString();
                    if(!term.equals("nbsp") && !term.equals("amp"))
                        tokenizedContentBuff.append(term).append(" ");
                }

                stream.end();
                stream.close();

                String refinedContent = tokenizedContentBuff.toString();

                // Field: FIELD_BOW
                // Analyzed content (without tag, urls).
                if(toIndexRefinedContent)
                    doc.add(new Field("", refinedContent, Field.Store.NO, Field.Index.NO));

                if(null != dumpPath) {
                    
                    FileWriter fw = new FileWriter(dumpPath, true);
                    BufferedWriter bw = new BufferedWriter(fw);
                    bw.write(refinedContent.replaceAll("\\w*\\d\\w* ", "")+"\n");
                    bw.close();
                }
            }
        } catch (IOException e) {
            doc = null;
        }
        return doc;
    } // end next()

    @Override
    public void remove() {
    }
}
