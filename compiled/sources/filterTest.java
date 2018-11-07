package com.dyn.filter;

import java.util.Map;

public class filterTest implements Filter {

  @Override
  // If I use String as input instead of JsonObject there is no error
  public String exec(Map<String,String> input) {
    if (input.containsKey("a")) {
      String tmp = "______FILTERED_____";
      return tmp;
    }
    return "__NOK__";
  }
}
