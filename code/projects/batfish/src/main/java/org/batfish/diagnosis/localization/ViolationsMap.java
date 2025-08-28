package org.batfish.diagnosis.localization;

import java.util.HashMap;
import java.util.Map;

public class ViolationsMap {
  //  <flowIp, Map<node, Violation> >
  public static Map<String, Map<String, Violation>> violationsMap = new HashMap<>();
  @Override public String toString() {
    return violationsMap.toString();
  }
}
