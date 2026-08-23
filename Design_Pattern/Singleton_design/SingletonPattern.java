// Singleton Design Pattern - Eager Initialization
class DatabaseConnection {
    
    // Static instance created at class loading time
    private static final DatabaseConnection instance = new DatabaseConnection();
    
    // Private constructor to prevent instantiation
    private DatabaseConnection() {
        System.out.println("DatabaseConnection instance created");
    }
    
    // Public method to get the singleton instance
    public static DatabaseConnection getInstance() {
        return instance;
    }
    
    // Example method
    public void connect() {
        System.out.println("Connected to Database");
    }
    
    public void disconnect() {
        System.out.println("Disconnected from Database");
    }
}

// Singleton Design Pattern - Lazy Initialization
class Logger {
    
    // Static instance (not initialized)
    private static Logger instance;
    
    // Private constructor
    private Logger() {
        System.out.println("Logger instance created");
    }
    
    // Synchronized method for thread-safe lazy initialization
    public static synchronized Logger getInstance() {
        if (instance == null) {
            instance = new Logger();
        }
        return instance;
    }
    
    public void log(String message) {
        System.out.println("[LOG] " + message);
    }
}

// Singleton Design Pattern - Bill Pugh Singleton (Best Practice)
class ConfigManager {
    
    // Private constructor
    private ConfigManager() {
        System.out.println("ConfigManager instance created");
    }
    
    // Static inner helper class
    private static class SingletonHelper {
        private static final ConfigManager instance = new ConfigManager();
    }
    
    // Public method to get singleton instance
    public static ConfigManager getInstance() {
        return SingletonHelper.instance;
    }
    
    public void loadConfig() {
        System.out.println("Configuration loaded");
    }
    
    public void displayConfig() {
        System.out.println("Displaying configuration settings...");
    }
}

// Main / Client Demo
class SingletonDemo {
    public static void main(String[] args) {
        
        // Example 1: Eager Initialization - DatabaseConnection
        System.out.println("=== Eager Initialization (DatabaseConnection) ===");
        DatabaseConnection db1 = DatabaseConnection.getInstance();
        DatabaseConnection db2 = DatabaseConnection.getInstance();
        
        db1.connect();
        System.out.println("db1 and db2 are same instance: " + (db1 == db2));
        db2.disconnect();
        
        // Example 2: Lazy Initialization - Logger
        System.out.println("\n=== Lazy Initialization (Logger) ===");
        Logger logger1 = Logger.getInstance();
        Logger logger2 = Logger.getInstance();
        
        logger1.log("Application started");
        System.out.println("logger1 and logger2 are same instance: " + (logger1 == logger2));
        logger2.log("Processing data");
        
        // Example 3: Bill Pugh Singleton - ConfigManager
        System.out.println("\n=== Bill Pugh Singleton (ConfigManager) ===");
        ConfigManager config1 = ConfigManager.getInstance();
        ConfigManager config2 = ConfigManager.getInstance();
        
        config1.loadConfig();
        System.out.println("config1 and config2 are same instance: " + (config1 == config2));
        config2.displayConfig();
        
        // Example 4: Demonstrating Singleton behavior
        System.out.println("\n=== Singleton Behavior Verification ===");
        System.out.println("Only ONE instance is created for each Singleton class");
        System.out.println("Multiple calls to getInstance() return the same object");
    }
}
