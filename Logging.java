public enum LogLevel{
    DEBUG(1),
    INFO(2),
    ERROR(3);
    private final int value;
    LogLevel(int value){
        this.value = value;
    }
    public int getValue(){
        return value;
    }
    public boolean isGreaterOrEqual(LogLevel other){
        return this.value >= other.value;
    }
}
public class LogMessage{
     
    private final LogLevel loglevel;
    private final String message;
    private final long timeStamp;

    public LogMessage(LogLevel loglevel,String message){
        this.loglevel = loglevel;
        this.message = message;
        this.timeStamp = System.currentTimeMillis();
    }
   public LogLevel getLevel(){
    return loglevel;
   } 
   public String getMessage(){
    return message;
   }
   public long getTimeStamp(){
    return timeStamp;
   }

   public String toString(){
    return "[" + loglevel + "] " + timeStamp + " -" + message;
   } 
}

public interface LogAppender{
    void append(LogMessage logMessage);
}

public class ConsoleAppender implements LogAppender{

    @Override
    public void append(LogMessage logMessage){
        System.out.println(logMessage);
    }
}

public class FileAppender implements LogAppender{

    private final String filePath;

    public FileAppender(String filePath){
        this.filePath = filePath;
    }

    @Override
    public void append(LogMessage logMessage){
        // Implementation for appending to a file
        System.out.println("Writing to file: " + filePath + " - " + logMessage);  
    }
}

abstract class LogHandler{

    protected LogLevel logLevel;
    protected LogHandler nextHandler;
    protected LogAppender logAppender;

    public LogHandler(LogLevel logLevel,LogAppender logAppender){
        this.logLevel = logLevel;
        this.logAppender = logAppender;
    }
    
    public void setNextHandler(LogHandler nextHandler){
        this.nextHandler = nextHandler;
    }

    public void logMessage(LogLevel messageLevel,String message){
        if(this.logLevel == messageLevel){
            logAppender.append(new LogMessage(messageLevel,message));
            write(message);
        }else if(nextHandler != null){
            nextHandler.logMessage(messageLevel,message);
        }
    }
  abstract protected void write(String message);
}

class InfoLogger extends LogHandler{
    public InfoLogger(LogLevel logLevel,LogAppender logAppender){
        super(logLevel,logAppender);
    }

    protected void write(String message){
        System.out.println("INFO" + message);
    }
}

public class LoggerConfig{
    private LogLevel logLevel;
    private LogAppender logAppender;

    public LoggerConfig(LogLevel logLevel,LogAppender logAppender){
        this.logLevel = logLevel;
        this.logAppender = logAppender;
    }

    public LogLevel getLogLevel(){
        return logLevel;
    }
    public void setLogLevel(LogLevel logLevel){
        this.logLevel = logLevel;
    }
    public LogAppender getLogAppender(){
        return logAppender;
    }
    public void setLogAppender(LogAppender logAppender){
        this.logAppender = logAppender;
    }
}

public class Logger{
    private static final ConcurrentHashMap<String,Logger>loggers = new ConcurrentHashMap<>();
    private volatile LoggerConfig loggerConfig;
    private Logger(LogLevel logLevel,LogAppender logAppender){
        this.loggerConfig = new LoggerConfig(logLevel,logAppender);
    }

    public static Logger getLogger(LogLevel logLevel,LogAppender  logAppender){
        String key = logLevel.name() + "_" + logAppender.getClass().getName();
        return loggers.computeIfAbsent(key,k-> new Logger(logLevel,logAppender));
    }
}








