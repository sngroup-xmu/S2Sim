package org.batfish.diagnosis.common;

import java.util.Set;

/**
 * 网络需求，目前准备三种：
 *  1）可达性
 *  2）waypoint
 *  3）bypass
 */
public class FlowRequirement {
    public enum RequirementType{
        REACH,
        WAYPOINT,
        BYPASS
    }

    String _srcNode;
    String _dstNode;
    String _reqDstPrefix;
    Set<String> _waypointNodes;
    Set<String> _bypassNodes;
    RequirementType _type;
}
