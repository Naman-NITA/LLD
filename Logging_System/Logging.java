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

class Logger {
    private static final ConcurrentHashMap<String, Logger> instances = new ConcurrentHashMap<>();
                                                 // static = one map shared by the whole program; stores created Loggers
    private volatile LoggerConfig config;        // FIX: volatile = every thread sees the newest config at once

    private Logger(LogLevel logLevel, LogAppender logAppender) {   // PRIVATE: outside code cannot call "new Logger"
        config = new LoggerConfig(logLevel, logAppender);          // start with the given settings
    }

    public static Logger getInstance(LogLevel logLevel, LogAppender logAppender) {  // the only door to get a Logger
        String key = logLevel.name() + "_" + logAppender.getClass().getName();      // e.g. "INFO_ConsoleAppender"
        return instances.computeIfAbsent(key, k -> new Logger(logLevel, logAppender));
                                                 // "if key missing, create + store, else return stored" (thread-safe)
    }

    public void setConfig(LoggerConfig config) {  // change settings while the program runs
        synchronized (Logger.class) {             // lock so two threads don't change at the same time
            this.config = config;                 // replace the old settings
        }
    }

    public void log(LogLevel level, String message) {          // the main method
        if (level.isGreaterOrEqual(config.getLogLevel())) {    // is this message important enough?
            LogMessage logMessage = new LogMessage(level, message);  // yes: pack it
            config.getLogAppender().append(logMessage);        // write it with the configured appender
        }                                                      // no: do nothing (filtered out)
    }

    public void debug(String message) { log(LogLevel.DEBUG, message); }  // shortcut for DEBUG
    public void info(String message)  { log(LogLevel.INFO, message); }   // shortcut for INFO
    public void error(String message) { log(LogLevel.ERROR, message); }  // shortcut for ERROR
}

class Main {
    private static LogHandler getChainOfLoggers(LogAppender appender) {  // builds the row of clerks
        LogHandler errorLogger = new ErrorLogger(LogLevel.ERROR, appender);  // last clerk
        LogHandler debugLogger = new DebugLogger(LogLevel.DEBUG, appender);  // middle clerk
        LogHandler infoLogger  = new InfoLogger(LogLevel.INFO, appender);    // first clerk
        infoLogger.setNextHandler(debugLogger);   // Info → Debug
        debugLogger.setNextHandler(errorLogger);  // Debug → Error
        return infoLogger;                       // give back the FIRST clerk (head of chain)
    }

    public static void main(String[] args) {     // program starts here
        LogAppender consoleAppender = new ConsoleAppender();     // writes to screen
        LogAppender fileAppender = new FileAppender("logs.txt"); // writes to logs.txt
        LogHandler loggerChain = getChainOfLoggers(consoleAppender);  // build chain that uses the screen

        System.out.println("Logging INFO level message:");        // heading so output is easy to read
        loggerChain.logMessage(LogLevel.INFO, "This is an information.");       // goes to InfoLogger
        System.out.println("\nLogging DEBUG level message:");     // FIX: "\n" = blank line (lost in your paste)
        loggerChain.logMessage(LogLevel.DEBUG, "This is a debug level information.");  // passes 1 clerk
        System.out.println("\nLogging ERROR level message:");
        loggerChain.logMessage(LogLevel.ERROR, "This is an error information.");       // passes 2 clerks

        System.out.println("\nUsing Singleton Logger:");
        Logger logger = Logger.getInstance(LogLevel.INFO, consoleAppender);  // get the shared Logger
        logger.setConfig(new LoggerConfig(LogLevel.INFO, fileAppender));     // switch it to write into the file
        logger.error("Using singleton Logger - Error message");              // ERROR >= INFO → written to logs.txt
    }
}