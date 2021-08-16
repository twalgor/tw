package io.github.twalgor.log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Log {
  File logFile;
  PrintStream ps;
  String type;
  String group;
  Calendar calendar;
  
  public Log(String type, String group) {
    this(type, group, true);
  }
  
  public Log(String type, String group, boolean append) {
    File dir = new File("log/" + type);
    dir.mkdirs();
    logFile = new File(dir, group + "_" + dateString() + ".log");
    
    try {
      ps = new PrintStream(new FileOutputStream(logFile, true));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    calendar = Calendar.getInstance();

    ps.println("Log opened for " + type + ", " + group + " at " 
        + calendar.getTime());
  }
  
  String dateString() {
    Date d = new Date();
    SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yy");
    return sdf.format(d);
  }
  
  public void log(String message, boolean verbose) {
    calendar.setTimeInMillis(System.currentTimeMillis());
    String time = calendar.getTime().toString();
    String[] s = time.split(" ");
    String line = s[2] + " " + s[3] + ":" + message;
    ps.println(line);
    ps.flush();
    if (verbose) {
      System.out.println(line);
    }
  }
  
  public void close() {
     ps.close();
  }
}
