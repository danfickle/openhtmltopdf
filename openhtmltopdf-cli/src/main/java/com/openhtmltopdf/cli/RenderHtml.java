package com.openhtmltopdf.cli;

import picocli.CommandLine;

public class RenderHtml {

  public static void main(String[] args) {
    CommandLine.run(new HtmlRenderer(), args);
  }
}
