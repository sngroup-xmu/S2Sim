package org.batfish.log;

//import com.google.gson.stream.JsonWriter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import javax.annotation.Nullable;
//import org.batfish.log.BgpLog;

//import java.io.FileOutputStream;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.io.OutputStreamWriter;
//import java.nio.file.Path;
//import java.util.Set;
//import javax.lang.model.SourceVersion;
//import javax.tools.Tool;

public class BgpLogs implements Serializable {
  static final String PROP_HOST_NAME = "hostName";
  static final String PROP_BGP_LOGS = "bgpLogs";


  private String _hostName;
//  public String rname;
  private ArrayList<BgpLog> _logs;
  @JsonIgnore
  private ArrayList<Long> runTime;
  @JsonIgnore
  private long toFileTime;

  public BgpLogs(){}

  public BgpLogs(String name){
//    this.rname = name;
    this._hostName = name;
    _logs = new ArrayList<BgpLog>();
    runTime = new ArrayList<Long>();
    toFileTime = 0;
  }

  public long getToFileTime() {
    return toFileTime;
  }

  public void addToFileTime(long time){
    toFileTime+=time;
  }

  public void addLog(BgpLog log){
    try {
      _logs.add(log);
    }catch (ClassCastException ca){
      ca.printStackTrace();
    }
  }
  public void runTimeAdd(long start, long end){
    return;
//    runTime.add(end-start);
  }
  @JsonCreator
  public BgpLogs(
      @Nullable @JsonProperty(PROP_HOST_NAME) String hostName,
      @Nullable @JsonProperty(PROP_BGP_LOGS) ArrayList<BgpLog> logs
  ){
    _hostName = hostName;
    _logs = logs;
  }

  @JsonProperty(PROP_HOST_NAME)
  public String get_hostName() {
    return _hostName;
  }

  @JsonProperty(PROP_BGP_LOGS)
  public ArrayList<BgpLog> get_logs() {
    return _logs;
  }

  public void tofile(String path) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(this);
    if (path != null){
      path = path + _hostName + "-bgplog.json";
    }else {
      path = System.getProperty("user.dir")+"/src/main/resources/jsonfile/"+_hostName+".json";
    }

//    System.out.println(json);
    FileWriter fileWriter = new FileWriter(new File(path));
    BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
    bufferedWriter.write(json);
    bufferedWriter.flush();
    bufferedWriter.close();
  }

  public void toFileSerializable(String path){
    File file = new File(path + _hostName);
    System.out.println("writing: "+file.getPath());
    if (!file.getParentFile().exists()){
      file.getParentFile().mkdirs();
    }
    try {
      ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(file));
      os.writeObject(this);
      os.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void toFileJson(String path){


    File file = new File(path + _hostName);
    System.out.println("writing: "+file.getPath());

    if (!file.getParentFile().exists()){
      file.getParentFile().mkdirs();
    }
    ObjectMapper mapper = new ObjectMapper();
    //    String json = mapper.writeValueAsString(_logs);

    try {

      mapper.writeValue(file, _logs);
    } catch (IOException e) {
      e.printStackTrace();
    }

  }
  public void emptyFunc(){}

}
