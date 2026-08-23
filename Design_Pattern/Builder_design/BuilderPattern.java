class User {

    // Properties
    private String name;
    private int age;
    private String email;
    private String phone;
    private String address;

    // Private constructor
    private User(Builder builder) {

        this.name = builder.name;
        this.age = builder.age;
        this.email = builder.email;
        this.phone = builder.phone;
        this.address = builder.address;
    }

    // Getters
    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }


    // Builder class
    public static class Builder {

        private String name;
        private int age;
        private String email;
        private String phone;
        private String address;


        public Builder name(String name) {
            this.name = name;
            return this;
        }


        public Builder age(int age) {
            this.age = age;
            return this;
        }


        public Builder email(String email) {
            this.email = email;
            return this;
        }


        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }


        public Builder address(String address) {
            this.address = address;
            return this;
        }


        public User build() {
            return new User(this);
        }
    }
}

// Main / Client - Display User information
class UserDemo {
    public static void main(String[] args) {
        
        // Example 1: Complete User Profile
        System.out.println("=== User 1: Complete Profile ===");
        User user1 = new User.Builder()
                .name("John Doe")
                .age(30)
                .email("john@example.com")
                .phone("123-456-7890")
                .address("123 Main St, New York")
                .build();
        
        displayUser(user1);

        System.out.println("\n=== User 2: Partial Profile ===");
        // Example 2: Partial User Profile (only name and email)
        User user2 = new User.Builder()
                .name("Jane Smith")
                .email("jane@example.com")
                .build();
        
        displayUser(user2);

        System.out.println("\n=== User 3: Minimal Profile ===");
        // Example 3: Minimal User Profile (only name)
        User user3 = new User.Builder()
                .name("Bob Johnson")
                .build();
        
        displayUser(user3);
    }

    // Helper method to display user details
    public static void displayUser(User user) {
        System.out.println("Name: " + user.getName());
        System.out.println("Age: " + (user.getAge() == 0 ? "Not provided" : user.getAge()));
        System.out.println("Email: " + (user.getEmail() == null ? "Not provided" : user.getEmail()));
        System.out.println("Phone: " + (user.getPhone() == null ? "Not provided" : user.getPhone()));
        System.out.println("Address: " + (user.getAddress() == null ? "Not provided" : user.getAddress()));
    }
}