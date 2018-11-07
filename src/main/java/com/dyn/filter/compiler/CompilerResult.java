package com.dyn.filter.compiler;

import java.util.ArrayList;
import java.util.List;

public class CompilerResult {

    private final List<String> err;


    public CompilerResult() {
        err = new ArrayList<>();
    }

    public boolean isCompilable() {
        return err.isEmpty();
    }


    public List<String> getCompilationErrors() {
        return err;
    }


}