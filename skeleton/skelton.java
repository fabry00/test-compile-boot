package com.dyn.filter;

import com.google.gson.JsonObject;

public class FILE_NAME_PLACE_HOLDER implements Filter {

  @Override
  // If I use String as input instead of JsonObject there is no error
  public String exec(JsonObject input) {
    if (CONDITION_PLACE_HOLDER) {
      DO_PLACE_HOLDER
      return tmp;
    }
    return "__NOK__";
  }
}