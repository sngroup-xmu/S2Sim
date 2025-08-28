package org.batfish.log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.batfish.datamodel.AbstractRoute;
//import org.checkerframework.checker.units.qual.A;

public class RipLog<R extends AbstractRoute> {


  HashMap<String, ArrayList<R>> _cause;
  HashMap<String, ArrayList<R>> _discard;
  HashMap<String, ArrayList<R>> _select;
  HashMap<String, ArrayList<R>> _send;
//  private ArrayList<Long> _completedTime;
//  ArrayList<R> _causeRoutes;
//  ArrayList<R> _discardRoutes;
//  ArrayList<R> _selectRoutes;

  public enum Action{
    CAUSE,   //Origin or Receive
    DISCARD,
    SELECT,  //Add to _ripInternalRib
    SEND;    //Send to other routers
  }

  public RipLog() {
    _cause = new HashMap<>();
    _discard = new HashMap<>();
    _select = new HashMap<>();
    _send = new HashMap<>();
//    _completedTime = new ArrayList<>();
//    _causeRoutes = new ArrayList<>();
//    _discardRoutes = new ArrayList<>();
//    _selectRoutes = new ArrayList<>();
  }

//  public void addLatency(long a) {
//    _completedTime.add(a);
//  }

//  public ArrayList<Long> getLatency() {
//    return _completedTime;
//  }

  public void add(String hostname, R r, Action a) {
    switch (a) {
    case CAUSE:  {
      put(_cause, hostname, r);
      break;
    }
    case DISCARD: {
      put(_discard, hostname, r);
      break;
    }
    case SELECT: {
      put(_select, hostname, r);
      break;
    }
    case SEND: {
      put(_send, hostname, r);
      break;
    }
    }
  }

  private void put(Map<String, ArrayList<R>> map, String key, R r) {
    if(map.containsKey(key)) {
      map.get(key).add(r);
    }
    else {
      map.put(key, new ArrayList<>());
      map.get(key).add(r);
    }
  }

//  public long size() {
//    long space = _cause.forEach((key, value) -> {
//      long valueSize = value.size() * AbstractRoute.MAX_TAG;
//    });
//  }


}
