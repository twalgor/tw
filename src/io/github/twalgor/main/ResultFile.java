package io.github.twalgor.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class ResultFile {
  File file;
  PrintStream ps;
  
  public ResultFile(String path) {
    file =  new File(path);
    File dir = new File(file.getParent());
    dir.mkdirs();
    
    try {
      ps = new PrintStream(new FileOutputStream(file, false));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }
  
  public void addLine(String line) {
    if (ps == null) {
      try {
        ps = new PrintStream(new FileOutputStream(file, true));
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }
    }
    ps.println(line);
  }
  
  public void close() {
    ps.close();
    ps = null;
  }
}
