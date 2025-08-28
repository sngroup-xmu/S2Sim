package org.batfish.ghost;

// import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
// import java.util.List;
import java.util.Set;

import org.batfish.datamodel.Bgpv4Route;
// import org.glassfish.grizzly.Copyable;

public class GhostBgpv4Route{
    private Bgpv4Route _bgpv4Route;
    private Set<Integer> _existConds;
    private Set<Integer> _selectedConds;
    private boolean _isMatchSelection;

    public GhostBgpv4Route(Bgpv4Route r) {
        _bgpv4Route = r;
        _existConds = new HashSet<>();
        _selectedConds = new HashSet<>();
        _isMatchSelection = false;
    }

    public void addExistCond(int condNum) {
        _existConds.add(condNum);
    }

    public Bgpv4Route getBgpv4Route() {
        return _bgpv4Route;
    }

    public void setBgpv4Route(Bgpv4Route r) {
        _bgpv4Route = r;
    }

    public boolean getIsMatch() {
        return _isMatchSelection;
    }
    
    public void setIsMatch(boolean value) {
        _isMatchSelection = value;
    }

    public void addSelectedCond(int condNum) {
        _selectedConds.add(condNum);
    }

    public Set<Integer> getExistCondsList() {
        return _existConds;
    }

    public Set<Integer> getSelectedConsList() {
        return _selectedConds;
    }

    public boolean isSelectedAtCond(int condNum) {
        return _selectedConds.contains(condNum);
    }

    public int getMaxSelectedCond() {
        return Collections.max(_selectedConds);
    }

    public int getMaxExistCond() {
        return Collections.max(_existConds);
    }

    public void setExistCondList(Set<Integer> condList) {
        _existConds = condList;
    }


    public void removeExistCond(int condNum) {
        _existConds.remove(condNum);
    }

    public void removeSelectedCond(int condNum) {
        _selectedConds.remove(condNum);
    }

    public void setExistCondListCopy(Set<Integer> condList) {
        Set<Integer> l = new HashSet<>();
        for (Integer i : condList) {
            l.add(i);
        }
        _existConds = l;
    }

    public void setSelectionListCopy(Set<Integer> condList) {
        Set<Integer> l = new HashSet<>();
        for (Integer i : condList) {
            l.add(i);
        }
        _selectedConds = l;
    }

    // @Override
    // public GhostBgpv4Route clone() {
    //     Bgpv4Route r = _bgpv4Route.toBuilder()
    //                                     .setAdmin(_bgpv4Route.getAdministrativeCost())
    //                                     .setAsPath(_bgpv4Route.getAsPath())
    //                                     .setClusterList(_bgpv4Route.getClusterList())
    //                                     .setCommunities(_bgpv4Route.getCommunities())
    //                                     .setLocalPreference(_bgpv4Route.getLocalPreference())
    //                                     .setMetric(_bgpv4Route.getMetric())
    //                                     .setNetwork(_bgpv4Route.getNetwork())
    //                                     .setNextHop(_bgpv4Route.getNextHop())
    //                                     .setNextHopInterface(_bgpv4Route.getNextHopInterface())
    //                                     .setNextHopIp(_bgpv4Route.getNextHopIp())
    //                                     .setNonForwarding(_bgpv4Route.getNonForwarding())
    //                                     .setNonRouting(_bgpv4Route.getNonRouting())
    //                                     .setOriginType(_bgpv4Route.getOriginType())
    //                                     .setProtocol(_bgpv4Route.getProtocol())
    //                                     .setReceivedFromIp(_bgpv4Route.getReceivedFromIp())
    //                                     .setReceivedFromRouteReflectorClient(_bgpv4Route.getReceivedFromRouteReflectorClient())
    //                                     .setSrcProtocol(_bgpv4Route.getSrcProtocol())
    //                                     .setTag(_bgpv4Route.getTag())
    //                                     .setWeight(_bgpv4Route.getWeight())
    //                                     .build();
    //     GhostBgpv4Route gRoute = new GhostBgpv4Route(r);
    //     gRoute.setExistCondListCopy(_existConds);
    //     gRoute.setSelectionListCopy(_selectedConds);
    //     return gRoute;
    // }


}

    