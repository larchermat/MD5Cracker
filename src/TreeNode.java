import java.util.HashMap;
import java.util.Map;

public class TreeNode {
    int key;
    Map<String, Integer> wordsMap;
    TreeNode parent;
    TreeNode left;
    TreeNode right;

    public TreeNode(int key, String word, int val, TreeNode parent) {
        this.key = key;
        wordsMap = new HashMap<>();
        HashMap<String, String> mapp = new HashMap<>();
        mapp.get("boh");
        wordsMap.put(word, val);
        this.parent = parent;
        left = null;
        right = null;
    }

    public void addPair(String word, int val){
        wordsMap.put(word, val);
    }

    public int getNumberForHash(String word){
        return wordsMap.get(word);
    }

    @Override
    public String toString() {
        return key + " cheksums: " + wordsMap.size();
    }

}
