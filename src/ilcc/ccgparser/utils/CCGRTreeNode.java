/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ilcc.ccgparser.utils;

import ilcc.ccgparser.utils.DataTypes.*;

/**
 *
 * @author ambati
 */
public abstract class CCGRTreeNode {
    
    public enum Combinator {
        fa, ba, fc, bc, bx, gfc, gbc, gbx, tfc, lp, rp, conj, conjF, lpconj, rpconj, tr, lexicon, lex, other, frag,
        lreveal, rreveal, rreveal1, rreveal2, rreveal3, rreveal4, rreveal5, rreveal6, rreveal7, rreveal8, rreveal9, rreveal10, rreveal11, rreveal12, rreveal13, rreveal14, rreveal17;
    }
    
    private final CCGCategory ccgCat;    
    private final Combinator ccgComb;
    private int headDir;
    private final SCoNLLNode headNode;
    private CCGRTreeNode parent;
    
    private CCGRTreeNode(CCGCategory ccgCat, Combinator comb, int headId, SCoNLLNode headNode) {
        this.ccgCat = ccgCat;
        this.ccgComb = comb;
        this.headDir = headId;
        this.headNode = headNode;
        this.parent = null;
    }
    
    public abstract CCGRTreeNode copy();
    
    public void setParent(CCGRTreeNode node){
        this.parent = node;
    }
    public Combinator getCombinator(){
        return this.ccgComb;
    }
    
    public void setHeadDir(int dir){
        this.headDir = dir;
    }
    
    public CCGRTreeNode getParent(){
        return this.parent;
    }
    
    public int getHeadDir(){
        return this.headDir;
    }
    
    public int getLSpan(){
        CCGRTreeNode left = this;
        while(left.getLeftChild() != null)
            left = left.getLeftChild();
        
        return left.getConllNode().getNodeId();
    }
    
    public int getRSpan(){        
        CCGRTreeNode right = this;
        int childcount = right.getChildCount();
        while(childcount != 0){
            if(childcount == 2)
                right = right.getRightChild();
            else
                right = right.getLeftChild();
            childcount = right.getChildCount();
                
        }
        return right.getConllNode().getNodeId();
    }
    
    public SCoNLLNode getConllNode(){
        return headNode;
    }
    
    public Word getHeadWrd(){
        return headNode.getWrd();
    }
    
    public String getWrdStr(){
        return headNode.getWrd().toString();
    }
    
    public int getNodeId(){
        return headNode.getNodeId();
    }
    
    public POS getPOS(){
        return headNode.getPOS();
    }
    
    public CCGCategory getCCGcat(){
        return ccgCat;
    }
    
    public CCGCategory getHeadcat(){
        return headNode.getccgCat();
    }
    
    public String toString2(){
        StringBuilder sb = new StringBuilder();
        sb.append(ccgCat.toString());sb.append("::");
        sb.append(headNode.getNodeId());sb.append("::");
        sb.append(headNode.getWrd());
        return sb.toString();
    }
    
    @Override
    public String toString(){        
        StringBuilder sb = new StringBuilder();        
        sb.append(headNode.getNodeId());sb.append("::");
        sb.append(headNode.getWrd());sb.append("::");
        sb.append(ccgCat.toString());
        return sb.toString();
    }
    
    public boolean isLeaf(){
        return (this instanceof CCGTreeNodeLeaf);
    }
    
    public boolean isUnary(){
        return (this instanceof CCGTreeNodeUnary);
    }
    
    public boolean isBinary(){
        return (this instanceof CCGTreeNodeBinary);
    }
        
    public int getLeftChildSpan(){
        int span = 0;
        if(this instanceof CCGTreeNodeBinary){
            CCGTreeNodeBinary node = (CCGTreeNodeBinary) this;
            span = node.getRSpan();
        }
        else if(this instanceof CCGTreeNodeUnary){
            CCGTreeNodeUnary node = (CCGTreeNodeUnary) this;
            span = node.getRSpan();
        }
        return span;
    }
    
