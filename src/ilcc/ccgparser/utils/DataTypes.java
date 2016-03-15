/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ilcc.ccgparser.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author ambati
 */
public class DataTypes {
    
    public static class Word {
        
        private final String word;        
        private final static Map<String, Word> cache = Collections.synchronizedMap(new HashMap<String, Word>());
        
        public static Word make(String string) {
            Word result = cache.get(string);
            if (result == null) {
                result = new Word(string);
                cache.put(string, result);
            }
            return result;
        }
        
        private Word(String str){
            word = str;
        }
        
        public String getWord(){
            return word;
        }
        
        @Override
        public String toString(){
            return word;
        }
    }
    
    public static class POS {
        
        private final String pos;        
        private final static Map<String, POS> cache = Collections.synchronizedMap(new HashMap<String, POS>());
        
        public static POS make(String string) {
            POS result = cache.get(string);
            if (result == null) {
                result = new POS(string);
                cache.put(string, result);
            }
            return result;
        }
        
        private POS(String str){
            pos = str;
        }
        
        public String getPos(){
            return pos;
        }
        
        @Override
        public String toString(){
            return pos;
        }
    }
    
    public static class CCGCategory {
        
        private final String ccgCat;        
        private final static Map<String, CCGCategory> cache = Collections.synchronizedMap(new HashMap<String, CCGCategory>());
        
        public static CCGCategory make(String string) {
            CCGCategory result = cache.get(string);
            if (result == null) {
                result = new CCGCategory(string);
                cache.put(string, result);
            }
            return result;
        }
        
        private CCGCategory(String str){
            ccgCat = str;
        }
        
        public String getCatStr(){
            return ccgCat;
        }
        
        public CCGCategory copy(){
            return CCGCategory.make(ccgCat);
        }
        
        @Override
        public String toString(){
            return ccgCat;
        }
    }
    
    public static class GoldccgInfo {
        private final List<ArcJAction> arcjActs;
        private final HashMap<String, CCGDepInfo> ccgDeps;
        private final CCGJSentence ccgsent;
        
        public GoldccgInfo(List<ArcJAction> acts, HashMap<String, CCGDepInfo> ccgdeps, CCGJSentence sent){
            arcjActs = acts;
            ccgDeps = ccgdeps;
            ccgsent = sent;
        }
        
        public CCGJSentence getccgSent(){
            return ccgsent;
        }
        
        public HashMap<String, CCGDepInfo> getccgDeps(){
            return ccgDeps;
        }
        
        public List<ArcJAction> getarcActs(){
            return arcjActs;
        }
    }
    
}