package wvec;

import java.nio.ByteBuffer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.util.BytesRef;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * Reads from a word2vec file and expands the
 * query with the k-NN set of terms...

 * @author dganguly
 */

public class WordVec {

    public static final String COMPOSING_DELIM = ":";
    public static final String FIELD_WORD_NAME = "wordname";
    public static final String FIELD_WORD_VEC = "wordvec";
    
    String word;
    float[] vec;
    float norm;

    public WordVec(int dimension) { vec = new float[dimension]; }
    
    /**
     * To construct the object when reading from word2vec o/p vec file
     * @param line 
     */
    public WordVec(String line) {
        String[] tokens = line.split("\\s+");
        word = tokens[0];
        vec = new float[tokens.length-1];
        for (int i = 1; i < tokens.length; i++)
            vec[i-1] = Float.parseFloat(tokens[i]);
    }

    /**
     * To reconstruct this object from Lucene document at query time
     * @param doc 
     */
    public WordVec(Document doc) {
        word = doc.get(FIELD_WORD_NAME);
        BytesRef vecBytesRef = doc.getBinaryValue(FIELD_WORD_VEC);
        vec = toFloatArray(vecBytesRef.bytes);
    }

    float[] toFloatArray(byte[] byteArray) {
        int times = Float.SIZE / Byte.SIZE;
        float[] floats = new float[byteArray.length / times];
        for(int i=0; i<floats.length; i++){
            floats[i] = ByteBuffer.wrap(byteArray, i*times, times).getFloat();
        }
        return floats;
    }
    
    public float getNorm() {
        if (norm > 0)
            return norm;
        
        // calculate and store
        float sum = 0;
        for (int i = 0; i < vec.length; i++) {
            sum += vec[i]*vec[i];
        }
        norm = (float)Math.sqrt(sum);
        return norm;
    }
    
    public int getDimension() { return this.vec.length; }
    
    static public WordVec centroid(WordVec a, WordVec b) {
        WordVec sum = new WordVec(a.vec.length);
        sum.word = a.word + COMPOSING_DELIM + b.word;
        for (int i = 0; i < a.vec.length; i++) {
            sum.vec[i] = (a.vec[i] + b.vec[i]);
        }
        return sum;
    }
    
    public float cosineSim(WordVec that) {
        float sum = 0;
        for (int i = 0; i < this.vec.length; i++) {
            sum += vec[i] * that.vec[i];
        }
        return sum/(this.getNorm()*that.getNorm());
    }
    
    public float euclideanDist(WordVec that) {
        float sum = 0;
        for (int i = 0; i < this.vec.length; i++) {
            sum += (vec[i] - that.vec[i]) * ((vec[i] - that.vec[i]));
        }
        return (float)Math.sqrt(sum);
    }

    public String getWord() { return word; }

    public float[] getVector() { return vec; }
    
    byte[] vecToByteArray(){
        int times = Float.SIZE / Byte.SIZE;
        byte[] bytes = new byte[this.vec.length * times];
        for(int i=0; i<this.vec.length; i++){
            ByteBuffer.wrap(bytes, i*times, times).putFloat(vec[i]);
        }
        return bytes;
    }    
    
    Document constructDoc() throws Exception {        
        Document doc = new Document();
        doc.add(new Field(FIELD_WORD_NAME, this.word, Field.Store.YES, Field.Index.NOT_ANALYZED));        
        doc.add(new StoredField(FIELD_WORD_VEC, vecToByteArray()));
        return doc;        
    }

    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer(word);
        buff.append(" ");
        for (float f : this.vec) {
            buff.append(f).append(" ");
        }
        return buff.toString();
    }
}

