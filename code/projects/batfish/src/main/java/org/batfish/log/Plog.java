// package org.batfish.log;

// import java.io.BufferedWriter;
// import java.io.File;
// import java.io.FileWriter;
// import java.io.IOException;
// import java.io.Writer;
// import java.util.ArrayList;
// import org.codehaus.jettison.json.JSONArray;
// import org.codehaus.jettison.json.JSONException;
// import org.codehaus.jettison.json.JSONObject;

// //import org.codehaus.jettison.json.JSONArray;

// public class Plog {
//   private ArrayList<OspfLog> _log;

// //  private ArrayList<RipLog> _ripLog;

// //  private Map<OspfLog, OspfAction> _mapLog;
//   private String _hostName;

//   //  public Plog() {
//   //    _plog = new ArrayList<OspfLog>();
//   //  }

//   public Plog(String _hostName) {
//     _log = new ArrayList<OspfLog>();
// //    _ripLog = new ArrayList<RipLog>();
//     this._hostName = _hostName;
//   }

//   public Plog(String _hostName, OspfLog l){
//     _log = new ArrayList<OspfLog>();
//     _log.add(l);
//     this._hostName = _hostName;
//   }

// //  public void add(OspfLog l) {
// //    _log.add(l);
// //  }
// //  public void add(OspfCause c, OspfAction a) {
// //    _log.add(new OspfLog(c, a));
// //  }
// //
// //  public void remove(OspfLog l) {
// //    if(!_log.remove(l))
// //      System.err.println("OspfLog failed to remove:" + l.hashcode());
// //  }
// //
// //  public void remove(OspfCause c, OspfAction a) {
// //    OspfLog l = new OspfLog(c, a);
// //    if(!_log.remove(l))
// //      System.err.println("OspfLog failed to remove:" + l.hashcode());
// //  }

//   public long size() {
//     long size = 0;
//     for (OspfLog i : _log) {
//       size += i._route.length();
//     }
//     return _hostName.length() + size;
//   }

//   public void write2File(String path) {
//     String fileName = _hostName + "plog.json";
//     JSONArray plogArray = new JSONArray();
//     Integer num = 1;
//     for (OspfLog i : _log) {
//       JSONObject plog = makeJsonObject(num, i);
//       //      JSONObject plog = new JSONObject(String.valueOf(num), i._route);
//       plogArray.put(plog);
//       num++;
//     }

//     try{
//       Writer output = null;
//       File file = new File(path + fileName);
//       if(!file.exists() | file.isDirectory()) {
//         try{
//           file.createNewFile();
//         } catch (IOException e) {
//           System.err.println("File failed to create.");
//         }
//       }
//       output = new BufferedWriter(new FileWriter(file));
//       for (int i=0; i<plogArray.length(); i++) {
//         output.write(plogArray.getJSONObject(i).toString());
//         output.write("\n");
//       }
//       output.close();
//       System.out.println("Success: log has been written to file: " + path + fileName);
//     } catch (Exception e) {
//       System.err.println("Error: log failed to write to file: " + path + fileName);
//     }
//   }

//   private JSONObject makeJsonObject(Integer num, OspfLog l) {
//     JSONObject object = new JSONObject();
//     JSONArray array = new JSONArray();
//     JSONObject c = new JSONObject();
//     JSONObject a = new JSONObject();
//     //    Integer i = 1;
//     try{
//       //      c.put("Cause ", l._c.getCause());
//       //      c.put("Sender ", l._c.getSender());
//       //      a.put("Action ", l._a.getAction());
//       //      a.put("FibEntry ", l._a.getFibEntry());
//       //      a.put("Receiver ", l._a.getReceiver());
//       c.put(String.valueOf(num), l._route);
//       //      array.put(c);
//       //      array.put(a);
//       //      object.put("log ", c);
//     } catch (JSONException e) {
//       e.printStackTrace();
//     }
//     return c;
//   }

//   public void readfromFile(String path) {

//   }

// }
