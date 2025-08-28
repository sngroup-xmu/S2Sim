package org.batfish.log;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.batfish.datamodel.DefinedStructureInfo;
import org.batfish.representation.cisco.CiscoConfiguration;
import org.batfish.representation.cisco.IpAsPathAccessList;
import org.batfish.representation.cisco.PrefixList;
import org.batfish.representation.cisco.RouteMapClause;
import org.batfish.representation.cisco.RouteMapMatchAsPathAccessListLine;
import org.batfish.representation.cisco.RouteMapMatchCommunityListLine;
import org.batfish.representation.cisco.RouteMapMatchIpPrefixListLine;
import org.batfish.representation.cisco.RouteMapMatchLine;
import org.batfish.representation.cisco.StandardCommunityList;

public class RouteMapMap implements Serializable {
  @JsonProperty("lines")
  public Map<Integer, RouteMapLine> lines = new HashMap<>();
  @JsonProperty("line_range")
  public DefinedStructureInfo info = null;
  @JsonProperty("name")
  public String name = null;
  @JsonProperty("community_lists")
  public Map<String, StandardCommunityList> _standardCommunityLists = new HashMap<>();
  @JsonProperty("as_path_lists")
  public Map<String, IpAsPathAccessList> _asPathAccessLists = new HashMap<>();
  @JsonProperty("prefix_lists")
  public Map<String, PrefixList> _prefixLists = new HashMap<>();

  public RouteMapMap(org.batfish.representation.cisco.RouteMap rmap, CiscoConfiguration configuration){
    name = rmap.getName();
    String route_map = "route-map";
    info = configuration.get_structureDefinitions().get(route_map).get(name);

    for (Integer lineno:rmap.getClauses().descendingKeySet()){
      RouteMapClause clause = rmap.getClauses().get(lineno);
      RouteMapLine routeMapLine = new RouteMapLine(clause);
      lines.put(lineno,routeMapLine);
      for (RouteMapMatchLine line : clause.getMatchList()) {
        if (line instanceof RouteMapMatchCommunityListLine){
          for (String list_name:((RouteMapMatchCommunityListLine) line).getListNames()){
            _standardCommunityLists.put(list_name, configuration.getStandardCommunityLists().get(list_name));
          }
        }
        if (line instanceof RouteMapMatchAsPathAccessListLine){
          for (String list_name:((RouteMapMatchAsPathAccessListLine) line).getListNames()){
            _asPathAccessLists.put(list_name, configuration.getAsPathAccessLists().get(list_name));
          }
        }
        if (line instanceof RouteMapMatchIpPrefixListLine){
          for (String list_name:((RouteMapMatchIpPrefixListLine) line).getListNames()){
            _prefixLists.put(list_name, configuration.getPrefixLists().get(list_name));
          }
        }
      }
    }
  }
}
