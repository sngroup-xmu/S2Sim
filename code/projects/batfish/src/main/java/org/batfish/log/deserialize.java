package org.batfish.log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class deserialize {
    public static void main(String[] args){
        String networkName = "15nodes";
        if (args.length>1){
            networkName = args[1];
        }
        String path1 = System.getProperty("user.dir")+"/log-serialize/"+networkName+"/";
        Map<String, BgpLogs> map = new HashMap<String, BgpLogs>();
        File file = new File(path1);
        File[] fileList = file.listFiles();
        for (int i=0; i< Objects.requireNonNull(fileList).length; i++){
            ObjectInputStream in = null;
            try {
                in = new ObjectInputStream(new FileInputStream(fileList[i].getAbsoluteFile()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                assert in != null;
                BgpLogs logs = (BgpLogs) in.readObject();
                map.put(logs.get_hostName(),logs);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            System.out.println("finish: "+ i);
        }
        System.out.println("done");
//        long startTime = System.currentTimeMillis();
//        for (int i =1; i<4000; i++){
//            System.out.println("fvdfbfbf"+i%45);
//        }
//        long endTime = System.currentTimeMillis();
//        System.out.println("runTime of deserialize: "+ (endTime-startTime));
//        for (int k=4; k<6; k+=2){
//            long startTime = System.currentTimeMillis();
//            ArrayList<BgpLog> allLogs = new ArrayList<>();
//            String rootPath = "/home/dell/yrl/org.batfish.log-data/org.batfish.log-serialize-append-aggregation/fattree"+k+"/";
//            File file = new File(rootPath);
//            File[] fileList = file.listFiles();
//            for (int i=0; i<fileList.length; i++){
//                ObjectInputStream in = null;
//                try {
//                    in = new ObjectInputStream(new FileInputStream(fileList[i].getAbsoluteFile()));
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                try {
//                    allLogs.add( (BgpLog) in.readObject());
//                } catch (IOException e) {
//                    e.printStackTrace();
//                } catch (ClassNotFoundException e) {
//                    e.printStackTrace();
//                }
////                System.out.println("finish: "+ i);
//            }
//            long endTime = System.currentTimeMillis();
//            System.out.println("runTime of deserialize: "+ (endTime-startTime));
//        }
    }

}
