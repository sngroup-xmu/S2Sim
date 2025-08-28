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

    String srcNode;
    String dstNode;
    String reqDstPrefix;
    Set<String> waypointNodes;
    Set<String> bypassNodes;
    RequirementType type;
}
