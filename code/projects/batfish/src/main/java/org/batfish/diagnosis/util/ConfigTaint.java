package org.batfish.diagnosis.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Ip6;
import org.batfish.datamodel.LineAction;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;
import org.batfish.dataplane.rib.StaticRib;
import org.batfish.diagnosis.common.ConfigurationLine;

public class ConfigTaint {
    /**
     * 获取配置文件里BGP进程号
     * @param filePath
     *
     */
   public static Long getAsNumber(String filePath) {
       BufferedReader reader;
       try {
           reader = new BufferedReader(new FileReader(filePath));
           String line = reader.readLine().trim();
           int lineNum = 1;

           while (line != null) {
               // System.out.println(line);
               // read next line
               if (!line.startsWith("#") && !line.startsWith("!")) {
                    if (line.trim().contains(KeyWord.BGP_PROCESS_COMMAND)) {
                        String[] words = line.split(" ");
                        return Long.parseLong(words[words.length-1]);
                    }
               }

               line = reader.readLine();
               lineNum += 1;
           }
           reader.close();
       } catch (IOException e) {
           e.printStackTrace();
       }
       return null;
   }

   /**
    * 针对 BGP unicast情况，如果没有 address-family ipv4视图，那就返回 bgp 视图
    * @param filePath
    *
    */
   public static ConfigurationLine findBgpAddressFamilyLine(String filePath) {
       List<ConfigurationLine> lines = new ArrayList<>();
       BufferedReader reader;

       try {
           reader = new BufferedReader(new FileReader(filePath));
           String line = reader.readLine().trim();
           int lineNum = 1;
           while (line != null) {
                line = line.trim();
                if (!line.startsWith("#") && !line.startsWith("!")) {
                    if (line.contains(KeyWord.BGP_PROCESS_COMMAND) || line.contains(KeyWord.ADDRESS_FAMILY)) {
                        lines.add(new ConfigurationLine(lineNum, line));
                    }
                }

               line = reader.readLine();
               lineNum += 1;
           }
           reader.close();
       } catch (IOException e) {
           e.printStackTrace();
       }
       return lines.get(lines.size()-1);
   }

   // 不包括找默认路由
   public static StaticRoute staticRouteFinder(StaticRib staticRib, Prefix prefix, boolean strictMatchPrefix, boolean considerDefaultRoute) {
       // 查找路由表里的
       staticRib.getRoutes(prefix);
       return null;
   }

   public static String genMissingNetworkConfigLine(Prefix prefix) {
       return "network " + prefix.getStartIp().toString() + " " + prefix.getPrefixLength();
   }

   public static String genIpPrefixRuleConfigLine(Prefix prefix, String name, int nodeIndex, String matchAction) {
       return "ip prefix-list " + name + " seq " + nodeIndex + " " + matchAction + " " + prefix.getFirstHostIp().toString() + "/" + prefix.getPrefixLength();
   }

   public static String genStaticRouteLine(StaticRoute staticRoute) {
        //long preference = staticRoute.getMetric();
        String network = staticRoute.getNetwork().toString();
        String prefixString = network.split("/")[0];
        String mask = "255.255.255.0";
        String nextHop = staticRoute.getNextHopInterface();

       return KeyWord.IP_STATIC_ROUTE_PREFIX + " " + prefixString + " " + mask + " " + nextHop;

   }

   /*
    * 把相应名字的接口相关行找到【不包括父接口】
    */
   public static List<ConfigurationLine> interfaceLinesFinder(String targetInfName, Interface targetInf, String filePath) {

       List<ConfigurationLine> lines = new ArrayList<>();
       BufferedReader reader;
       try {
           reader = new BufferedReader(new FileReader(filePath));
           String line = reader.readLine().trim();
           int lineNum = 1;
           boolean reachedTargetLine = false;
           while (line != null) {
             if (!line.startsWith("#") && !line.startsWith("!")) {
               if (reachedTargetLine && line.contains(KeyWord.ENDING_TOKEN)) {
                   break;
               }
               if (reachedTargetLine) {
                   lines.add(new ConfigurationLine(lineNum, line));
               }

               if (line.startsWith(KeyWord.INTERFACE)) {
                   // 行的第二位是接口名称
                   String thisInfName = line.split(" ")[1];
                   if (targetInfName.equals(thisInfName) || targetInfName.contains(thisInfName) || thisInfName.contains(targetInfName)) {
                       lines.add(new ConfigurationLine(lineNum, line));
                       reachedTargetLine = true;
                   }
               }
             }
            line = reader.readLine();
            lineNum += 1;
           reader.close();
        }
       } catch (IOException e) {
           e.printStackTrace();
       }
       return lines;

   }



