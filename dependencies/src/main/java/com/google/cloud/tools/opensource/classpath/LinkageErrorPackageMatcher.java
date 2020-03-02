package com.google.cloud.tools.opensource.classpath;


import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.google.common.annotations.VisibleForTesting;

class LinkageErrorPackageMatcher implements SymbolProblemMatcher {

  @JacksonXmlProperty(localName = "name")
  private String className;

  // For Xml parsing
  private LinkageErrorPackageMatcher() {}

  @VisibleForTesting
  LinkageErrorPackageMatcher(String className) {
    this.className = className;
  }

  @Override
  public boolean match(SymbolProblem problem, ClassFile sourceClass) {
    return true;
  }
}
