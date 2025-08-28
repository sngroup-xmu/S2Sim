package org.batfish.log;

import java.util.ArrayList;
import org.batfish.datamodel.AbstractRoute;

public class BgpLogNode {
  public String hostName;
  public AbstractRoute route;
  public ArrayList<BgpLogNode> kids;
  public BgpLogNode parent;

  public BgpLogNode(){}
  BgpLogNode(String hostName, AbstractRoute route, BgpLogNode parentNode){
    this.hostName = hostName;
    this.route = route;
    this.parent = parentNode;
    this.kids = new ArrayList<BgpLogNode>();
  }
  BgpLogNode(String hostName, AbstractRoute route){
    this.hostName = hostName;
    this.route = route;
    this.kids = new ArrayList<BgpLogNode>();
  }
  BgpLogNode(String hostName){
    this.hostName = hostName;
    this.kids = new ArrayList<BgpLogNode>();
  }
  public void addKid(BgpLogNode node){
    kids.add(node);
  }

  public void setParent(BgpLogNode parent) {
    this.parent = parent;
  }

  public void setRoute(AbstractRoute route) {
    this.route = route;
  }
}
