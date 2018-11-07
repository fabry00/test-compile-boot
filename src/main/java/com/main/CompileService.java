package com.main;

import com.dyn.filter.Filter;
import com.dyn.filter.runner.FiltersLoader;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.List;
import java.util.Locale;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

import org.springframework.stereotype.Service;

@Service
public class CompileService implements DiagnosticListener<JavaFileObject> {

  public void test() {
    List<Filter> filters = new FiltersLoader(this).load();

    // EXPECTED ______FILTERED________
    JsonParser parser = new JsonParser();
    JsonObject input1 = parser.parse("{\"a\": \"OK\"}").getAsJsonObject();
    System.out.println("1: " + filters.get(0).exec(input1));

    // EXPECTED __NOK__
    JsonObject input2 = parser.parse("{\"b\": \"NOK\"}").getAsJsonObject();
    System.out.println("2: " + filters.get(0).exec(input2));
  }

  @Override
  public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
    throw new RuntimeException(diagnostic.getMessage(Locale.ENGLISH));
  }
}
