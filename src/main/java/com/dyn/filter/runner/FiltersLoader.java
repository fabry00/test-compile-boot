package com.dyn.filter.runner;

import com.dyn.filter.Filter;
import com.dyn.filter.compiler.CompilationException;
import com.dyn.filter.compiler.DynamicCompiler;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

import org.apache.commons.io.FilenameUtils;

public class FiltersLoader {
  public static final String FOLDER_FILTERS = "filters/";
  public static final String CLASS_OUTPUT_FOLDER = "compiled/";
  public static final String SOURCE_OUTPUT_FOLDER = CLASS_OUTPUT_FOLDER + "/sources";
  public static final String SKELETON = "skeleton/skelton.java";
  public static final String FILTER_PACKAGE = "com.dyn.filter.";

  private final DiagnosticListener<JavaFileObject> errorListener;

  public FiltersLoader(DiagnosticListener<JavaFileObject> errorListener) {
    this.errorListener = errorListener;
  }

  public List<Filter> load() throws CompilationException {

    List<Filter> filters = new ArrayList<>();

    FileFilter fileFilter = (pathname) -> {
      return FilenameUtils.getExtension(pathname.getName()).equals("xml");
    };

    DynamicCompiler compiler = new DynamicCompiler(errorListener);
    File[] filtersList = new File(FOLDER_FILTERS).listFiles(fileFilter);
    for (File f : filtersList) {
      compiler.compile(f.getName(), readContentFile(f.getPath())).ifPresent(filter -> {
        filters.add(filter);
      });
    }

    if (filters.isEmpty()) {
      throw new CompilationException("No filters provided. Check Folder the" + fileFilter);
    }
    return filters;
  }

  public static String readContentFile(String path) {
    try {
      return readContentFile(new File(path));
    } catch (IOException e) {
      throw new RuntimeException();
    }
  }

  private static String readContentFile(File f) throws IOException {
    return String.join("\n", Files.readAllLines(Paths.get(f.getAbsolutePath())));
  }
}
