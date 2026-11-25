import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

enum LogLevel{
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
class LogMessage{
     
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

interface LogAppender{
    void append(LogMessage logMessage);
}

class ConsoleAppender implements LogAppender{

    @Override
    public void append(LogMessage logMessage){
        System.out.println(logMessage);
    }
}

class FileAppender implements LogAppender{

    private final String filePath;

    public FileAppender(String filePath){
        this.filePath = filePath;
    }

    @Override
    public void append(LogMessage logMessage){
        // Append the complete message to the file, creating it when it does not exist.
        try (BufferedWriter writer = Files.newBufferedWriter(
                Path.of(filePath),
                java.nio.charset.StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {
            writer.write(logMessage.toString());
            writer.newLine();
        } catch (IOException exception) {
            throw new RuntimeException("Unable to write log to " + filePath, exception);
        }
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
            // The matching handler owns this severity and sends the event to the appender.
            logAppender.append(new LogMessage(messageLevel,message));
        }else if(nextHandler != null){
            // Otherwise, pass the request to the next handler in the chain.
            nextHandler.logMessage(messageLevel,message);
        }
    }
}

class InfoLogger extends LogHandler{
    public InfoLogger(LogLevel logLevel,LogAppender logAppender){
        super(logLevel,logAppender);
    }

    protected void write(String message){
        System.out.println("INFO" + message);
    }
}

class DebugLogger extends LogHandler{
    public DebugLogger(LogLevel logLevel,LogAppender logAppender){
        super(logLevel,logAppender);
    }
}

class ErrorLogger extends LogHandler{
    public ErrorLogger(LogLevel logLevel,LogAppender logAppender){
        super(logLevel,logAppender);
    }
}

class LoggerConfig{
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

class Logger{
    private static final ConcurrentHashMap<String,Logger>loggers = new ConcurrentHashMap<>();
    private volatile LoggerConfig loggerConfig;
    private LogHandler logChain;

    private Logger(LogLevel logLevel,LogAppender logAppender){
        this.loggerConfig = new LoggerConfig(logLevel,logAppender);
        // Build one chain: DEBUG -> INFO -> ERROR.
        LogHandler debug = new DebugLogger(LogLevel.DEBUG,logAppender);
        LogHandler info = new InfoLogger(LogLevel.INFO,logAppender);
        LogHandler error = new ErrorLogger(LogLevel.ERROR,logAppender);
        debug.setNextHandler(info);
        info.setNextHandler(error);
        this.logChain = debug;
    }

    public static Logger getLogger(LogLevel logLevel,LogAppender  logAppender){
        String key = logLevel.name() + "_" + logAppender.getClass().getName();
        return loggers.computeIfAbsent(key,k-> new Logger(logLevel,logAppender));
    }

    public void setLevel(LogLevel logLevel){
        loggerConfig.setLogLevel(logLevel);
    }

    public void setAppender(LogAppender logAppender){
        loggerConfig.setLogAppender(logAppender);

        // Rebuild the chain so every level sends output to the new appender.
        LogHandler debug = new DebugLogger(LogLevel.DEBUG,logAppender);
        LogHandler info = new InfoLogger(LogLevel.INFO,logAppender);
        LogHandler error = new ErrorLogger(LogLevel.ERROR,logAppender);
        debug.setNextHandler(info);
        info.setNextHandler(error);
        this.logChain = debug;
    }

    // A message is logged only when it meets the configured minimum level.
    private void log(LogLevel messageLevel,String message){
        if(messageLevel.isGreaterOrEqual(loggerConfig.getLogLevel())){
            logChain.logMessage(messageLevel,message);
        }
    }

    public void debug(String message){
        log(LogLevel.DEBUG,message);
    }

    public void info(String message){
        log(LogLevel.INFO,message);
    }

    public void error(String message){
        log(LogLevel.ERROR,message);
    }

    public static void main(String[] args){
        Logger logger = Logger.getLogger(LogLevel.DEBUG,new ConsoleAppender());
        logger.debug("Debug message");
        logger.info("Application started");
        logger.error("Application failed");
    }
}


