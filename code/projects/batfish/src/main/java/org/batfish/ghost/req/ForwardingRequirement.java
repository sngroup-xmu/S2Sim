package org.batfish.ghost.req;

import org.batfish.datamodel.Ip;

public class ForwardingRequirement{
    String _srcNode;
    String _dstNode;
    Ip _prefix;

    public ForwardingRequirement(Ip prefix) {
        _prefix = prefix;
    }
    

}