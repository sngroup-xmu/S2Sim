package org.batfish.log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;

public class deserializeBgpLogs {
    public static void main(String[] args){
//        long startTime = System.currentTimeMillis();
//        for (int i =1; i<4000; i++){
//            System.out.println("fvdfbfbf"+i%45);
//        }
//        long endTime = System.currentTimeMillis();
//        System.out.println("runTime of deserialize: "+ (endTime-startTime));
        for (int k=4; k<56; k+=2){
            long startTime = System.currentTimeMillis();
            ArrayList<BgpLogs> allLogs = new ArrayList<>();
            String rootPath = "/home/dell/yrl/org.batfish.log-data/org.batfish.log-serialize/fattree"+k+"/";
            File file = new File(rootPath);
            File[] fileList = file.listFiles();
            for (int i=0; i<fileList.length; i++){
                ObjectInputStream in = null;
                try {
                    in = new ObjectInputStream(new FileInputStream(fileList[i].getAbsoluteFile()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    allLogs.add( (BgpLogs) in.readObject());
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
//                System.out.println("finish: "+ i);
            }
            long endTime = System.currentTimeMillis();
            System.out.println("runTime of deserialize: "+ (endTime-startTime));
        }


    }

}
