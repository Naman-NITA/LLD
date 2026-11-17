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
   public LogLevlel getLevel(){
    return loglevel;
   }
   public String getmessage(){
    return message;
   }
   public long gettimeStamp(){
    return timeStamp;
   }

   public String toString(){
    return "[" + level + "] " + timeStamp + " -" + message;
   } 
}






