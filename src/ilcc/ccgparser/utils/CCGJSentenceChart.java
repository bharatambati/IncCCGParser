/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ilcc.ccgparser.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 *
 * @author ambati
 */
public class CCGJSentenceChart implements Serializable {
    
    ArrayList<CCGJNode> nodes;
    public HashMap pragDeps;
    public HashMap conllDeps;
    public String ccgDeriv;
    public CCGJNode ccgDerivTree;
    public String sentence;
    
    public CCGJSentenceChart(){
        nodes = new ArrayList<>();
        pragDeps = new HashMap();
        conllDeps = new HashMap();
        ccgDeriv = "";
        sentence = "";
    }
    
    public int getLength(){
        return nodes.size();
    }
    
    public void setCcgDeriv(String deriv){
        ccgDeriv = deriv;
    }
    
    public void setSentence(String sent){
        sentence = sent;
    }
    
    public void setccgDerivTree(CCGJNode root){
        ccgDerivTree = root;
    }
    
    public void addCCGJNode(CCGJNode node){
        nodes.add(node);
        sentence+=node.getHeadWrd()+" ";
    }
    
    public void fillCoNLL(ArrayList<String> lines){
        ArrayList<String> cats;
        for(String line:lines){
            String[] parts = line.split("\t");
            if(parts.length >= 8){
                CoNLLNode node = new CoNLLNode(Integer.parseInt(parts[0]), parts[1], parts[2], parts[3], parts[4], parts[5], Integer.parseInt(parts[6]), parts[7], parts[8]);
                cats = new ArrayList<>();
                cats.addAll(Arrays.asList(parts[8].split("\\|\\|")));                
                CCGJNode clnode = null;
                //CCGJNode clnode = new CCGJNode(node, "");
                //CCGJNode clnode = new CCGJNode(node, cats);
                nodes.add(clnode);
                sentence+=parts[1]+" ";
            }
        }
    }
    
    public void fillCoNLLwp(ArrayList<String> lines){
        ArrayList<String> cats;
        for(String line:lines){
            String[] parts = line.split("\t");
            if(parts.length >= 8){
                CoNLLNode node = new CoNLLNode(Integer.parseInt(parts[0]), parts[1], parts[2], parts[3], parts[4], parts[5], Integer.parseInt(parts[6]), parts[7], parts[8]);                
                CCGJNode clnode = null;
                //cats = new ArrayList<>();
                //cats.addAll(Arrays.asList(parts[8].split("\\|\\|")));
                //CCGJNode clnode = new CCGJNode(node, "");
                nodes.add(clnode);
                sentence+=parts[1]+" ";
            }
        }
    }
    
    public void fillDeriv(String deriv){
        ccgDeriv = deriv;
    }
    
    public ArrayList<CCGJNode> getNodes(){
        ArrayList<CCGJNode> list = new ArrayList<>();
        for(CCGJNode node : nodes)
            list.add(node);
        return list;
    }
    
    public CCGJNode getNodeAt(int i){        
        return nodes.get(i);
    }
    
    public CCGJNode getDerivRoot(){
        return ccgDerivTree;
    }
    
    public String toString(){
        return sentence;
    } 
}