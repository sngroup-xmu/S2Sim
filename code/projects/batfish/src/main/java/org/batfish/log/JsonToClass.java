package org.batfish.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JsonToClass {
  public JsonToClass(){}
  public static BgpLogs fileToClass(String filePath){
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.readValue(new File(filePath),BgpLogs.class);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }
  public static Map<String, BgpLogs> fattreeToLogs(int k){
    Map<String, BgpLogs> map = new HashMap<String, BgpLogs>();
    String path = "/home/dell/yrl/org.batfish.log-data/org.batfish.log/fattree"+k+"/";
    File file = new File(path);
    File[] fileArray = file.listFiles();
    for (int i=0; i<fileArray.length; i++){
      System.out.println(fileArray[i].getAbsolutePath());
      map.put(fileArray[i].getName(), fileToClass(fileArray[i].getAbsolutePath()));
    }
    return map;
  }
}
