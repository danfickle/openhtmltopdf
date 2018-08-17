package com.openhtmltopdf.cli;

import picocli.CommandLine;

public class Main {

  public static void main(String[] args) {
    CommandLine.run(new MarkdownRenderer(), args);
  }
}
