package com.dyn.filter.compiler;

public class CompilationException extends RuntimeException {

  private static final long serialVersionUID = -7546046014455932210L;

  public CompilationException(String message) {
    super(message);
  }
  
  public CompilationException(Throwable e) {
    super(e);
  }

}