   public static ConfigurationLine staticRouteLinesFinder(String node, StaticRoute staticRoute, String filePath) {
       // filePath是cfg文件地址，keyWords是静态路由相关的
       // 直接在入参的route上了
       BufferedReader reader;
       try {
           reader = new BufferedReader(new FileReader(filePath));
           String line = reader.readLine().trim();
           int lineNum = 1;
           while (line != null) {
               line = line.trim();
               if (!line.startsWith("#") && !line.startsWith("!")) {
                    if (line.startsWith(KeyWord.IP_STATIC_ROUTE_PREFIX)) {
                        String[] words = line.split(" ");
                        /* 主要是区分前缀是掩码一起还是分开写的
                            * 0  1     2      3
                            * ip route prefix mask { ip-address | interface-type interface-number [ ip-address ]}
                            */
                        if (Prefix.create(Ip.parse(words[2]), Ip.parse(words[3])).containsPrefix(staticRoute.getNetwork())) {
                            // ip-prefix 形式
                            if (staticRoute.getNetwork().equals(Prefix.parse(words[2]))) {
                                return new ConfigurationLine(lineNum, line);
                            }
                        } else {
                            assert false: "WRONG CONFIG: " + line + " @" + node + ":" + lineNum;
                        }
                    }
               }
               line = reader.readLine();
               lineNum += 1;
           }
           reader.close();
       } catch (IOException e) {
           e.printStackTrace();
       }
       return null;
   }

   /**
    * 找到某 route-map policyName 的所有相关配置【不包括引用它的地方】【也不包括它引用的prefix-list这种】
    * 1) ip-prefix
    * 2) community-filter
    *
    * @param node       节点
    * @param policyName 政策名字
    * @param fileName   文件名称
    * @return {@link Map}<{@link Integer}, {@link String}>
    *//*
    *
    */
   public static Map<Integer, String> policyLinesFinder(String node, String policyName, String fileName) {
       //fileName改成自己存放配置的目录
       String route_policy = KeyWord.ROUTE_POLICY + " " + policyName;

       BufferedReader reader = null;
       try {
           reader = new BufferedReader(new FileReader(fileName));
           String tempString = null;
           int line = 1;
           boolean flag = false;
           Map<Integer,String> route_policy_map = new LinkedHashMap<>();
           ArrayList<String> if_match_list = new ArrayList<String>();
           while ((tempString = reader.readLine()) != null) {
               tempString = tempString.trim();
               if(tempString.startsWith(route_policy)) {//输出route-map那行
                   route_policy_map.put(line,tempString);
                   flag = true;
               }
               else {
                   if(flag && tempString.equals(KeyWord.ENDING_TOKEN)){//输出到!那行
                       flag = false;
                   }else if(flag){//#输出route-map到#之间的内容

                       route_policy_map.put(line,tempString);
                       if(tempString.startsWith(KeyWord.POLICY_MATCH)){
//                           tempString = tempString.replaceAll(KeyWord.POLICY_MATCH,"ip");
                           if_match_list.add(tempString);
                       }
                   }
                //    else{
                //        String modified_tempString = tempString;
                //        //@TODO: ??这啥
                //        if(tempString.contains("basic")){
                //            modified_tempString = tempString.replace("basic ","");
                //        }
                //        else if(tempString.contains("advanced")){
                //            modified_tempString = tempString.replace("advanced ","");
                //        }
                //        for(String keyword:if_match_list){
                //            if(modified_tempString.startsWith(keyword)){
                //                route_policy_map.put(line,tempString);
                //                break;
                //            }

                //        }
                //    }
               }
               line++;
           }
           reader.close();
           return route_policy_map;
       } catch (IOException e) {
           e.printStackTrace();
       } finally {
           if (reader != null) {
               try {
                   reader.close();
               } catch (IOException e1) {
                   e1.printStackTrace();
               }
           }
       }
       return new LinkedHashMap<Integer, String>();
   }

