/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ilcc.ccgparser.utils;

import edinburgh.ccg.deps.CCGcat;
import ilcc.ccgparser.utils.DataTypes.*;
import ilcc.ccgparser.utils.ccgCombinators.RuleType;
import java.util.HashMap;

/**
 *
 * @author ambati
 */
public abstract class SCCGJTreeNode {
    
    private final CCGcat ccgCat;
    private final RuleType ruleType;
    private final int headDir;
    private final SCoNLLNode headNode;
    
    private SCCGJTreeNode(CCGcat ccgCat, RuleType rtype, int headId, SCoNLLNode headNode) {
        this.ccgCat = ccgCat;
        this.headDir = headId;
        this.headNode = headNode;
        this.ruleType = rtype;
    }
    
    public int getHeadDir(){
        return this.headDir;
    }
    
    public int getLSpan(){
        SCCGJTreeNode left = this;
        while(left.getLeftChild() != null)
            left = left.getLeftChild();
        
        return left.getConllNode().getNodeId();
    }
    
    public int getRSpan(){        
        SCCGJTreeNode right = this;
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
    
    public CCGcat getCCGcat(){
        return ccgCat;
    }
    
    public RuleType getRuleType(){
        return ruleType;
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
        return (this instanceof CCGJTreeNodeLeaf);
    }
    
    public boolean isUnary(){
        return (this instanceof CCGJTreeNodeUnary);
    }
    
    public boolean isBinary(){
        return (this instanceof CCGJTreeNodeBinary);
    }
        
    public int getLeftChildSpan(){
        int span = 0;
        if(this instanceof CCGJTreeNodeBinary){
            CCGJTreeNodeBinary node = (CCGJTreeNodeBinary) this;
            span = node.getRSpan();
        }
        else if(this instanceof CCGJTreeNodeUnary){
            CCGJTreeNodeUnary node = (CCGJTreeNodeUnary) this;
            span = node.getRSpan();
        }
        return span;
    }
    
    static class CCGJTreeNodeBinary extends SCCGJTreeNode {
        
        final boolean headIsLeft;
        final SCCGJTreeNode leftChild;
        final SCCGJTreeNode rightChild;
        
        private CCGJTreeNodeBinary(CCGcat ccgCat, int headId, SCoNLLNode headNode, 
                RuleType ruleType, boolean headIsLeft, SCCGJTreeNode leftChild, SCCGJTreeNode rightChild) {
            super(ccgCat, ruleType, headId, headNode);
            this.headIsLeft = headIsLeft;
            this.leftChild = leftChild;
            this.rightChild = rightChild;
        }        
        
        @Override
        public int getChildCount() {
            return 2;
        }
        
        @Override
        public SCCGJTreeNode getLeftChild(){
            return this.leftChild;
        }
        
        @Override
        public SCCGJTreeNode getRightChild(){
            return this.rightChild;
        }
    }
    
    static class CCGJTreeNodeLeaf extends SCCGJTreeNode {
        private CCGJTreeNodeLeaf(CCGcat ccgCat, int headId, SCoNLLNode headNode, HashMap depMap) {
            super(ccgCat, RuleType.lexicon, headId, headNode);
        }
                
        @Override
        public int getChildCount() {
            return 0;
        }       
        
        @Override
        public SCCGJTreeNode getLeftChild(){
            return null;
        }
        
        @Override
        public SCCGJTreeNode getRightChild(){
            return null;
        }
    }
    
    static class CCGJTreeNodeUnary extends SCCGJTreeNode {
        
        final SCCGJTreeNode child;
        
        private CCGJTreeNodeUnary(CCGcat ccgCat, RuleType rtype, int headId, SCoNLLNode headNode, SCCGJTreeNode child) {
            super(ccgCat, rtype, headId, headNode);            
            this.child = child;
        }
                
        @Override
        public int getChildCount() {
            return 1;
        }
        
        @Override
        public SCCGJTreeNode getLeftChild() {
            return this.child;
        }        
        
        @Override
        public SCCGJTreeNode getRightChild() {
            return null;
        }
    }
    
    public static SCCGJTreeNode makeBinary(CCGcat ccgCat, RuleType ruleType, boolean headIsLeft, SCCGJTreeNode left, SCCGJTreeNode right){
        
        int headid;
        SCoNLLNode headcNode;
        if(headIsLeft)
            headcNode = left.getConllNode();
        else
            headcNode = right.getConllNode();
        
        headid = (headIsLeft) ? 0 : 1;  
        SCCGJTreeNode result = new CCGJTreeNodeBinary(ccgCat, headid, headcNode, 
                ruleType, headIsLeft, left, right);
        
        return result;
    }
    
    public static SCCGJTreeNode makeUnary(CCGcat ccgCat, RuleType ruleType, SCCGJTreeNode child) {
        int headid;
        SCoNLLNode headcNode = child.getConllNode();
        headid = 0;
        return new CCGJTreeNodeUnary(ccgCat, ruleType, headid, headcNode, child);
    }
    
    public static SCCGJTreeNode makeLeaf(CCGcat ccgCat, SCoNLLNode headcNode) {
        int headid;
        headid = -1;
        return new CCGJTreeNodeLeaf(ccgCat, headid, headcNode, new HashMap<>());
    }
    
    public abstract int getChildCount();
    public abstract SCCGJTreeNode getLeftChild();
    public abstract SCCGJTreeNode getRightChild();
}