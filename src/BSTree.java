public class BSTree {
    TreeNode root;

    public BSTree(int key, String word, int val) {
        root = new TreeNode(key, word, val, null);
    }

    public void add(int key, String word, int val) {
        add(root, key, word, val);
    }

    public void add(TreeNode n, int key, String word, int val) {
        if(n.key == key){
            n.addPair(word, val);
        } else if (n.key > key) {
            if (n.left != null)
                add(n.left, key, word, val);
            else
                n.left = new TreeNode(key, word, val, n);
        } else {
            if (n.right != null)
                add(n.right, key, word, val);
            else
                n.right = new TreeNode(key, word, val, n);
        }
    }

    public TreeNode find(int k) {
        return find(root, k);
    }

    public TreeNode find(TreeNode n, int k) {
        if (n != null) {
            if (n.key == k)
                return n;
            if (k < n.key)
                return find(n.left, k);
            else
                return find(n.right, k);
        }
        return null;
    }

}
