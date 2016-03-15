/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ilcc.ccgparser.utils;

import ilcc.ccgparser.utils.DataTypes.CCGCategory;
import java.util.ArrayList;
import java.util.List;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * @author ambati
 */
public class DepTree extends DefaultMutableTreeNode{
    
    private final int headId;
    private final CCGCategory headCat;
    
    public DepTree(int id, CCGCategory cat){        
        headId = id;
        headCat = cat;
    }
    
    public CCGCategory getccgCat(){
        return headCat;
    }
    
    public int getId(){
        return headId;
    }
    
    public DepTree copy(){
        return new DepTree(this.headId, this.headCat);
    }
    
    public void addChild(DepTree child){
        int id = 0;
        
        for(int i = 0; i< this.getChildCount(); i++){
            DepTree cur = (DepTree) this.getChildAt(i);
            if(child.getId() > cur.getId())
                id = i+1;
            else
                break;
        }
        
        this.insert(child, id);
    }
    
    public DepTree getLeftMost(int id, String catStr){
        DepTree lmost = null;
        for(int i =0; i< this.getChildCount();i++){
            DepTree left = (DepTree) this.getChildAt(i);
            String catString = left.getccgCat().toString();
            int oIndex = catString.indexOf('[');
            if(oIndex != -1)
                catString = catString.substring(0, oIndex);
            if(catString.matches(catStr)){
                lmost = left;
                break;
            }
        }
        return lmost;
    }
    
    public DepTree getRightMost(DepTree vert){
        DepTree rmost = null;
        int count = vert.getChildCount();
        if(count>0){
            DepTree rvert = (DepTree) vert.getChildAt(count-1);
            if(rvert.getId() > vert.getId())
                rmost = rvert;
        }
        
        return rmost;
    }
    
    public DepTree getRightMost2(DepTree vert){
        DepTree rmost = null;
        int count = vert.getChildCount();
        if(count>=2){
            DepTree rvert = (DepTree) vert.getChildAt(count-1);
            DepTree rvert2 = (DepTree) vert.getChildAt(count-2);
            if(rvert2.getId() > vert.getId()){
                //rmost = rvert2;
                int count2 = rvert2.getChildCount();
                if(count2>0 && ( ((DepTree)rvert2.getChildAt(count2-1)).getId() == rvert.getId()) )
                    rmost = rvert2;
            }
        }
        return rmost;
    }
    
    public List<DepTree> getRightPer(){
        List<DepTree> list = new ArrayList<>();        
        list.add(this);
        DepTree rmost = getRightMost(this);
        while(rmost != null){
            list.add(rmost);
            DepTree tmp = getRightMost2(rmost);
            if(tmp!=null) list.add(tmp);
            rmost = getRightMost(rmost);
        }
        return list;
    }
    
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(headId);sb.append("--");sb.append(headCat);
        return sb.toString();
    }
}
