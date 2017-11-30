package wvec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
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

public class WordNN {

    public static final String FIELD_WORD_NAME = "wordname";
    public static final String FIELD_WORD_NNS = "nns";
    
    String word;
    String nns;
    String nn[];

    /**
     * 
     * @param line 
     */
    public WordNN(String line) {
        String[] tokens = line.split(";");
        word = tokens[0];
        nns = tokens[1];

        tokens = nns.split(",");
        nn = new String[tokens.length-1];
        for (int i = 1; i < tokens.length; i++)
            nn[i-1] = tokens[i];
    }


    /**
     * To reconstruct this object from Lucene document at query time
     * @param doc 
     */
    public WordNN(Document doc) throws IOException, ClassNotFoundException {
        word = doc.get(FIELD_WORD_NAME);
        nns = doc.get(FIELD_WORD_NNS);
        BytesRef vecBytesRef = doc.getBinaryValue("nn");
        nn = (String[]) deserialize(vecBytesRef.bytes);
    }

    public String getWord() { return word; }

    public String getNNs() { return nns; }

    public String[] getNN() { return nn; }

    byte[] convertToBytes(String[] strs) {
        System.out.println(Arrays.toString(strs));
        ArrayList<Byte> byteList = new ArrayList<Byte>();
        for (String str: strs) {
            int len = str.getBytes().length;
            ByteBuffer bb = ByteBuffer.allocate(4);
            bb.putInt(len);
            byte[] lenArray = bb.array();
            for (byte b: lenArray) {
                byteList.add(b);
            }
            byte[] strArray = str.getBytes();
            for (byte b: strArray) {
                byteList.add(b);
            }
        }
        byte[] result = new byte[byteList.size()];
        for (int i=0; i<byteList.size(); i++) {
            result[i] = byteList.get(i);
        }
        return result;    
    }

    /**
     * Following serialize() and deserialize() also works.
     */
    /*
    byte[] serialize(String[] strs) {
        ArrayList<Byte> byteList = new ArrayList<Byte>();
        for (String str: strs) {
            int len = str.getBytes().length;
            ByteBuffer bb = ByteBuffer.allocate(4);
            bb.putInt(len);
            byte[] lenArray = bb.array();
            for (byte b: lenArray) {
                byteList.add(b);
            }
            byte[] strArray = str.getBytes();
            for (byte b: strArray) {
                byteList.add(b);
            }
        }
        byte[] result = new byte[byteList.size()];
        for (int i=0; i<byteList.size(); i++) {
            result[i] = byteList.get(i);
        }
        return result;
    }

    String[] deserialize(byte[] bytes) {
        ArrayList<String> strList = new ArrayList<String>();
        for (int i=0; i< bytes.length;) {
            byte[] lenArray = new byte[4];
            for (int j=i; j<i+4; j++) {
                lenArray[j-i] = bytes[j];
            }
            ByteBuffer wrapped = ByteBuffer.wrap(lenArray);
            int len = wrapped.getInt();
            byte[] strArray = new byte[len];
            for (int k=i+4; k<i+4+len; k++) {
                strArray[k-i-4] = bytes[k];
            }
            strList.add(new String(strArray));
            i += 4+len;
        }
        return strList.toArray(new String[strList.size()]);
    }
    */

    byte[] serialize(Object obj) throws IOException {
        try(ByteArrayOutputStream b = new ByteArrayOutputStream()){
            try(ObjectOutputStream o = new ObjectOutputStream(b)){
                o.writeObject(obj);
            }
            return b.toByteArray();
        }
    }

    Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try(ByteArrayInputStream b = new ByteArrayInputStream(bytes)){
            try(ObjectInputStream o = new ObjectInputStream(b)){
                return o.readObject();
            }
        }
    }

   Document constructDoc() throws Exception {        
        Document doc = new Document();
        doc.add(new Field(FIELD_WORD_NAME, this.word, Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field(FIELD_WORD_NNS, this.nns, Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new StoredField("nn", serialize(nn)));
        return doc;        
    }

    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer(word);
        buff.append(" " + nns);
        return buff.toString();
    }
}

