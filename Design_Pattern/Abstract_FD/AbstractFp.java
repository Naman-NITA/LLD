// Product 1
interface Button {
    void click();
}

// Product 2
interface Checkbox {
    void check();
}


// Windows Button
class WindowsButton implements Button {

    @Override
    public void click() {
        System.out.println("Windows Button clicked");
    }
}


// Windows Checkbox
class WindowsCheckbox implements Checkbox {

    @Override
    public void check() {
        System.out.println("Windows Checkbox checked");
    }
}


// Mac Button
class MacButton implements Button {

    @Override
    public void click() {
        System.out.println("Mac Button clicked");
    }
}


// Mac Checkbox
class MacCheckbox implements Checkbox {

    @Override
    public void check() {
        System.out.println("Mac Checkbox checked");
    }
}


// Abstract Factory
interface GUIFactory {

    Button createButton();

    Checkbox createCheckbox();
}


// Windows Factory
class WindowsFactory implements GUIFactory {

    @Override
    public Button createButton() {
        return new WindowsButton();
    }

    @Override
    public Checkbox createCheckbox() {
        return new WindowsCheckbox();
    }
}


// Mac Factory
class MacFactory implements GUIFactory {

    @Override
    public Button createButton() {
        return new MacButton();
    }

    @Override
    public Checkbox createCheckbox() {
        return new MacCheckbox();
    }
}


// Main / Client
class Main {

    public static void main(String[] args) {

        // Example 1: Windows Factory
        System.out.println("=== Windows GUI ===");
        GUIFactory factory = new WindowsFactory();

        // Factory creates the objects
        Button button = factory.createButton();
        Checkbox checkbox = factory.createCheckbox();

        // Use the objects
        button.click();
        checkbox.check();

        // Example 2: Mac Factory
        System.out.println("\n=== Mac GUI ===");
        GUIFactory macFactory = new MacFactory();

        // Factory creates the objects
        Button macButton = macFactory.createButton();
        Checkbox macCheckbox = macFactory.createCheckbox();

        // Use the objects
        macButton.click();
        macCheckbox.check();
    }
}