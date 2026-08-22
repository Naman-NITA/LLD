// Product class
class Computer {
    private String CPU;
    private String RAM;
    private String storage;
    private String GPU;
    private boolean hasWiFi;
    private boolean hasBluetoooth;

    // Private constructor - only Builder can call it
    private Computer(ComputerBuilder builder) {
        this.CPU = builder.CPU;
        this.RAM = builder.RAM;
        this.storage = builder.storage;
        this.GPU = builder.GPU;
        this.hasWiFi = builder.hasWiFi;
        this.hasBluetoooth = builder.hasBluetoooth;
    }

    // Display computer specs
    public void displaySpecs() {
        System.out.println("Computer Specs:");
        System.out.println("CPU: " + CPU);
        System.out.println("RAM: " + RAM);
        System.out.println("Storage: " + storage);
        System.out.println("GPU: " + GPU);
        System.out.println("WiFi: " + hasWiFi);
        System.out.println("Bluetooth: " + hasBluetoooth);
    }

    // Static Builder class
    public static class ComputerBuilder {
        private String CPU;
        private String RAM;
        private String storage;
        private String GPU;
        private boolean hasWiFi;
        private boolean hasBluetoooth;

        // Required parameters
        public ComputerBuilder(String CPU, String RAM) {
            this.CPU = CPU;
            this.RAM = RAM;
        }

        // Optional parameters
        public ComputerBuilder storage(String storage) {
            this.storage = storage;
            return this;
        }

        public ComputerBuilder GPU(String GPU) {
            this.GPU = GPU;
            return this;
        }

        public ComputerBuilder WiFi(boolean hasWiFi) {
            this.hasWiFi = hasWiFi;
            return this;
        }

        public ComputerBuilder Bluetooth(boolean hasBluetoooth) {
            this.hasBluetoooth = hasBluetoooth;
            return this;
        }

        // Build method to create Computer object
        public Computer build() {
            return new Computer(this);
        }
    }
}

// Main / Client
class BuilderDemo {
    public static void main(String[] args) {
        // Example 1: Basic Gaming Computer
        System.out.println("=== Gaming Computer ===");
        Computer gamingPC = new Computer.ComputerBuilder("Intel i9", "32GB")
                .storage("1TB SSD")
                .GPU("RTX 4090")
                .WiFi(true)
                .Bluetooth(true)
                .build();
        gamingPC.displaySpecs();

        System.out.println("\n=== Office Computer ===");
        // Example 2: Basic Office Computer
        Computer officePC = new Computer.ComputerBuilder("Intel i5", "8GB")
                .storage("256GB SSD")
                .WiFi(true)
                .build();
        officePC.displaySpecs();

        System.out.println("\n=== Minimal Computer ===");
        // Example 3: Minimal Computer with only required fields
        Computer minimalPC = new Computer.ComputerBuilder("Intel i3", "4GB")
                .build();
        minimalPC.displaySpecs();
    }
}
