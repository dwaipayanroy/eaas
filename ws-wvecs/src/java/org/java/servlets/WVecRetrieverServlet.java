/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.java.servlets;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
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
import wvec.WordVec;
import wvec.WordVecsRetriever;

/**
 *
 * @author Debasis
 */
public class WVecRetrieverServlet extends HttpServlet {

    HashMap<String, WordVecsRetriever>   wvecsRetrieverMap;
    WordVecsRetriever retriever;
    String output;

    String coll;
    Boolean isAnalyzed;
    String model;
    int dim;

    Boolean     queryToAnalyze;     // whether the query terms to analyze or not
    Analyzer    analyzer;           // query term analyzer
    String[]    queryWords;         // the query words

    Boolean     func;
    int         k;                  // k for KNN

    public void init(String propFile) {

        try {
            String propFileName = propFile;

            // Create an object for either WT10G or ClueWeb retriever
            Properties prop = new Properties();
            prop.load(new FileReader(propFileName));

            String coll;
            String wvecsIndexBasedir = prop.getProperty("wvecsIndexBasedir");

            System.out.println(wvecsIndexBasedir);
            // +++ opening all the vector indices
            File[] directories = new File(wvecsIndexBasedir).listFiles(File::isDirectory);
            wvecsRetrieverMap = new HashMap<>();
            for(File directory : directories) {
                coll = directory.getName();
                retriever = new WordVecsRetriever(directory.getAbsolutePath());
                System.out.println("Opening Index: " + coll);
                wvecsRetrieverMap.put(coll, retriever);
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

        // default settings
        isAnalyzed = true;
        queryToAnalyze = false;

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
                // --- TODO
            }
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");

        try {
            PrintWriter out = response.getWriter();

            setParameters(request);

            retriever = wvecsRetrieverMap.get(coll);

            if (retriever == null) {
                out.println("<html>");
                out.println("<body>");
                out.println("<h1>Index doesn't exists.</h1> Exiting...");
                out.println("</body>");
                out.println("</html>");
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

                WordVec wvec;

                for(String queryWord : queryWords) {

                    JSONObject obj = new JSONObject();
                    JSONObject vec = new JSONObject();
                    obj.put("word", queryWord);

                    wvec = retriever.retrieve(queryWord);

                    if(wvec != null) {
                        output = wvec.toString();
                        float v[] = wvec.getVector();
                        obj.put("vector", Arrays.toString(v).replace("[", "").replace("]", "").replace(",","").trim());
//                        out.println(output);
                        request.setAttribute("Word", queryWord);
                        request.setAttribute("Vector", wvec.toString());
                    }
                    else {
                        obj.put("vector", "OOV");
                        request.setAttribute("Word", queryWord);
                        request.setAttribute("Vector", "Not Found");
//                        out.println(queryWord + " Not found");
                    }
                    out.println(obj.toJSONString());

                    request.setAttribute("VecValue", output);
//                request.getRequestDispatcher("/index.jsp").forward(request, response);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /* // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code."> */
    // </editor-fold>
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

        processRequest(request, response);
        request.getRequestDispatcher("/index.jsp").forward(request, response);

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
        for (Map.Entry<String, WordVecsRetriever> eachRetriever : wvecsRetrieverMap.entrySet()) {
            try {
                eachRetriever.getValue().close();
            } catch (Exception ex) {
                Logger.getLogger(WVecRetrieverServlet.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void main(String[] args) throws ServletException {

        String propFileName;

        WVecRetrieverServlet obj = new WVecRetrieverServlet();

        try {
            if(args.length == 1) {
                propFileName = args[0];
            }
            else {
                //propFileName = "/home/dwaipayan/Dropbox/programs/wvec-reproducibility/servlet-init.properties";
                propFileName = "/home/dwaipayan/Dropbox/programs/wvec-reproducibility/servlet-init.properties";
            }

            Properties prop = new Properties();
            prop.load(new FileReader(propFileName));

            obj.init(propFileName);
//            WordVecsRetriever retriever = new WordVecsRetriever(prop);
            WordVecsRetriever retriever;
            //retriever = new WordVecsRetriever("/user1/faculty/cvpr/irlab/collections/foo");
            retriever = new WordVecsRetriever("home/dwaipayan/wvecsIndex/foo");
            if (retriever.isIndexExists) {
                System.out.println(retriever.retrieve("foreign"));
                System.out.println(retriever.retrieve("minorit"));
                System.out.println(retriever.retrieve("german"));
            }
            retriever = obj.wvecsRetrieverMap.get("foo");
            if (retriever == null) {
                System.out.println("<h1>Index doesn't exists.</h1> Exiting...");
            }
            else {
                System.out.println("index exists");
                System.out.println(retriever.retrieve("foreign"));
                System.out.println(retriever.retrieve("minorit"));
                System.out.println(retriever.retrieve("german"));
            }

        }
        catch (Exception ex) {
            ex.printStackTrace();            
        }                
    }
}
