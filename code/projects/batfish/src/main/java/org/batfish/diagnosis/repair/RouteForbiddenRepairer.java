package org.batfish.diagnosis.repair;

import java.util.ArrayList;
import java.util.List;

import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.AsSet;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.diagnosis.common.ConfigurationLine;
import org.batfish.diagnosis.localization.RouteForbiddenLocalizer;
import org.batfish.diagnosis.util.KeyWord;

public class RouteForbiddenRepairer extends Repairer {
   RouteForbiddenLocalizer localizer;

   public AbstractRoute getForbiddenRoute() {
       return localizer.getRoute();
   }

   //TODO: 考虑同一个policy多次被localized，生成repair的时候序号不要重叠
   public RouteForbiddenRepairer(RouteForbiddenLocalizer localizer) {
       this.localizer = localizer;
   }


   public String getPolicyName() {
       return localizer.getPolicyName();
   }


   /*
    * 修复步骤
      STEP 0: 找到拦截的那条规则：
      *       只考虑
      *        a) [ip-prefix ... deny + route-policy ... permit] / [ip-prefix ... permit + route-policy ... deny]
      *        b) [ip-prefix ... deny + route-policy ... deny]
      *       最终都是找到那条explicitly match 的 ip-prefix + route-policy 改成permit
      STEP 1: 没有这样一条完全匹配的ip规则拦截，则在route-policy最前面新添规则
      STEP 2: 如何在现有配置上改动最小（可以都先按STEP 0做 然后压缩配置？）
    *
    */
   @Override
   public void genRepair() {

       Configuration configuration = localizer.getConfiguration();

       // STEP 0.1: 找到拦截的那条route-policy规则
       String policyName = localizer.getPolicyName();
       if (policyName!=null) {
           RoutingPolicy routingPolicy = configuration.getRoutingPolicies().get(policyName);
       } else {
            System.out.println("没有找到policyName");
           return;
       }
       // STEP 0.2: 找到拦截的那条ip-prefix规则


       // STEP 1: 没有这样一条完全匹配的ip规则拦截，则在route-policy最前面新添规则【startLine是route-policy最前面】
       ConfigurationLine startLine = new ConfigurationLine(Integer.MAX_VALUE, "");
       for (ConfigurationLine line : localizer.getErrorLines()) {
            if (line.getLineNumber() < startLine.getLineNumber() && !line.getLine().contains(KeyWord.BGP_NEIGHBOR)) {
                startLine.setLineNumber(line.getLineNumber());
                startLine.setLine(line.getLine());
            }
       }
       String prefixStr = "";
       String asPathStr = "";
       int insertLine = startLine.getLineNumber();
       if (localizer.getBgpv4Route()!=null) {
            prefixStr = localizer.getBgpv4Route().getNetwork().toString();
            asPathStr = localizer.getBgpv4Route().getAsPath().toString().replace("[", "").replace("]", "");
       } else {
            prefixStr = localizer.getBgpRouteLog().getIpPrefixString();
            asPathStr = localizer.getBgpRouteLog().getAsPath().toString().replace("[", "").replace("]", "");
       }
       // 生成ip prefix-list
       String newPrefixListName = "FILTER_" + prefixStr.replace("/", "_").replace(".", "_");
       String newPrefixList = "ip prefix-list " + newPrefixListName + " seq 10 permit " + prefixStr + "\n" + "!";
       addAddedLine(insertLine, newPrefixList);
       String newAsListSeq = "10";
       String newAsList = "ip as-path access-list " + newAsListSeq + " permit " + "^" + asPathStr + "$" + "\n" + "!";
       addAddedLine(insertLine, newAsList);
       // 生成route-policy
       String[] words = startLine.getLine().split(" ");
       int seqNum = Integer.parseInt(words[words.length-1]);
       String newRoutePolicy = "route-policy " + policyName + " permit " + (seqNum-1);
       addAddedLine(insertLine, newRoutePolicy);
       addAddedLine(insertLine, "match as-path "+ newAsListSeq);
       addAddedLine(insertLine, "match ip-prefix " + newPrefixListName);
       addAddedLine(insertLine, "!");
   }



}
