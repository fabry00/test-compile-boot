package com.dyn.filter;

import com.google.gson.JsonObject;

public interface Filter {
  public String exec(JsonObject input);
}