   public static Map<Integer, String> genMissingPeerConfigLines(String peerName, String peerIp, String asNumber) {
       Map<Integer, String> lines = new LinkedHashMap<>();
       lines.put(-1, KeyWord.BGP_PEER + " " + peerIp + " " + "remote-as" + " " + asNumber);
       lines.put(-2, KeyWord.BGP_PEER + " " + peerIp + " description " + "\"To " + peerName + "\"");
       return lines;
   }

   public static String genMissingBgpReflectLine(String peerIp) {
       return KeyWord.BGP_PEER + " " + peerIp + " reflect-client";
   }

   /**
    * route-map行的格式是:
    * 0            1                   2           3    4
    * route-map [route-map-name] [matchMode] node [node-index]
    *
    * @return {@link String}
    */
   public static String genMissingRoutePolicyLine(String policyName, LineAction matchMode, int nodeIndex) {
       return KeyWord.ROUTE_POLICY + " " + policyName + " " + matchMode.toString() + " " + "seq" + " " + nodeIndex;
   }

   /*
    * 找到包含所有关键字的全部行
    */
   public static Map<Integer, String> taint(String node, String[] keyWords, String filePath) {
       Map<Integer, String> lineMap = new LinkedHashMap<>();
       BufferedReader reader;

       try {
           reader = new BufferedReader(new FileReader(filePath));
           String line = reader.readLine().trim();
           int lineNum = 1;
           while (line != null) {
               // System.out.println(line);
               // read next line
               if (!line.startsWith("#") && !line.startsWith("!")) {
                 if (ifThisLineMatch(line, keyWords)) {
                   lineMap.put(lineNum, line.trim());
                 }
               }
               line = reader.readLine();
               lineNum += 1;
           }
           reader.close();
       } catch (IOException e) {
           e.printStackTrace();
       }
       return lineMap;
   }

   public static boolean ifThisLineMatch(String line, String[] keyWords) {
       for (String word : keyWords) {
           if (!line.contains(word.trim())) {
               return false;
           }
       }
       return true;
   }

   public static Map<Integer, String> taintWithForbidWord(String node, String[] keyWords, String forbidWord, String filePath) {
       Map<Integer, String> lineMap = new LinkedHashMap<>();
       BufferedReader reader;

       try {
           reader = new BufferedReader(new FileReader(filePath));
           String line = reader.readLine().trim();
           int lineNum = 1;
           while (line != null) {
               // System.out.println(line);
               // read next line
               if (!line.startsWith("#") && !line.startsWith("!")) {
                    boolean ifThisLine = false;
                    String[] lineWords = line.split(" ");
                    if (ifLineContaintsAllWords(line, keyWords) && !line.contains(forbidWord) && !line.contains(KeyWord.ENDING_TOKEN)) {
                        ifThisLine = true;
                    }

                    if (ifThisLine) {
                        lineMap.put(lineNum, line.trim());
                    }
               }
               line = reader.readLine();
               lineNum += 1;
           }
           reader.close();
       } catch (IOException e) {
           e.printStackTrace();
       }
       return lineMap;
   }

   public static String transPrefixOrIpToIpString(String str) {
       if (str.contains("/")) {
           str = str.split("/")[0];
       }
       return str;
   }

   public static String transPrefixOrIpToPrefixString(String str) {
       if (!str.contains("/")) {
           str += "/32";
       }
       return str;
   }

   public static boolean isIpv4OrIpv6(String ipString) {
       ipString = transPrefixOrIpToIpString(ipString);
       Optional<Ip> ipv4 = Ip.tryParse(ipString);
       if (!ipv4.isPresent()) {
           Optional<Ip6> ipv6 = Ip6.tryParse(ipString);
           if (!ipv6.isPresent()) {
               return false;
           }
       }
       return true;
   }

   /**
    * peerTaint的时候调用，检查当前行是否包含相应ipString【主要是考虑ipv6有时在配置里会用到缩写的情况】
    */

   private static boolean ifLineContainsIp4OrIp6(String line, String ipString) {
       ipString = transPrefixOrIpToIpString(ipString);
       String[] words = line.split(" ");
       for (String word: words) {
           if (word.contains(".") || word.contains(":")) {
               if (Ip.tryParse(ipString).isPresent()) {
                   return word.equals(ipString);
               } else if (Ip6.tryParse(ipString).isPresent() && Ip6.tryParse(word).isPresent()) {
                   return Ip6.parse(ipString).equals(Ip6.tryParse(word).isPresent());
               }
           }
       }
       return false;
   }

