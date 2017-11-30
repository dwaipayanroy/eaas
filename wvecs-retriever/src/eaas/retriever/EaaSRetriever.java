/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eaas.retriever;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

/**
 *
 * @author dwaipayan
 */
public class EaaSRetriever {

    String  protocol;
    String  host;
    int     port;
    String  file;

    String      coll;
    Boolean     analyzed;
    String      model;
    int         dim;

    Boolean     toAnalyze;  // whether to analyze the query words or not
    String      words;      // the query words
    String      urlStr;
    URL         url;

    String      serviceName;
    String      func;       // func=[vec]/knn for kNN computation
    int         k;          // the value of K for kNN computation

    /**
     * Need to set this according to the server settings.
     */
    public EaaSRetriever() {

        protocol = "http";
        host = "192.168.57.110";
//        host = "localhost";
        port = 8080;
        file = "/ws-wvecs/";    // to be appended with 'WVecRetrieverServlet' OR, 'NNRetrieverServlet'.
    }

    /**
     * Depending on the configurations, makes the URL pattern to be sent to the 
     * server for retrieval.
     * @return The retrieval URL, converted into string, that is sent to the server.
     * @throws MalformedURLException in case the formed URL is malformed.
     */
    String makeURL() throws MalformedURLException {

        urlStr = String.format("%s?coll=%s&analyzed=%s&model=%s&dim=%d&word=%s&toAnalyze=%s&func=%s&k=%d", 
                new URL(protocol, host, port, file).toString(), coll, analyzed.toString(), model, dim, words, toAnalyze.toString(), func, k);

        //System.out.println(urlStr);
        url = new URL(urlStr);

        return urlStr;
    }

    /**
     * Returns the URL in string form.
     * @return The retrieval URL, converted into string, that is sent to the server.
     */
    public String returnURLStr() { return urlStr; }

    /**
     * The input file have to contain the following informations in the following way: <p>
     * coll=collection-name <p>
     * analyzed=true/false <p>
     * model=cbow/skipgram <p>
     * dim=200/500 - dimension of the embeddings to be used <p>
     * words=space-separated-multiple-words <p>
     * func=vec/knn (in case vector OR, kNN is requested to be retrieved)
     * k=integer (how many similar terms are requested, default - 30)
     * 
     * @param configFile The input configuration file
     * @throws java.io.FileNotFoundException In case the configuration file is missing
     * @throws java.io.IOException Input-Output Exception
     */
    void parseInput(String configFile) throws FileNotFoundException, IOException {

        Properties prop = new Properties();
        prop.load(new FileReader(configFile));

        coll = prop.getProperty("coll");
        analyzed = Boolean.parseBoolean(prop.getProperty("analyzed"));
        model = prop.getProperty("model");
        dim = Integer.parseInt(prop.getProperty("dim"));
        toAnalyze = Boolean.parseBoolean(prop.getProperty("toAnalyze"));
        words = prop.getProperty("words");
        words = words.replace(" ", ":");

        func = prop.getProperty("func","vec");
        if("knn".equalsIgnoreCase(func)) {
            k = Integer.parseInt(prop.getProperty("k", "30"));
            serviceName = "NNRetrieverServlet";
        }
        else
             serviceName = "WVecRetrieverServlet";
        file = file + serviceName;
    }

    /**
     * Performs the actual requests to the server.
     */
    void request(){

        try {
            //url = new URL( "http://192.168.57.110:8080/ws-wvecs/WVecRetrieverServlet?coll=foo&analyzed=true&word=foreign" );
            URLConnection con = url.openConnection();  
            con.setDoInput(true);  
            con.setDoOutput(true);  
            con.setUseCaches(false);
            con.setDefaultUseCaches(false);  
            con.setRequestProperty("Content-Type", "text/xml");  

            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));

            String inputLine;
            while ((inputLine = br.readLine()) != null) {
                System.out.println(inputLine);
            }
        }

        catch(ConnectException ex) {
            System.err.println("Error connecting to the server. Try again or contact the admin.");
        } catch (IOException ex) {
            System.err.println("Exception occured: " + ex);
        }

    }

    /**
     * The client-end function that is to be called for the retrieval.
     * @param configFile To contain the following settings: <p>
     * coll=name of collection of wordvector to be used for retrieval <p>
     * analyzed=whether to use stop-stemmed version of word vector <p>
     * model=cbow/skipgram <p>
     * dim=200/500 <p>
     * words=list of space separated words for which the similar terms are to be retrieved<p>
     * @throws IOException Input-Output Exception
     */
    public void submitRequest(String configFile) throws IOException {

        parseInput(configFile);
        makeURL();
        request();

    }

    /**
     * For unit testing.
     * @param args the command line arguments
     * @throws java.io.IOException Input-Output Exception
     */
    public static void main(String[] args) throws IOException {

        String usage = "java ws.retriever.WvecsRetriever <prop-file>\n"
            + "The prop file must contain: \n"
            + "coll=\n"
            + "analyzed=true/false\n"
            + "model=cbow/skipgram\n"
            + "dim=200/300\n"
            + "words=<list of space separated words>\n"
            + "func=[vec]/knn (in case vector OR, kNN is requested to be retrieved)\n"
            + "k=integer (how many similar terms are requested, default - 30)";
        EaaSRetriever obj = new EaaSRetriever();

        if(args.length != 1) {
            System.out.println("Usage: " + usage);
            obj.submitRequest("/home/dwaipayan/Dropbox/programs/wvec-reproducibility/wvecs-retriever/init.properties");
        }

        else
            obj.submitRequest(args[0]);

    }
    
}
