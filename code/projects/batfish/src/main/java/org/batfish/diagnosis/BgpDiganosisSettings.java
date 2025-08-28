package org.batfish.diagnosis;

import org.batfish.diagnosis.common.NetworkType;

public class BgpDiganosisSettings {
   public static NetworkType netType;
   /*
      repair必须同时（先）localize，localize和diagnose互斥
      diagnose localize repair
         *        F       *     --> diagnosis, no localization, no repair
         *        T       F     --> localization, no diagnosis, no repair
         *        T       T     --> localization, repair, no diagnosis     */
   public static boolean ifLocalize = false;
   public static boolean ifRepair = true;
   public static boolean ifSave = true;
}
