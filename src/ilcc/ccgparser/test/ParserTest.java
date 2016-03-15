/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ilcc.ccgparser.test;

import ilcc.ccgparser.incderivation.*;
import ilcc.ccgparser.utils.DataTypes.GoldccgInfo;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

/**
 *
 * @author ambati
 */
public class ParserTest {
    
    public static void main(String[] args) throws IOException, Exception {
        
        String trainAutoFile, trainConllFile, testPargFile, testConllFile;
        String home = "/home/ambati/ilcc/projects/parsing/experiments/english/ccg/";
        
        trainAutoFile = home+"data/final/devel.gccg.auto";
        trainConllFile = home+"data/final/devel.accg.conll";
        testPargFile = home+"data/final/devel.gccg.jparg";
        testConllFile = home+"data/final/devel.accg.conll";
        
        SRParser srparser;
        //parser = new ArcEager();
        //parser = new ArcEagerExtended();
        //parser = new ArcStandard();
        srparser = new RevInc();
        //IncDerivation parser = new IncDerivation();
        
        long start, end;
        double time;
        
        System.err.println("Started: " + new Date(System.currentTimeMillis()) +"\n");
        start = (long) (System.currentTimeMillis());
        srparser.fillData(trainConllFile, trainAutoFile, trainAutoFile, new HashMap<>());
        end = (long) (System.currentTimeMillis());
        time = 0.001*(end-start)/60;
        System.err.println("Finished: " + new Date(System.currentTimeMillis())+"\nProcess took: " + time +" minutes \n");
        //parser.parse();
    }
}