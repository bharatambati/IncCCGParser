/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ilcc.ccgparser.utils;

import java.io.Serializable;

/**
 *
 * @author ambati
 * 
 */
public class CoNLLNode implements Serializable{
    
    private final int nodeId;
    private final String word;
    private final String lemma;
    private final String cpos;
    private final String pos;
    private final String[] feats;
    private final String depLabel;
    private final int dephead;    
    private final String supertag;
    
    public CoNLLNode(int id, String wrd, String lem, String cpostag, String postag, String featStr, int head, String label, String sTag){
        nodeId = id;
        word = wrd;
        lemma = lem;
        cpos = cpostag;
        pos = postag;
        feats = featStr.split("|");
        dephead = head;
        depLabel = label;
        supertag = sTag;
    }
    
    public String getSuperTag(){
        return supertag;
    }
    
    public String getWrd(){
        return word;
    }
    
    public String getLemma(){
        return lemma;
    }
    
    public String getPOS(){
        return pos;
    }
    
    public int getNodeId(){
        return nodeId;
    }
    
    public String toString(){
        return nodeId+"::"+word;
    }
}
