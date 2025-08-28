package org.batfish.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.log.RipLog.Action;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class RipLogs<R extends AbstractRoute> {

  private enum InitOrInstall {
    INIT,
    INSTALL
  }

  private final ArrayList<R> _initRoutes;
  private final ArrayList<RipLog<R>> _ripLog;
  private final ArrayList<R> _installRoutes;
  private final String _hostName;
  private final ArrayList<Long> _completedTime;

  public RipLogs(String _hostName) {
    this._hostName = _hostName;
    _initRoutes = new ArrayList<>();
    _ripLog = new ArrayList<>();
    _installRoutes = new ArrayList<>();
    _completedTime = new ArrayList<>();
  }

  public static InitOrInstall getInitAction() {
    return InitOrInstall.INIT;
  }

  public static InitOrInstall getInstallAction() {
    return InitOrInstall.INSTALL;
  }


  public void add(R r, InitOrInstall a) {
    switch (a) {
    case INIT:
        {
          long start = System.nanoTime();
          _initRoutes.add(r);
          long end = System.nanoTime();
          _completedTime.add((end - start));
      }
    break;
    case INSTALL:
        {
          long start = System.nanoTime();
          _installRoutes.add(r);
          long end = System.nanoTime();
          _completedTime.add((end - start));
      }
    break;
    default:break;
    }
  }

  public void add(RipLog<R> _rl) {
    long start = System.nanoTime();
    _ripLog.add(_rl);
    long end = System.nanoTime();
    _completedTime.add((end - start));
  }

  public void add(String hostName, R r, Action a) {
    //start
    long start = System.nanoTime();
    _ripLog.get(_ripLog.size()-1).add(hostName, r, a);
    long end = System.nanoTime();
    //end
    _completedTime.add((end - start));
  }

  public void writeLatency(String path, String fn) {
    String fileName = fn + "latency.md";
    Writer output = null;
    File file = new File(path + fileName);
    if(!file.exists() | file.isDirectory()) {
      try{
        file.createNewFile();
      } catch (IOException e) {
        System.err.println("File failed to create.");
      }
    }
    try{
      output = new BufferedWriter(new FileWriter(file, true));
      for(Long a : _completedTime) {
        output.append(a.toString());
        output.append("\n");
      }
      output.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void write2file(String path) {
    String fileName = _hostName + "plog.json";
    JSONArray plogArray = new JSONArray();
//    JSONArray latencyArray = new JSONArray();
    Integer num = 1;
    for (RipLog<R> i : _ripLog) {
      JSONObject plog = makeJsonObject(num, i);
      //      JSONObject plog = new JSONObject(String.valueOf(num), i._route);
      plogArray.put(plog);
//      latencyArray.put(i.getLatency());
      num++;
    }

    try{
      Writer output = null;
      File file = new File(path + fileName);
      if(!file.exists() | file.isDirectory()) {
        try{
          file.createNewFile();
        } catch (IOException e) {
          System.err.println("File failed to create.");
        }
      }
      output = new BufferedWriter(new FileWriter(file));
      for (int i=0; i<plogArray.length(); i++) {
        output.write(plogArray.getJSONObject(i).toString());
        output.write("\n");
      }
//      for(int i=0; i<latencyArray.length(); i++) {
//        output.write(latencyArray.getJSONObject(i).toString());
//        output.write("\n");
//      }
      output.close();
      System.out.println("Success: log has been written to file: " + path + fileName);
    } catch (Exception e) {
      System.err.println("Error: log failed to write to file: " + path + fileName);
    }
  }

  private JSONObject makeJsonObject(Integer num, RipLog<R> l) {
    JSONObject object = new JSONObject();



//    JSONArray send = new JSONArray();

    JSONObject fcause = new JSONObject();
    JSONObject fselect = new JSONObject();
//    JSONObject fsend = new JSONObject();

    //    Integer i = 1;
    try{
      for(String key:l._cause.keySet()) {
        JSONArray cause = new JSONArray();
        l._cause.get(key).forEach(v -> {
          cause.put(v.toString());
        });
        try{
          fcause.put(key, cause);
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }

      for(String key:l._select.keySet()) {
        JSONArray select = new JSONArray();
        l._select.get(key).forEach(v -> {
          select.put(v.toString());
        });
        try{
          fselect.put(key, select);
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }

//      l._cause.forEach((key, value) -> {
//        value.forEach(v -> {
//          cause.put(v.toString());
//          cause.put("\n");
//        });
//        try {
//          fcause.put(key, cause);
//        } catch (JSONException e) {
//          e.printStackTrace();
//        }
//      });

//      l._select.forEach((key, value) -> {
//        value.forEach(v -> {
//          select.put(v.toString());
//          select.put("\n");
//        });
//        try {
//          fselect.put(key, select);
//        } catch (JSONException e) {
//          e.printStackTrace();
//        }
//      });


      object.put("Cause ", fcause);
      object.put("", "\n");
      object.put("Select ", fselect);
      object.put("", "\n");

    } catch (JSONException e) {
      e.printStackTrace();
    }
    return object;
  }

}
