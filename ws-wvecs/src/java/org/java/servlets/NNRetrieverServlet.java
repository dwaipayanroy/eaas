/*
 * TODO
 */
package org.java.servlets;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import static java.lang.System.out;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import wvec.NNRetriever;
import wvec.WordNN;

class NNs {
    String  nn;
    float   cosineSim;
}

class CachedNNs {

    String collKey;
    HashMap<String, List<NNs>> termNNs;
}
/**
 *
 * @author Debasis
 */
public class NNRetrieverServlet extends HttpServlet {

    HashMap<String, NNRetriever>   nnRetrieverMap;
    NNRetriever retriever;
    String outputStr;

    String coll;
    Boolean isAnalyzed;
    String model;
    int dim;

    Boolean     queryToAnalyze;     // whether the query terms to analyze or not
    Analyzer    analyzer;           // query term analyzer
    String[]    queryWords;         // the query words

    int         k;                  // k for KNN
    String      outputFormat;             // output-format: text/json

    public void init(String propFile) {

        try {
            String propFileName = propFile;

            // Create an object for either WT10G or ClueWeb retriever
            Properties prop = new Properties();
            prop.load(new FileReader(propFileName));

            String coll;
            String nnIndexBasedir = prop.getProperty("nnIndexBasedir");

            System.out.println(nnIndexBasedir);
            // +++ opening all the vector indices
            File[] directories = new File(nnIndexBasedir).listFiles(File::isDirectory);
            nnRetrieverMap = new HashMap<>();
            for(File directory : directories) {
                coll = directory.getName();
                retriever = new NNRetriever(directory.getAbsolutePath());
                System.out.println("Opening Index: " + coll);
                nnRetrieverMap.put(coll, retriever);
            }
            // --- opening all the vector indices

            // +++ Initializing default settings of word2vec training
            model = "cbow";
            dim = 200;
            // ---

            // +++ initialize the analyzer to analyze the query terms
            String filePath = prop.getProperty("stopFilePath");

            FileReader fr = new FileReader(filePath);
            BufferedReader br = new BufferedReader(fr);
            String line;
            List<String> stopwords = new ArrayList<>();
            while ( (line = br.readLine()) != null )
                stopwords.add(line.trim());

            analyzer = new EnglishAnalyzer(StopFilter.makeStopSet(stopwords));
            // ---
        }
        catch (Exception ex) {
            ex.printStackTrace();            
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {

        String propFileName = config.getInitParameter("configFile");

        init(propFileName);
    }

//    List<NNs> getCachedNN(HttpServletRequest request, String query) {
//
//        String key;
//        HttpSession session = request.getSession();
//
//        HashMap<String, List<NNs>> cachedNNs = (HashMap<String, List<NNs>>) session.getAttribute(key);
//
//        if(null == cachedNNs) {
//            
//        }
//        return cachedNNs.get(query);
//    }

    public void setParameters(HttpServletRequest request) {

        Enumeration paramNames = request.getParameterNames();

        // setting the default values for the following parameters
        isAnalyzed = true;
        queryToAnalyze = false;
        k = 30;
        outputFormat = "text";

        while(paramNames.hasMoreElements()) {
            String paramName = (String)paramNames.nextElement();
            switch (paramName) {

                case "coll":
                    coll = request.getParameter(paramName);
                    break;

                case "word":
                case "words":
                    queryWords = request.getParameter(paramName).split(":");
                    break;

                case "toAnalyze":
                    queryToAnalyze = Boolean.parseBoolean(request.getParameter(paramName));
                    break;

                case "k":
                    k = Integer.parseInt(request.getParameter("k"));
                    if(k > 100)
                        k = 100;
                    break;

                // +++ TODO: To add the following 

                case "model":
                    model = request.getParameter("model");
                    break;

                case "analyzed":
                    isAnalyzed = Boolean.parseBoolean(request.getParameter(paramName));
                    break;

                case "dim":
                    dim = Integer.parseInt(request.getParameter(paramName));
                    break;

                case "output":
                    outputFormat = request.getParameter("output");
                    break;
                // --- TODO
            }
        }
    }

    /**
     * Returns a string with the value: <p>
     *  - Index not exists;
     *  - Term - Vector not found;
     *  - (The NN list with their cosine similarity)
     * 
     * @param request
     * @param response
     * @return 
     * @throws ServletException
     * @throws IOException 
     */
    protected String processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String responseString = "";

        response.setContentType("text/html;charset=UTF-8");

        try {
            PrintWriter out = response.getWriter();

            setParameters(request);

            retriever = nnRetrieverMap.get(coll);

            if (retriever == null) {
                responseString = "Index not exists.";
                out.println("Index doesn't exists.");
            }
            else {

                // +++ Analyzing the query terms: Removing stopwords and stemming
                if(queryToAnalyze) {
                    TokenStream stream = analyzer.tokenStream("", new StringReader(Arrays.toString(queryWords)));
                    CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);
                    stream.reset();

                    StringBuffer tokenizedContentBuff = new StringBuffer();
                    String term;
                    while (stream.incrementToken()) {
                        term = termAtt.toString();
                        tokenizedContentBuff.append(term).append(" ");
                    }

                    stream.end();
                    stream.close();

                    queryWords = tokenizedContentBuff.toString().split(" ");
                }
                // --- Analyzing done

                WordNN wnn;

                for(String queryWord : queryWords) {        // for each query words

                    JSONObject obj = new JSONObject();
                    JSONArray list = new JSONArray();
                    obj.put("word", queryWord);

                    wnn = retriever.retrieve(queryWord);

                    if(wnn != null) {
                        outputStr = wnn.toString();
                        String nn[] = wnn.getNN();
                        for(int i=0; i<k; i++)
                            list.add(nn[i]);
                        obj.put("nns", list);

                        responseString = outputStr;
//                        out.println(outputStr);
                        request.setAttribute("Word", queryWord);
                        request.setAttribute("Vector", outputStr);
                    }
                    else {
                        obj.put("nns", "not found");
                        request.setAttribute("Word", queryWord);
                        request.setAttribute("Vector", "Not Found");
                        responseString = queryWord + " Not found";
//                        out.println(queryWord + " Not found");
                    }
                    out.println(obj.toJSONString());

                    request.setAttribute("VecValue", outputStr);
//                    request.getRequestDispatcher("/index.jsp").forward(request, response);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        return responseString;
    }

    /* // <editor-fold defaultstate="collapsed" desc="HttpServlet methods: doGet(), doPost(). Click + to edit code."> */
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        PrintWriter out = response.getWriter();
        String responseString = processRequest(request, response);
        request.getRequestDispatcher("/index.jsp").forward(request, response);

        out.println("<html>");
        out.println("<body>");
        out.println("<h1>" + responseString + "</h1>");
        out.println("</body>");
        out.println("</html>");
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }

    /**
     * Closes all the index readers.
     */
    @Override
    public void destroy() {
        for (Map.Entry<String, NNRetriever> eachRetriever : nnRetrieverMap.entrySet()) {
            try {
                eachRetriever.getValue().close();
            } catch (Exception ex) {
                Logger.getLogger(NNRetrieverServlet.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    // </editor-fold>

    public static void main(String[] args) throws ServletException {

        String propFileName;

        NNRetrieverServlet obj = new NNRetrieverServlet();

        try {
            if(args.length == 1) {
                propFileName = args[0];
            }
            else {
                //propFileName = "/user1/faculty/cvpr/irlab/dwaipayan/programs/wvec-reproducibility/servlet-init.properties";
                propFileName = "/home/dwaipayan/Dropbox/programs/wvec-reproducibility/servlet-init.properties";
            }
            
            Properties prop = new Properties();
            prop.load(new FileReader(propFileName));

            obj.init(propFileName);
            NNRetriever retriever;
            //retriever = new NNRetriever("/user1/faculty/cvpr/irlab/collections/w2v-nn-index/gov2");
            retriever = new NNRetriever("/home/dwaipayan/nnIndex/gov2");
//            if (retriever.isIndexExists) {
//                System.out.println(retriever.retrieve("foreign"));
//                System.out.println(retriever.retrieve("minorit"));
//                System.out.println(retriever.retrieve("german"));
//            }
            retriever = obj.nnRetrieverMap.get("gov2");
            if (retriever == null) {
                System.out.println("<h1>Index doesn't exists.</h1> Exiting...");
            }
            else {
                System.out.println("index exists");
                WordNN wvec = retriever.retrieve("river");
                if(wvec != null) {
                    Arrays.stream(wvec.toString().split(",")).limit(10).forEach(out::println);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();            
        }                
    }
}
