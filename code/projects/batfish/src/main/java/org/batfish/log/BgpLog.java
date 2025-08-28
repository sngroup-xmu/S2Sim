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
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.Bgpv4Route;
import org.batfish.datamodel.Prefix;
import org.batfish.dataplane.rib.RouteAdvertisement;

public class BgpLog implements Serializable {
  public static final String PROP_ITER = "iter";
  public static final String PROP_CAUSE = "cause";
  public static final String PROP_RECEIVED = "bgpReceived";
  public static final String PROP_INSTALLED = "installed";

  public int _iter; //actually, a global variable
  private Map<String, ArrayList<RouteAdvertisement<Bgpv4Route>>> _cause;
  private Map<String, ArrayList<AbstractRoute>> _bgpReceived; //import policy
  private ArrayList<AbstractRoute> _installed;

  public BgpLog(){}
  public BgpLog(int iter){
    this._iter = iter;
    _cause = new HashMap<String, ArrayList<RouteAdvertisement<Bgpv4Route>>>();
    _bgpReceived = new HashMap<String, ArrayList<AbstractRoute>>();
    _installed = new ArrayList<AbstractRoute>();
  }

  @JsonProperty(PROP_ITER)
  public int get_iter(){return _iter;}
  @JsonProperty(PROP_CAUSE)
  public Map<String, ArrayList<RouteAdvertisement<Bgpv4Route>>> get_cause(){
    return _cause;
  }
  @JsonProperty(PROP_RECEIVED)
  public Map<String, ArrayList<AbstractRoute>> get_bgpReceived(){return _bgpReceived;}
  @JsonProperty(PROP_INSTALLED)
  public ArrayList<AbstractRoute> get_installed(){return _installed;};

  @JsonCreator
  public BgpLog(
      @JsonProperty(PROP_ITER) int iter,
      @Nullable @JsonProperty(PROP_CAUSE) Map<String, ArrayList<RouteAdvertisement<Bgpv4Route>>> cause,
      @Nullable @JsonProperty(PROP_RECEIVED) Map<String, ArrayList<AbstractRoute>> received,
      @Nullable @JsonProperty(PROP_INSTALLED) ArrayList<AbstractRoute> installed
  ){
    _iter = iter;
    _cause = cause;
    _bgpReceived = received;
    _installed = installed;
  }

  public void addCause(String hostName, RouteAdvertisement<Bgpv4Route> causeRoute){
    if (_cause.get(hostName)==null){
      ArrayList<RouteAdvertisement<Bgpv4Route>> routes = new ArrayList<RouteAdvertisement<Bgpv4Route>>();
      routes.add(causeRoute);
      _cause.put(hostName, routes);
    } else {
      _cause.get(hostName).add(causeRoute);
    }
  }

  public void addReceived(String hostName, AbstractRoute route){
    if (_bgpReceived.get(hostName)==null){
      ArrayList<AbstractRoute> routes = new ArrayList<AbstractRoute>();
      routes.add(route);
      _bgpReceived.put(hostName, routes);
    } else {
      _bgpReceived.get(hostName).add(route);
    }
  }

  public void addInstalled(AbstractRoute route){
    _installed.add(route);
  }

  public void toFileSerializable(String path, String _hostName, boolean print){
    if ((this._cause.keySet().isEmpty())&(this._bgpReceived.keySet().isEmpty())){
      if (this._installed.isEmpty()){
        if (print){
          System.out.println("iter: "+_iter+"  router: "+_hostName+" ***no record***");
        }
        return;
      }

    }
    path = path + _hostName + "-bgplog"+_iter;
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
