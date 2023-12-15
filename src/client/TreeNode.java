package client;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Nodi dell'albero binario di ricerca {@link BSTree}, che utilizzano chiavi int corrispondenti alla somma dei byte
 * degli hash, e contengono una lista con i numeri i cui hash hanno somma uguale a key
 */
public class TreeNode {
    int key;
    List<Integer> numList;
    TreeNode parent;
    TreeNode left;
    TreeNode right;

    public TreeNode(int key, int val, TreeNode parent) {
        this.key = key;
        numList = new ArrayList<>();
        numList.add(val);
        this.parent = parent;
        left = null;
        right = null;
    }

    public void add(int val) {
        numList.add(val);
    }

    /**
     * Metodo che ritorna, se esiste, il numero corrispondente ad un hash
     * @param word hash da trovare
     * @return il numero corrispondente alla soluzione, null in caso non venisse trovato
     */
     public Integer getNumberForHash(String word) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        Integer number = null;
        byte[] hashNum;
        String hash;
        for (Integer num : numList) {
            try {
                hashNum = md.digest(String.valueOf(num).getBytes("UTF-8"));
                hash = new String(hashNum, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            if (hash.equals(word)){
                number = num;
                break;
            }
        }
        return number;
    }
}