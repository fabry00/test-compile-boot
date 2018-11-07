package com.dyn.filter.compiler;

import com.dyn.filter.Filter;
import com.dyn.filter.runner.FiltersLoader;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class DynamicCompiler {

  private static final String XML_ACTION = "action";
  private static final String XML_CONDITION = "condition";
  private static final String FILE_NAME_PLACE_HOLDER = "FILE_NAME_PLACE_HOLDER";
  private static final String DO_PLACE_HOLDER = "DO_PLACE_HOLDER";
  private static final String CONDITION_PLACE_HOLDER = "CONDITION_PLACE_HOLDER";
  private final Logger logger = Logger.getLogger(getClass().getSimpleName());
  private final DiagnosticListener<JavaFileObject> errorListener;

  private static class InMemoryJavaFileObject extends SimpleJavaFileObject {
    private String contents;

    public InMemoryJavaFileObject(final String className, final String contents) {
      super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
      this.contents = contents;
    }

    @Override
    public CharSequence getCharContent(final boolean ignoreEncodingErrors) throws IOException {
      return contents;
    }
  }

  public DynamicCompiler(DiagnosticListener<JavaFileObject> errorListener) {
    this.errorListener = errorListener;
  }

  public Optional<Filter> compile(final String fileName, final String input) {
    logger.log(Level.INFO, "Compiling filter: {0}", new Object[] { fileName });
    try {
      String classPath = getClassPath();
      logger.log(Level.INFO, "Classpath: {0}", new Object[] { classPath });
      final List<JavaFileObject> fObjects = new ArrayList<>();
      String code = FiltersLoader.readContentFile(FiltersLoader.SKELETON);
      Map<String, String> javaCode = getFilterCode(input);
      code = code.replace(CONDITION_PLACE_HOLDER, javaCode.get(XML_CONDITION));
      code = code.replace(DO_PLACE_HOLDER, javaCode.get(XML_ACTION));

      String className = FilenameUtils.removeExtension(fileName);
      code = code.replace(FILE_NAME_PLACE_HOLDER, className);
      try (PrintWriter out = new PrintWriter(FiltersLoader.SOURCE_OUTPUT_FOLDER + "/" + className + ".java")) {
        out.println(code);
      }

      fObjects.add(new InMemoryJavaFileObject(className, code));

      final Iterable<? extends JavaFileObject> files = fObjects;
      CompilerResult cresult = new CompilerResult();
      compile(files, classPath, cresult);
      if (cresult.isCompilable()) {
        return Optional.of(getInstance(classPath, className));
      }

      return Optional.empty();
    } catch (ParserConfigurationException | IOException | SAXException e) {
      throw new CompilationException(e);
    }
  }

  private String getClassPath() {
    StringBuilder sb = new StringBuilder();
    for (URL url : ((URLClassLoader) Thread.currentThread().getContextClassLoader()).getURLs()) {
      sb.append(url.getPath()).append(File.pathSeparator);
    }
    return sb.toString();
  }

  private Map<String, String> getFilterCode(String input)
      throws SAXException, IOException, ParserConfigurationException {
    Map<String, String> javaCode = new HashMap<>();
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    Document doc = dBuilder.parse(new InputSource(new StringReader(input)));
    doc.getDocumentElement().normalize();

    NodeList nList = doc.getElementsByTagName(XML_CONDITION);
    if (nList.item(0).getNodeType() == Node.ELEMENT_NODE) {
      Element eElement = (Element) nList.item(0);
      javaCode.put(XML_CONDITION, eElement.getTextContent().trim());
    }

    nList = doc.getElementsByTagName(XML_ACTION);
    if (nList.item(0).getNodeType() == Node.ELEMENT_NODE) {
      Element eElement = (Element) nList.item(0);
      javaCode.put(XML_ACTION, eElement.getTextContent().trim());
    }
    return javaCode;
  }

  private boolean compile(final Iterable<? extends JavaFileObject> files, String classPath,
      final CompilerResult cresult) {

    final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

    // for compilation diagnostic message processing on compilation
    final StandardJavaFileManager fileManager = compiler.getStandardFileManager(errorListener, Locale.ENGLISH,
        Charset.defaultCharset());

    final List<String> optionList = new ArrayList<>();

    optionList.addAll(Arrays.asList("-classpath", classPath));
    optionList.add("-Xlint:unchecked");
    optionList.add("-Xlint:none");
    optionList.addAll(Arrays.asList("-d", FiltersLoader.CLASS_OUTPUT_FOLDER));
    final JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, errorListener, optionList, null,
        files);
    return task.call();
  }

  public Filter getInstance(String classPath, final String className) throws CompilationException {
    final File file = new File(FiltersLoader.CLASS_OUTPUT_FOLDER);

    try {
      logger.log(Level.INFO, "Instantiating class {0}", new Object[] { className });
      URI u = file.toURI();
      URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
      Class<URLClassLoader> urlClass = URLClassLoader.class;
      Method method = urlClass.getDeclaredMethod("addURL", new Class[] { URL.class });
      method.setAccessible(true);
      method.invoke(urlClassLoader, new Object[] { u.toURL() });
      u = new File(classPath).toURI();
      logger.log(Level.INFO, "ClassPath URI {0}", new Object[] { u });
      urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
      urlClass = URLClassLoader.class;
      method = urlClass.getDeclaredMethod("addURL", new Class[] { URL.class });
      method.setAccessible(true);
      method.invoke(urlClassLoader, new Object[] { u.toURL() });

      final Class<?> thisClass = urlClassLoader.loadClass(FiltersLoader.FILTER_PACKAGE + className);
      Constructor<?> constructor = thisClass.getConstructor();
      return (Filter) constructor.newInstance();
    } catch (final InstantiationException | MalformedURLException | ClassNotFoundException | InvocationTargetException
        | NoSuchMethodException | IllegalAccessException e) {
      logger.log(Level.SEVERE, "GetInstance error", e);
      throw new CompilationException(e);
    }
  }

}
