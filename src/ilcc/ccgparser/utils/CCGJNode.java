/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ilcc.ccgparser.utils;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import edinburgh.ccg.deps.CCGcat;
import ilcc.ccgparser.utils.DataTypes.CCGCategory;
import java.io.Serializable;
import java.util.ArrayList;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * @author ambati
 */
public class CCGJNode extends DefaultMutableTreeNode implements Serializable {
    
    private CCGcat ccgCat;
    private int headId;
    private String headWrd;
    private int lspan;
    private int rspan;
    private SCoNLLNode headNode;
    private ArrayList<CCGCategory> ccgCats;
    private CCGJNode left;
    private CCGJNode right;

    public CCGJNode() {
        
    }
    
    public CCGJNode(int wId, String lexString) {
        headId = -1;
        
        // (<L (S/S)/NP IN IN In (S_113/S_113)/NP_114>)        
        String wrd, lemma, pos, cpos, feats="";
        ArrayList<String> items = Lists.newArrayList(Splitter.on(CharMatcher.anyOf("<> ")).trimResults().omitEmptyStrings().split(lexString));
        String cat = items.get(1);
        pos = items.get(2);
        cpos = items.get(3);
	wrd = items.get(4);
	lemma = wrd;
        headWrd = wrd;
        ccgCat = CCGcat.lexCat(wrd, cat);
        //System.out.println(ccgCat.getIndCatDeps());
        headNode = new SCoNLLNode(wId, wrd, pos, cat);
        ccgCats = new ArrayList<>();
    }    
    
    public CCGJNode(SCoNLLNode node, ArrayList<String> cats) {
        ccgCats = new ArrayList<>();
        headId = -1;
        headNode = node;
        String wrd = node.getWrd().toString();
        for(String catStr : cats)
            ccgCats.add(CCGCategory.make(catStr));
    }
    
    public CCGJNode(SCoNLLNode node, String cat) {
        ccgCats = new ArrayList<>();
        headId = -1;
        headNode = node;
        String wrd = node.getWrd().toString();
        ccgCat = CCGcat.lexCat(wrd, cat);
    }
    
    public CCGJNode(SCoNLLNode node, CCGcat cat) {
        ccgCats = new ArrayList<>();
        headId = -1;
        headNode = node;
        ccgCat = cat;
    }
    
    public void setLeftNode(CCGJNode node){
        left = node;
    }
    
    public void setRightNode(CCGJNode node){
        right = node;
    }
    
    public CCGJNode getLeftNode(){
        return left;
    }
    
    public CCGJNode getRightNode(){
        return right;
    }
    
    public void setHeadId(int id){
        headId = id;
    }
    
    public void setConllNode(SCoNLLNode cNode){
        headNode = cNode;
    }
    
    public void setCCGcat(CCGcat cat){
        ccgCat = cat;
    }
    
    public void setCCGcat(String catStr){
        ccgCat = CCGcat.lexCat(headNode.getWrd().toString(), catStr);
    }
    
    public CCGcat getCCGcat(){
        return ccgCat;
    }
    
    public void addChild(CCGJNode child, int id){
        this.insert(child, id);
    }    
        
    public CCGJNode getChild(int id){        
        CCGJNode child = null;        
        if(children.size() > id)
            child = (CCGJNode) children.get(id);
        return child;
    }
    
    public SCoNLLNode getConllNode(){
        return headNode;
    }
    
    public int getHeadId(){
        return headId;
    }
    
    public int getChildCount(){
        if(children == null)
            return 0;
        else
            return children.size();
    }
        
    public CCGJNode getHeadChild(){
        return (CCGJNode) this.getChildAt(headId);
    }
    
    public String getHeadWrd(){
        return headWrd;
    }
    
    public ArrayList<CCGCategory> getCatList(){
        return ccgCats;
    }
    
    public void setLSpan(int span){
        lspan = span;
    }
    
    public void setRSpan(int span){
        rspan = span;
    }
    
    public void setSpans(int left, int right){
        lspan = left;
        rspan = right;
    }
    
    public int getLSpan(){
        return lspan;
    }
    
    public int getRSpan(){
        return rspan;
    }
    
    public String toString2(){
        return headNode.getNodeId()+"::"+headNode.getWrd()+"::"+ccgCat.catString;
    }
    
    public String toString(){
        return headNode.getNodeId()+"::"+headNode.getWrd();
    }
    
    public boolean isPunct(){
        if(headNode.getWrd().equals(headNode.getPOS()))
            return true;
        else
            return false;
    }
}