   /**
     * 根据配置文件路径、route-map 名称和 neighbor IP 查找：
     * 1. 给定 neighbor IP 对应的 peer-group 使用行。
     * 2. 如果该 neighbor 所属的 peer-group使用了指定 route-map，
     *    则输出该 peer-group的定义行（要求该行为严格定义行：“neighbor <group> peer-group”）。
     *
     * @param configFilePath 配置文件路径
     * @param routeMapName   route-map 名称，例如 "policy_1"
     * @param neighborIP     neighbor IP 地址，例如 "163.154.129.166"
     */
    public static Map<Integer, String> peerPolicyTaint(String filePath, String routeMapName, String neighborIP, String direction) {
        Map<Integer, String> lineMap = new LinkedHashMap<>();
        BufferedReader reader;

        String neighborPeerGroupLine = null; // 给定 neighbor 对应的 peer-group 使用行
        String neighborPG = null;            // neighbor 所属的 peer-group 名称
        String neighborWithRouteMapLine = null;

        // 用于存储 peer-group 定义行（严格定义行：仅出现 "neighbor <group> peer-group"）
        // Map<String, String> pgDefinitionMap = new HashMap<>();

        // 用于存储 route-map 引用行，键为 peer-group 名称
        Map<String, String> routeMapRefMap = new HashMap<>();

        // 正则表达式定义：
        Pattern neighborWithRouteMapPattern = Pattern.compile("^\\s*neighbor\\s+"
                + Pattern.quote(neighborIP) + "\\s+route-map\\s+" + Pattern.quote(routeMapName) + "\\s+(\\S+)+\\s*$");
        // ① 匹配给定 neighbor IP 的 peer-group 使用行（例如：neighbor 163.154.129.166 peer-group group_0）
        Pattern neighborPattern = Pattern.compile("^\\s*neighbor\\s+"
                + Pattern.quote(neighborIP) + "\\s+peer-group\\s+(\\S+).*");
        // ② 严格匹配 peer-group 定义行，要求行仅包含 "neighbor <group> peer-group"
//        Pattern pgDefinitionPattern = Pattern.compile("^\\s*neighbor\\s+(\\S+)\\s+peer-group\\s*$");
        // ③ 匹配 route-map 引用行，格式必须为 "neighbor <group> route-map <routeMapName> out"
        Pattern routeMapPattern = Pattern.compile("^\\s*neighbor\\s+(\\S+)\\s+route-map\\s+"
                + Pattern.quote(routeMapName) + "\\s+(\\S+)+\\s*$");


        try {
            reader = new BufferedReader(new FileReader(filePath));
            String line = reader.readLine().trim();
            int lineNum = 1;
            while (line != null) {
                if (!line.startsWith("#") && !line.startsWith("!")) {
                    // ① 查找 neighbor 的 peer-group 行
                    if (neighborPeerGroupLine == null) {
                        Matcher nm = neighborPattern.matcher(line);
                        if (nm.find()) {
                            neighborPeerGroupLine = line;
                            lineMap.put(lineNum, line);
                            neighborPG = nm.group(1);
                        }
                    }
                    if (neighborWithRouteMapLine == null) {
                        Matcher nm = neighborWithRouteMapPattern.matcher(line);
                        if (nm.find()) {
                            neighborWithRouteMapLine = line;
                            lineMap.put(lineNum, line);
                            return lineMap;
                        }
                    }
                }

                // ③ 收集所有引用指定 route-map 的行
                Matcher rmm = routeMapPattern.matcher(line);
                if (rmm.find()) {
                    String pgRouteMap = rmm.group(1);
                    if (pgRouteMap.equals(neighborPG)) {
                        lineMap.put(lineNum, line);
                        return lineMap;
                    }
                }
                line = reader.readLine();
                lineNum += 1;
            }

            // if (neighborWithRouteMapLine != null) {
            //     System.out.println(neighborWithRouteMapLine);
            // } else {
            //     System.out.println("=== 结果 ===");
            //     // 输出 neighbor IP 的 peer-group 使用行
            //     if (neighborPeerGroupLine != null) {
            //         System.out.println("【Neighbor " + neighborIP + " 的 peer-group 行】");
            //         System.out.println(neighborPeerGroupLine);
            //     } else {
            //         System.out.println("未找到 neighbor " + neighborIP + " 的 peer-group 行。");
            //     }

            //     System.out.println();
            //     System.out.println("【与 route-map " + routeMapName + " 相关的 peer-group 定义行】");



            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lineMap;
    }

   // 查找 neighbor peerip [keyword] 语句是否存在，遇到group可以迭代找到相关语句
   public static Map<Integer, String> peerTaint(String node, String[] keyWords, String filePath) {
       Map<Integer, String> lineMap = new LinkedHashMap<>();
       // 检查关键词前两位是否是peer和ip地址, 固定index 0是peer关键字, index 1是peer ip【ipv4和ipv6都考虑】

       if (keyWords.length<2 || !keyWords[0].equals(KeyWord.BGP_PEER) || !isIpv4OrIpv6(keyWords[1])) {
           return lineMap;
       }
       String peerIpString = keyWords[1];
       boolean ifIpv6Peer = isIpv4OrIpv6(peerIpString);
       keyWords[1] = "";

       BufferedReader reader;

       try {
           reader = new BufferedReader(new FileReader(filePath));
           String line = reader.readLine().trim();
           int lineNum = 1;
           while (line != null) {
               line = line.trim();
               if (!line.startsWith("#") && !line.startsWith("!")) {
                    boolean ifThisLine = true;
                    if (line.contains(keyWords[0]) && ifLineContaintsAllWords(line, keyWords) && !line.contains(KeyWord.ENDING_TOKEN)) {
                        if (ifLineContainsIp4OrIp6(line, peerIpString)) {
                            lineMap.put(lineNum, line);
                        } else if (line.contains("group")) {
                            // 获取group的名称
                            String[] lineWords = line.split(" ");
                            String groupName = lineWords[lineWords.length-1];
                            String[] groupTargetWords = keyWords.clone();
                            groupTargetWords[1] = groupName;
                            // 把ref peer groupd 那行也加进来
                            lineMap.put(lineNum, line);
                            Map<Integer, String> groupLines = taint(node, groupTargetWords, filePath);
                            lineMap.putAll(groupLines);
                        }
                    }
               }
               line = reader.readLine();
               lineNum += 1;
           }
           reader.close();
       } catch (IOException e) {
           e.printStackTrace();
       }
       return lineMap;
   }

   public static boolean ifLineContaintsAllWords(String line, String[] words) {
       for (String string : words) {
           if (!line.contains(string)) {
               return false;
           }
       }
       return true;
   }

   // 获取 bgp 相应地址镞的preference值设置(单播、vpn实例)
   public static List<Integer> getBgpIpv4Preference(String node, String vpnName, String filePath) {
       BufferedReader reader;
       String startKeyWord = KeyWord.IPV4_FAMILY;
       if (vpnName.equals(KeyWord.PUBLIC_VPN_NAME)) {
           vpnName = KeyWord.UNICAST;
       }
       try {
           reader = new BufferedReader(new FileReader(filePath));
           String line = reader.readLine().trim();
           boolean ifReachTargetVpn = false;
           while (line != null && !ifReachTargetVpn) {
            if (!line.startsWith("#") && !line.startsWith("!")) {

               line = line.trim();
               if (!ifReachTargetVpn && line.startsWith(startKeyWord)) {
                   if (line.contains(vpnName)) {
                       ifReachTargetVpn = true;
                   }
               }
               if (ifReachTargetVpn && line.contains(KeyWord.PREFERENCE)) {
                   String[] words = line.split(" ");
                   if (words.length!=4) {
                       break;
                   }
                   List<Integer> prefList = new ArrayList<>();
                   prefList.add(Integer.parseInt(words[1]));
                   prefList.add(Integer.parseInt(words[2]));
                   prefList.add(Integer.parseInt(words[3]));
                   return prefList;
               }
            }
             line = reader.readLine();
           }
           reader.close();
       } catch (IOException e) {
           e.printStackTrace();
       }
       return null;
   }

   /**
    * 找到配置文件里的所有接口以及对应行
    * @param filePath 文件路径
    * @return Map的key值是接口名称，value是接口相关配置行
    */
   public static Map<String, List<ConfigurationLine>> getAllInterfaceLines(String filePath) {
       if (filePath==null) {
           return null;
       }
       Map<String, List<ConfigurationLine>> linesMap = new LinkedHashMap<>();
       BufferedReader reader;
       try {
           reader = new BufferedReader(new FileReader(filePath));
           String line = reader.readLine().trim();
           int lineNum = 1;
           while (line != null) {
            if (!line.startsWith("#") && !line.startsWith("!")) {
               if (line.startsWith(KeyWord.INTERFACE)) {
                   // 行的第二位是接口名称
                   String thisInfName = line.split(" ")[1];
                   List<ConfigurationLine> lines = new ArrayList<>();
                   // 把#前的所有行都解析成Interface
                   while(!line.contains(KeyWord.ENDING_TOKEN) && line!=null) {
                       lines.add(new ConfigurationLine(lineNum, line));
                       line = reader.readLine();
                       lineNum += 1;
                   }
                   linesMap.put(thisInfName, lines);
               }
           }
             line = reader.readLine();
             lineNum += 1;
        }
         reader.close();
       } catch (IOException e) {
           e.printStackTrace();
       }
       return linesMap;
   }

    /**
     * 根据接口ip查找对应的接口命令行
     * @param filePath 文件路径
     * @return 返回指定接口的所有配置行；若没有指定ip的接口，返回null
     */

    public static List<ConfigurationLine> getInterfaceLine(String filePath, String ifaceIp){
        Map<String, List<ConfigurationLine>> allInterfaces = getAllInterfaceLines(filePath);
        assert allInterfaces != null;
        for(String iface : allInterfaces.keySet()){
            List<ConfigurationLine> ifaceLines = allInterfaces.get(iface);
            //标记是否为要找的接口
            boolean isSpecificIface = false;
            for (ConfigurationLine l : ifaceLines) {
                if (l.getLine().contains(ifaceIp) && !l.getLine().contains(KeyWord.ENDING_TOKEN)) {
                    isSpecificIface = true;
                    break;
                }
            }
            //找到指定的接口，返回接口配置
            if(isSpecificIface){
                return ifaceLines;
            }
        }
        return null;
    }

   /**
    * 查找配置里的所有全局命令: 前后都有结束符#的行
    * @param filePath config的路径
    * @param startWithString 可指定此入参找到以该string开头的全局命令，默认为""
    *
    */
   public static List<ConfigurationLine> getGlobalConfigLines(String filePath, String startWithString) {
       if (filePath==null) {
           return null;
       }
       List<ConfigurationLine> targetLines = new ArrayList<>();
       BufferedReader reader;
       try {
           reader = new BufferedReader(new FileReader(filePath));
           String prevLine = reader.readLine().trim();
           String curLine = reader.readLine().trim();
           String nextLine = reader.readLine();
           int curLineNum = 2;
           while (curLine != null) {
            if (!curLine.startsWith("#") && !curLine.startsWith("!")) {
               if (prevLine.contains(KeyWord.ENDING_TOKEN)) {
                   // 当前行的下一行不是#则跳出
                   if (nextLine != null && nextLine.contains(KeyWord.ENDING_TOKEN)) {
                       // 当前line前后都是#
                       nextLine = nextLine.trim();
                       if (curLine.startsWith(startWithString)) {
                           targetLines.add(new ConfigurationLine(curLineNum, curLine));
                       }
                   }
               }
               prevLine = curLine;
               curLine = nextLine;
               nextLine = reader.readLine();
               curLineNum += 1;
           }
           reader.close();
        }
       } catch (IOException e) {
           e.printStackTrace();
       }
       return targetLines;
   }

//   查找peer-group名称（若有）
   @Nullable public  static String getPeerGroupName(String node, String[] keyWords, String filePath){
     Map<Integer, String> lineMap = new LinkedHashMap<>();
     // 检查关键词前两位是否是peer和ip地址, 固定index 0是peer关键字, index 1是peer ip【ipv4和ipv6都考虑】

     if (keyWords.length<2 || !keyWords[0].equals(KeyWord.BGP_PEER) || !isIpv4OrIpv6(keyWords[1])) {
       return null;
     }
     String groupName = null;

     BufferedReader reader;

     try {
       reader = new BufferedReader(new FileReader(filePath));
       String line = reader.readLine().trim();
       int lineNum = 1;
       while (line != null) {
         line = line.trim();
         if (!line.startsWith("#") && !line.startsWith("!")) {
           boolean ifThisLine = true;
           if (line.contains(keyWords[0]) && ifLineContaintsAllWords(line, keyWords) && !line.contains(KeyWord.ENDING_TOKEN)) {
              groupName = line.trim().split(" ")[3];
              break;
           }
         }
         line = reader.readLine();
         lineNum += 1;
       }
       reader.close();
     } catch (IOException e) {
       e.printStackTrace();
     }
     return groupName;
   }

}
