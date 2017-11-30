import java.nio.ByteBuffer;
import java.util.ArrayList;

public class Serialization {
    public static byte[] serialize(String[] strs) {
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

    public static String[] unserialize(byte[] bytes) {
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

    public static void main(String[] args) {
        String[] input = {"This is","a serialization problem;","string concatenation will do as well","in some cases."};
        byte[] byteArray = serialize(input);
        String[] output = unserialize(byteArray);
        for (String str: output) {
            System.out.println(str);
        }
    }
}