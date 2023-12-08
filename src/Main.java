import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Main {

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        BSTree tree = initializeTree();
        String myString = "36982";
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] bytesOfString = myString.getBytes("UTF-8");
        byte[] msgDigest = md.digest(bytesOfString);
        String word = new String(msgDigest, "UTF-8");
        Long start = System.currentTimeMillis();
        Long end;
        int checksum = 0;
        for(int j = 0; j < word.length(); j++){
            checksum += word.charAt(j);
        }
        TreeNode n = tree.find(checksum);
        int num = n.getNumberForHash(word);
        end = System.currentTimeMillis();
        System.out.println("The number is: " + num + " time required " + (end-start) + " milliseconds");
        System.out.println("Start: " + start);
        System.out.println("End: " + end);
    }

    public static BSTree initializeTree() throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        int i = 0;
        byte[] guess = md.digest((String.valueOf(i).getBytes("UTF-8")));
        String guessStr = new String(guess, "UTF-8");
        int checksum = 0;
        for(int j = 0; j < guessStr.length(); j++){
            checksum += guessStr.charAt(j);
        }
        BSTree tree = new BSTree(checksum, guessStr, i);
        for (i = 1; i < 99999999; i++){
            guess = md.digest((String.valueOf(i).getBytes("UTF-8")));
            guessStr = new String(guess, "UTF-8");
            checksum = 0;
            for(int j = 0; j < guessStr.length(); j++){
                checksum += guessStr.charAt(j);
            }
            tree.add(checksum, guessStr, i);
        }
        return tree;
    }
}

