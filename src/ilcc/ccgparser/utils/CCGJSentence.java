/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ilcc.ccgparser.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author ambati
 */

public class CCGJSentence implements Serializable {
    
    ArrayList<CCGJTreeNode> nodes;
    public HashMap<String, CCGDepInfo> pargDeps;
    public HashMap conllDeps;
    public String ccgDeriv;
    public CCGJTreeNode ccgDerivTree;
    public String sentence;
    
    public CCGJSentence(){
        nodes = new ArrayList<>();
        pargDeps = new HashMap();
        conllDeps = new HashMap();
        ccgDeriv = "";
        sentence = "";
    }
    
    public int getLength(){
        //return nodes.size();
        return toString().split(" ").length;
    }
    
    public void setCcgDeriv(String deriv){
        ccgDeriv = deriv;
    }
    
    public void setSentence(String sent){
        sentence = sent;
    }
    
    public void setccgDerivTree(CCGJTreeNode root){
        ccgDerivTree = root;
    }
    
    public void addCCGJTreeNode(CCGJTreeNode node){
        nodes.add(node);
        sentence+=node.getHeadWrd()+" ";
    }
    
    public CCGJTreeNode getNode(int id){
        return nodes.get(id);
    }
    
    public CCGJTreeNode get(int id){
        return nodes.get(id);
    }
    
    public void setpargdeps(HashMap<String, CCGDepInfo> deps){
        pargDeps = deps;
    }
    
    public HashMap<String, CCGDepInfo> getPargDeps(){
        return pargDeps;
    }
    
    public void fillCoNLL(ArrayList<String> lines){
        String[] cats;
        StringBuilder sb = new StringBuilder();
        for(String line:lines){
            String[] parts = line.split("\t");
            if(parts.length >= 8){
                cats = parts[8].split("\\|\\|");
                SCoNLLNode cnode = new SCoNLLNode(Integer.parseInt(parts[0]), parts[1], parts[3], cats[0]);
                CCGJTreeNode node = CCGJTreeNode.makeLeaf(null, cnode);
                node.setCCGcats(cats);
                nodes.add(node);
                sb.append(parts[1]);sb.append(" ");
            }
        }
        sentence += sb.toString();
    }
    
    public void updateCoNLL(ArrayList<String> lines){
        String[] cats;
        int id = 0;
        for(String line:lines){
            String[] parts = line.split("\t");
            if(parts.length >= 8){
                //CoNLLNode node = new CoNLLNode(Integer.parseInt(parts[0]), parts[1], parts[2], parts[3], parts[4], parts[5], Integer.parseInt(parts[6]), parts[7], parts[8]);
                cats = parts[8].split("\\|\\|");
                //CCGJNode clnode = new CCGJNode(node, "");
                //CCGJTreeNode clnode = new CCGJTreeNode(node, cats);
                //nodes.add(clnode);
                //sentence+=parts[1]+" ";
                CCGJTreeNode cnode = getNode(id);
                cnode.setCCGcats(cats);
            }
            id++;
        }
    }    
    
    public void fillDeriv(String deriv){
        ccgDeriv = deriv;
    }
    
    public ArrayList<CCGJTreeNode> getNodes(){
        ArrayList<CCGJTreeNode> list = new ArrayList<>();
        for(CCGJTreeNode node : nodes)
            list.add(node);
        return list;
    }
    
    public CCGJTreeNode getDerivRoot(){
        return ccgDerivTree;
    }
    
    @Override
    public String toString(){
        return sentence;
    } 
}