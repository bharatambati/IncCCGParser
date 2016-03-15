/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ilcc.ccgparser.utils;

import edinburgh.ccg.deps.CCGcat;
import ilcc.ccgparser.utils.DataTypes.CCGCategory;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author ambati
 */
public class DepGraph {
    
    private final HashMap<Integer, CCGCategory> vertices;
    private final HashMap<Integer, ArrayList<Integer>> edgemap;
    
    public DepGraph(int v){
        vertices = new HashMap<>(v);
        edgemap = new HashMap<>(v);
    }
    
    public DepGraph(HashMap<Integer, CCGCategory> nvertices, HashMap<Integer, ArrayList<Integer>> nedgemap){
        vertices = nvertices;
        edgemap = nedgemap;
    }
    
    public void addVertex(int id, CCGCategory cat){
        vertices.put(id, cat);
    }
    
    public CCGCategory getVertex(int id){
        return vertices.get(id);
    }
    
    public void addEdge(int pid, int cid){
        ArrayList<Integer> list = new ArrayList<>();
        if(edgemap.containsKey(pid)){
            list = edgemap.get(pid);
            int index;
            for(index =0; index<list.size(); index++){
                if(list.get(index) == cid)
                    return;
                if(list.get(index) > cid)
                    break;
            }
            list.add(index, cid);
        }
        else{
            list.add(cid);
        }
        edgemap.put(pid, list);
    }
    
    public Integer getRightMost(Integer vert){
        Integer rmost = null;
        ArrayList<Integer> edgelist;
        if((edgelist = edgemap.get(vert)) != null){
            Integer rvert = edgelist.get(edgelist.size()-1);
            if(rvert > vert)
                rmost = rvert;
        }
        return rmost;
    }
    
    private Integer getRightMost2(Integer vert){
        Integer rmost = null;
        ArrayList<Integer> edgelist;
        if((edgelist = edgemap.get(vert)) != null && edgelist.size()>=2){
            Integer rvert = edgelist.get(edgelist.size()-1);
            Integer rvert2 = edgelist.get(edgelist.size()-2);
            if(rvert2 > vert){
                //rmost = rvert2;
                if( (edgelist = edgemap.get(rvert2)) != null && edgelist.get(edgelist.size()-1) == rvert)
                    rmost = rvert2;
            }
        }
        return rmost;
    }
    
    public Integer getLeftMost(Integer vert){
        Integer lmost = null;
        ArrayList<Integer> edgelist;
        if((edgelist = edgemap.get(vert)) != null){
            Integer lvert = edgelist.get(0);
            if(lvert < vert)
                lmost = lvert;
        }
        return lmost;
    }
    
    public Integer getLeftMost(Integer vert, String catstr){
        Integer lmost = null;
        ArrayList<Integer> edgelist;
        if((edgelist = edgemap.get(vert)) != null){
            Integer lvert;
            for(int i=0; i< edgelist.size(); i++){
                lvert = edgelist.get(i);
                if(lvert < vert && CCGcat.noFeatures(vertices.get(lvert).toString()).matches(catstr))
                //if(lvert.getIndex() < vert.getIndex() && lvert.getHeadCat().matches(catstr))
                    lmost = lvert;
            }
        }
        return lmost;
    }
    
    public ArrayList<Integer> getRightPer(Integer vert){
        ArrayList<Integer> list = new ArrayList<>();
        list.add(vert);
        Integer rmost = getRightMost(vert);
        while(rmost != null){
            list.add(rmost);
            Integer tmp = getRightMost2(rmost);
            if(tmp!=null) list.add(tmp);
            rmost = getRightMost(rmost);
        }
        return list;
    }
    
    public DepGraph copy(){
        
        HashMap<Integer, CCGCategory> nvertices = copyVertexMap();
        HashMap<Integer, ArrayList<Integer>> nedgemap = copyEdgeMap();
        DepGraph nDepGraph = new DepGraph(nvertices, nedgemap);
        //nDepGraph.vertices = copyHashMap(vertices);
        return nDepGraph;
    }
    
    private HashMap<Integer, CCGCategory> copyVertexMap(){
        HashMap<Integer, CCGCategory> nvertices = new HashMap<>(this.vertices);
        for(Integer key : this.vertices.keySet())
            nvertices.put(key, this.vertices.get(key).copy());
        return nvertices;
    }
    
    public HashMap copyEdgeMap(){
        HashMap<Integer, ArrayList<Integer>> nedgemap = new HashMap<>(this.edgemap);
        for(Integer key : this.edgemap.keySet()){
            ArrayList<Integer> list = edgemap.get(key);
            ArrayList<Integer> nlist = new ArrayList<>(list.size());
            for(int i : list)
                nlist.add(i);
            nedgemap.put(key, nlist);
        }
        
        return nedgemap;
    }
    
    /*
    public void addEdge(int pid, CCGCategory phcat, int cid, CCGCategory chcat){
        Vertex pvert = new Vertex(pid, phcat);
        Vertex cvert = new Vertex(cid, chcat);
        ArrayList<Edge> list = new ArrayList<>();
        
        if(edgemap.containsKey(pvert)){
            list = edgemap.get(pvert);
            int index;
            for(index =0; index<list.size(); index++){
                if(list.get(index).getVertex().getIndex() == cvert.getIndex())
                    return;
                if(list.get(index).getVertex().getIndex() > cvert.getIndex())
                    break;
            }
            list.add(index, new Edge(cvert));
        }
        else{
            list.add(new Edge(cvert));
        }
        edgemap.put(pvert, list);
    }
      */      
}
