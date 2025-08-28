//package org.batfish.diagnosis.localization;
//
//import org.batfish.datamodel.Configuration;
//import org.batfish.datamodel.Interface;
//import org.batfish.datamodel.Prefix;
//
//public class LocalizationUtil {
//
//
//    public static Interface findTargetInfNameFromConfig(Prefix prefix, Configuration configuration) {
//        if (prefix!=null) {
//            for (String infName: configuration.getAllInterfaces().keySet()) {
//                Interface inf = configuration.getAllInterfaces().get(infName);
//                if (inf.getPrimaryNetwork().containsPrefix(prefix)) {
//                    return inf;
//                }
//            }
//        }
//
//        return null;
//    }
//}
