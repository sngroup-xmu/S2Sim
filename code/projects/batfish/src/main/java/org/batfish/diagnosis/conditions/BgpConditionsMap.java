package org.batfish.diagnosis.conditions;

import java.util.HashMap;
import java.util.Map;

//import org.batfish.diagnosis.Diagnoser;

public class BgpConditionsMap {
  public static Map<String, Map<String, BgpCondition>> BgpConditions = new HashMap<>();

  @Override public String toString() {
    return BgpConditions.toString();
  }
}