    static class CCGTreeNodeBinary extends CCGRTreeNode {
        
        final boolean headIsLeft;
        final CCGRTreeNode leftChild;
        final CCGRTreeNode rightChild;
        
        private CCGTreeNodeBinary(CCGCategory ccgCat, Combinator comb, int headId, SCoNLLNode headNode, 
                boolean headIsLeft, CCGRTreeNode leftChild, CCGRTreeNode rightChild) {
            super(ccgCat, comb, headId, headNode);
            this.headIsLeft = headIsLeft;
            this.leftChild = leftChild;
            this.rightChild = rightChild;
        }        
        
        @Override
        public int getChildCount() {
            return 2;
        }
        
        @Override
        public CCGRTreeNode getLeftChild(){
            return this.leftChild;
        }
        
        @Override
        public CCGRTreeNode getRightChild(){
            return this.rightChild;
        }
        
        @Override
        public CCGRTreeNode copy(){
            return new CCGTreeNodeBinary(this.getCCGcat().copy(), this.getCombinator(), this.getHeadDir(), this.getConllNode(), this.headIsLeft, this.getLeftChild(), this.getRightChild());
        }
    }
    
    static class CCGTreeNodeLeaf extends CCGRTreeNode {
        private CCGTreeNodeLeaf(CCGCategory ccgCat, Combinator comb, int headId, SCoNLLNode headNode) {
            super(ccgCat, comb, headId, headNode);
        }
                
        @Override
        public int getChildCount() {
            return 0;
        }       
        
        @Override
        public CCGRTreeNode getLeftChild(){
            return null;
        }
        
        @Override
        public CCGRTreeNode getRightChild(){
            return null;
        }    
        
        @Override
        public CCGRTreeNode copy(){
            return new CCGTreeNodeLeaf(this.getCCGcat().copy(), this.getCombinator(), this.getHeadDir(), this.getConllNode());
        }
    }
    
    static class CCGTreeNodeUnary extends CCGRTreeNode {
        
        final CCGRTreeNode child;
        
        private CCGTreeNodeUnary(CCGCategory ccgCat, Combinator comb, int headId, SCoNLLNode headNode, CCGRTreeNode child) {
            super(ccgCat, comb, headId, headNode);            
            this.child = child;
        }
                
        @Override
        public int getChildCount() {
            return 1;
        }
        
        @Override
        public CCGRTreeNode getLeftChild() {
            return this.child;
        }        
        
        @Override
        public CCGRTreeNode getRightChild() {
            return null;
        }

        @Override
        public CCGRTreeNode copy() {
            return new CCGTreeNodeUnary(this.getCCGcat().copy(), this.getCombinator(), this.getHeadDir(), this.getConllNode(), this.getLeftChild());
        }
    }
    
    public static CCGRTreeNode makeBinary(CCGCategory ccgCat, Combinator comb, boolean headIsLeft, CCGRTreeNode left, CCGRTreeNode right){
        
        int headid;
        SCoNLLNode headcNode;
        if(headIsLeft)
            headcNode = left.getConllNode();
        else
            headcNode = right.getConllNode();
        
        headid = (headIsLeft) ? 0 : 1;
        CCGRTreeNode result = new CCGTreeNodeBinary(ccgCat, comb, headid, headcNode, 
                headIsLeft, left, right);
        
        return result;
    }
    
    public static CCGRTreeNode makeUnary(CCGCategory ccgCat, Combinator comb, CCGRTreeNode child) {
        int headid;
        SCoNLLNode headcNode = child.getConllNode();
        headid = 0;
        return new CCGTreeNodeUnary(ccgCat, comb, headid, headcNode, child);
    }
    
    public static CCGRTreeNode makeLeaf(CCGCategory ccgCat, Combinator comb, SCoNLLNode headcNode) {
        int headid;
        headid = -1;
        return new CCGTreeNodeLeaf(ccgCat, comb, headid, headcNode);
    }
    
    public abstract int getChildCount();
    public abstract CCGRTreeNode getLeftChild();
    public abstract CCGRTreeNode getRightChild();
}