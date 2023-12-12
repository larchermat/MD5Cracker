import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

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

    public Integer getNumberForHash(String word) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        Integer number = null;
        for (Integer num : numList) {
            String hashNum;
            try {
                hashNum = new String(md.digest(String.valueOf(num).getBytes("UTF-8")), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            if (hashNum.equals(word)) {
                number = num;
                break;
            }
        }
        return number;
    }

    @Override
    public String toString() {
        return key + " cheksums: " + numList.size();
    }

}