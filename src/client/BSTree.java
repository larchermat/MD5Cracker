package client;

public class BSTree {
    TreeNode root;

    public BSTree() {
        root = null;
    }

    public BSTree(int key, int val) {
        root = new TreeNode(key, val, null);
    }

    public void add(int key, int val) {
        if (root == null)
            root = new TreeNode(key, val, null);
        else
            add(root, key, val);
    }

    public void add(TreeNode n, int key, int val) {
        if (n.key == key) {
            n.add(val);
        } else if (n.key > key) {
            if (n.left != null)
                add(n.left, key, val);
            else
                n.left = new TreeNode(key, val, n);
        } else {
            if (n.right != null)
                add(n.right, key, val);
            else
                n.right = new TreeNode(key, val, n);
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