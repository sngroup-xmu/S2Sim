package org.batfish.log;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.batfish.datamodel.LineAction;
import org.batfish.representation.cisco.RouteMapClause;
import org.batfish.representation.cisco.RouteMapMatchLine;
import org.batfish.representation.cisco.RouteMapSetLine;

//Policy Template.
//For a router, for each neighbor of the router, there are two RouteMap:
//import from this neighbor and export to this neighbor
//The RouteMap are not associated with the nodes on the Pgraph.

public class RouteMapLine implements Serializable {
  @JsonProperty("name")
  public String name = "";
  @JsonProperty("access")
  public LineAction access=null;
  @JsonProperty("lineno")
  public int lineno = 0;
  @JsonProperty("matches")
  public Map<String, RouteMapMatchLine> matches = new HashMap<>();
  @JsonProperty("actions")
  public List<RouteMapSetLine> actions = null;


  public RouteMapLine(RouteMapClause routeMapClause){
    name = routeMapClause.getMapName();
    access = routeMapClause.getAction();
    lineno = routeMapClause.getSeqNum();
    for (RouteMapMatchLine mapMatchLine: routeMapClause.getMatchList()){
      matches.put(mapMatchLine.getClass().getName(), mapMatchLine);
    }
    actions = routeMapClause.getSetList();
  }

//  @JsonProperty()


  @Override
  public String toString() {
    return "RouteMapLine{" + "matches=" + matches.toString() + ", actions=" + actions.toString() + ", access='" + access
        + '\'' + '}';
  }

//  public void generate_template(String host_name, String policy_name,
//      Integer lines, String[] match_types, String[] action_types, String networks, String community) {
//    for (int i=0; i<lines; i++) {
//      MatchSelectOne m = new MatchSelectOne();
//      m.generate_template(host_name, match_types, networks, community);
//      this.matches.add(m);
//      ActionSetOne a = new ActionSetOne();
//      a.generate_template(host_name, action_types, community);
//      this.actions.add(a);
//    }
//  }
//
//  public void generate_template(String host_name, String policy_name) {
//    this.generate_template(host_name, policy_name, 1, null, null, "1", "1");
//  }
}
