package org.batfish.log;

//import java.util.ArrayList;
//import com.alibaba.fastjson.annotation.JSONField;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.batfish.datamodel.OspfRoute;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.Bgpv4Route;
import org.batfish.datamodel.OspfIntraAreaRoute;
import org.batfish.datamodel.OspfRoute;
import org.batfish.datamodel.Prefix;
import org.batfish.dataplane.rib.RouteAdvertisement;

public class OspfLog implements Serializable {
  public static final String PROP_ITER = "iter";
  public static final String PROP_CAUSE = "cause";
  public static final String PROP_RECEIVED = "processed";
  public static final String PROP_INSTALLED = "installed";
  public static final String PROP_SEND = "send";

  public int _iter; //actually, a global variable
  private Map<String, ArrayList<OspfRoute>> _cause;
  private Map<String, ArrayList<OspfRoute>> _processed; //import policy
  private ArrayList<AbstractRoute> _installed;
  private Map<String, ArrayList<OspfRoute>> _send;

  public OspfLog(){}
  public OspfLog(int iter){
    this._iter = iter;
    _cause = new HashMap<String, ArrayList<OspfRoute>>();
    _processed = new HashMap<String, ArrayList<OspfRoute>>();
    _installed = new ArrayList<AbstractRoute>();
    _send = new HashMap<String, ArrayList<OspfRoute>>();
  }

  @JsonProperty(PROP_ITER)
  public int get_iter(){return _iter;}
  @JsonProperty(PROP_CAUSE)
  public Map<String, ArrayList<OspfRoute>> get_cause(){
    return _cause;
  }
  @JsonProperty(PROP_RECEIVED)
  public Map<String, ArrayList<OspfRoute>> get_processed(){return _processed;}
  @JsonProperty(PROP_INSTALLED)
  public ArrayList<AbstractRoute> get_installed(){return _installed;};
  public Map<String, ArrayList<OspfRoute>> get_send(){
    return _send;
  }

  @JsonCreator
  public OspfLog(
      @JsonProperty(PROP_ITER) int iter,
      @Nullable @JsonProperty(PROP_CAUSE) Map<String, ArrayList<OspfRoute>> cause,
      @Nullable @JsonProperty(PROP_RECEIVED) Map<String, ArrayList<OspfRoute>> processed,
      @Nullable @JsonProperty(PROP_INSTALLED) ArrayList<AbstractRoute> installed,
      @Nullable @JsonProperty(PROP_SEND) Map<String, ArrayList<OspfRoute>> send
  ){
    _iter = iter;
    _cause = cause;
    _processed = processed;
    _installed = installed;
    _send = send;
  }

  public void addCause(String hostName, OspfRoute causeRoute){
    if (_cause.get(hostName)==null){
      ArrayList<OspfRoute> routes = new ArrayList<OspfRoute>();
      routes.add(causeRoute);
      _cause.put(hostName, routes);
    } else {
      _cause.get(hostName).add(causeRoute);
    }
  }

  public void addSend(String hostName, OspfRoute t){
    if (_send.get(hostName)==null){
      ArrayList<OspfRoute> routes = new ArrayList<OspfRoute>();
      routes.add(t);
      _send.put(hostName, routes);
    } else {
      _send.get(hostName).add(t);
    }
  }

  public void addProcessed(String hostName, OspfRoute route){
    if (_processed.get(hostName)==null){
      ArrayList<OspfRoute> routes = new ArrayList<OspfRoute>();
      routes.add(route);
      _processed.put(hostName, routes);
    } else {
      _processed.get(hostName).add(route);
    }
  }

  public void addInstalled(OspfRoute route){
    _installed.add(route);
  }

  public void toFileSerializable(String path, String _hostName, boolean print){
    if ((this._cause.keySet().isEmpty())&(this._processed.keySet().isEmpty())){
      if (this._installed.isEmpty()){
        if (print){
          System.out.println("iter: "+_iter+"  router: "+_hostName+" ***no record***");
        }
        return;
      }

    }
    path = path + _hostName + "-OspfLog"+_iter;
    try {
      ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(path));
      os.writeObject(this);
      os.close();
      if (print){
        System.out.println("iter: "+_iter+"  router: "+_hostName);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


}
