

import java.time.Duration; // Calculates how long a vehicle stayed in the lot.
import java.time.LocalDateTime; // Stores ticket entry time and the exit time used for billing.
import java.util.ArrayList; // Creates a mutable list of spots for each vehicle type.
import java.util.EnumMap; // Efficiently maps enum vehicle types to their spots.
import java.util.List; // Represents floors, spots, and immutable demo data.
import java.util.Map; // Stores spot groups and per-vehicle hourly rates.
import java.util.Optional; // Represents the possibility that no suitable spot exists.
import java.util.UUID; // Generates unique ticket identifiers.
import java.util.concurrent.ConcurrentHashMap; // Safely stores active tickets during concurrent access.

// Supported vehicle categories; each spot in this example accepts its matching category only.
enum VehicleType {
    CAR,
    TRUCK,
    BIKE
}

// A vehicle is the input domain object used when entering the parking lot.
class Vehicle {
    private final String plateNumber; // Identifies the vehicle for display or future lookup.
    private final VehicleType type; // Determines which spots and price apply.

    public Vehicle(String plateNumber, VehicleType type) {
        this.plateNumber = plateNumber; // Save the registration plate.
        this.type = type; // Save the vehicle category.
    }

    String getPlateNumber() { // Exposes the plate without allowing it to be changed.
        return plateNumber;
    }

    VehicleType getType() { // Exposes the category used by allocation and pricing.
        return type;
    }
}

// A parking spot owns the occupancy state and prevents two threads parking in it simultaneously.
class ParkingSpot {
    private final String id; // Unique spot label, such as 0-C1.
    private final int floorNo; // Floor where this spot is located.
    private final VehicleType type; // Vehicle category accepted by this spot.
    private Vehicle parkedVehicle; // Null means free; otherwise this is the occupying vehicle.

    ParkingSpot(String id, int floorNo, VehicleType type) {
        this.id = id; // Save the spot label.
        this.floorNo = floorNo; // Save the floor number.
        this.type = type; // Save the supported vehicle category.
    }

    synchronized boolean tryPark(Vehicle vehicle) { // Atomically check compatibility and claim this spot.
        if (parkedVehicle != null || vehicle.getType() != type) { // Reject occupied or incompatible spots.
            return false; // Tell the caller to look for another spot.
        }
        parkedVehicle = vehicle; // Claim the spot while holding this object's monitor.
        return true; // Report a successful allocation.
    }

    synchronized void unpark() { // Synchronization keeps occupancy updates consistent between threads.
        parkedVehicle = null; // Mark the spot as available again.
    }

    synchronized boolean isFree() { // Read occupancy under the same lock used for updates.
        return parkedVehicle == null; // Return whether another vehicle may try to claim this spot.
    }

    String getId() { // Exposes the label for ticket and demo output.
        return id;
    }

    int getFloorNo() { // Return the floor number using its correct integer type.
        return floorNo;
    }

    VehicleType getType() { // Exposes the category for floor indexing and allocation.
        return type;
    }
}

// A floor groups spots by vehicle category so allocation only scans relevant spaces.
class ParkingFloor {
    private final int floorNo; // Identifies this floor.
    private final Map<VehicleType, List<ParkingSpot>> spots = new EnumMap<>(VehicleType.class); // Keeps one spot list per enum category.

    ParkingFloor(int floorNo) {
        this.floorNo = floorNo; // Save this floor's number.
    }

    void addSpot(ParkingSpot spot) { // Adds a spot to the appropriate category list.
        spots.computeIfAbsent(spot.getType(), key -> new ArrayList<>()).add(spot); // Create the list if absent, then append the spot.
    }

    List<ParkingSpot> getSpots(VehicleType type) { // Returns the spots eligible for the requested vehicle.
        return spots.getOrDefault(type, List.of()); // Return an immutable empty list when this floor has none.
    }

    int getFloorNo() { // Exposes the floor number for ordering or display.
        return floorNo;
    }
}

// A ticket records the vehicle, its allocated spot, and when the stay began.
class Ticket {
    private final String id = UUID.randomUUID().toString(); // Make a unique identifier for checkout.
    private final Vehicle vehicle; // Keep the vehicle needed for validation and pricing.
    private final ParkingSpot spot; // Keep the reserved spot that must be released on exit.
    private final LocalDateTime entryTime = LocalDateTime.now(); // Capture the start time once, at ticket creation.

    Ticket(Vehicle vehicle, ParkingSpot spot) {
        this.vehicle = vehicle; // Link the ticket to its vehicle.
        this.spot = spot; // Link the ticket to its allocated spot.
    }

    String getId() { // Exposes the ticket key used at checkout.
        return id;
    }

    Vehicle getVehicle() { // Allows pricing to read the vehicle category.
        return vehicle;
    }

    ParkingSpot getSpot() { // Allows checkout to release the correct spot.
        return spot;
    }

    LocalDateTime getEntryTime() { // Allows pricing to calculate the stay duration.
        return entryTime;
    }
}

// Strategy contract: implementations decide which suitable spot should be attempted.
interface SpotAllocationStrategy {
    Optional<ParkingSpot> findSpot(List<ParkingFloor> floors, VehicleType type); // Empty means no eligible free spot was found.
}

// Selects the first available spot by floor order and then by spot insertion order.
class NearestSpotStrategy implements SpotAllocationStrategy {
    @Override // Confirms this method implements the allocation strategy contract.
    public Optional<ParkingSpot> findSpot(List<ParkingFloor> floors, VehicleType type) {
        for (ParkingFloor floor : floors) { // Visit floors in their configured order.
            for (ParkingSpot spot : floor.getSpots(type)) { // Only inspect spots that accept this vehicle category.
                if (spot.isFree()) { // Ignore spots that are already occupied.
                    return Optional.of(spot); // Return the first free candidate to the caller.
                }
            }
        }
        return Optional.empty(); // Report that no suitable free spot exists.
    }
}

// Strategy contract: pricing implementations calculate a fee for a ticket and exit time.
interface PricingStrategy {
    double calculateFee(Ticket ticket, LocalDateTime exitTime); // Makes pricing replaceable without changing ParkingLot.
}

// Charges at least one hour and rounds any partial hour up to the next hour.
class HourlyPricingStrategy implements PricingStrategy {
    private final Map<VehicleType, Double> ratePerHour = Map.of( // Define an unmodifiable rate table by vehicle category.
            VehicleType.BIKE, 10.0,
            VehicleType.CAR, 20.0,
            VehicleType.TRUCK, 40.0
    );

    @Override // Confirms this method implements the pricing strategy contract.
    public double calculateFee(Ticket ticket, LocalDateTime exitTime) {
        long minutes = Duration.between(ticket.getEntryTime(), exitTime).toMinutes(); // Measure the stay in whole minutes.
        long hours = Math.max(1, (long) Math.ceil(minutes / 60.0)); // Round up partial hours and charge at least one.
        return hours * ratePerHour.get(ticket.getVehicle().getType()); // Multiply duration by this vehicle's rate.
    }
}

// Coordinates parking and checkout while delegating allocation and pricing policies.
class ParkingLot {
    private final List<ParkingFloor> floors; // The ordered floors managed by this lot.
    private final SpotAllocationStrategy allocationStrategy; // Pluggable policy for selecting spots.
    private final PricingStrategy pricingStrategy; // Pluggable policy for calculating checkout fees.
    private final Map<String, Ticket> activeTickets = new ConcurrentHashMap<>(); // Indexes tickets that have not exited.

    ParkingLot(List<ParkingFloor> floors, SpotAllocationStrategy allocationStrategy, PricingStrategy pricingStrategy) {
        this.floors = List.copyOf(floors); // Copy caller data so the floor collection cannot be modified externally.
        this.allocationStrategy = allocationStrategy; // Save the selected allocation policy.
        this.pricingStrategy = pricingStrategy; // Save the selected pricing policy.
    }

    Ticket parkVehicle(Vehicle vehicle) { // Entry workflow: allocate a spot, occupy it, then issue a ticket.
        for (int attempt = 0; attempt < 3; attempt++) { // Retry if another thread claims the candidate first.
            Optional<ParkingSpot> candidate = allocationStrategy.findSpot(floors, vehicle.getType()); // Ask the selected policy for a candidate.
            if (candidate.isEmpty()) { // Stop immediately when no compatible free spot is visible.
                throw new IllegalStateException("Parking full for " + vehicle.getType()); // Explain why entry failed.
            }
            ParkingSpot spot = candidate.get(); // Unwrap the candidate after verifying it exists.
            if (spot.tryPark(vehicle)) { // Atomically claim it, guarding against concurrent entry.
                Ticket ticket = new Ticket(vehicle, spot); // Start the parking session and record its entry time.
                activeTickets.put(ticket.getId(), ticket); // Make the ticket available for checkout.
                return ticket; // Return the customer's proof of parking.
            }
        }
        throw new IllegalStateException("Could not allocate a spot, try again"); // Report repeated allocation contention.
    }

    double unparkVehicle(String ticketId) { // Exit workflow: find ticket, bill the stay, and release the spot.
        Ticket ticket = activeTickets.remove(ticketId); // Atomically consume the ticket so it cannot be used twice.
        if (ticket == null) { // Missing ticket means invalid or already checked out.
            throw new IllegalStateException("Invalid ticket"); // Reject the checkout request.
        }
        double fee = pricingStrategy.calculateFee(ticket, LocalDateTime.now()); // Delegate the fee calculation to its strategy.
        ticket.getSpot().unpark(); // Release the vehicle's spot for the next customer.
        return fee; // Return the amount due to the caller.
    }
}

// Package-private demo entry point; the file remains named ParkingLot.java to match the ParkingLot class file.
class Main {
    public static void main(String[] args) {
        ParkingFloor groundFloor = new ParkingFloor(0); // Create the ground floor.
        groundFloor.addSpot(new ParkingSpot("0-C1", 0, VehicleType.CAR)); // Add a car-only parking space.
        groundFloor.addSpot(new ParkingSpot("0-B1", 0, VehicleType.BIKE)); // Add a bike-only parking space.

        ParkingLot lot = new ParkingLot( // Assemble the lot with its domain data and interchangeable policies.
                List.of(groundFloor), // Configure the ordered floor list.
                new NearestSpotStrategy(), // Choose the first available compatible spot.
                new HourlyPricingStrategy() // Charge by rounded-up hour and vehicle category.
        );

        Ticket ticket = lot.parkVehicle(new Vehicle("KA01AB1234", VehicleType.CAR)); // Simulate a vehicle entering.
        System.out.println("Ticket: " + ticket.getId()); // Show the ticket used later for exit.
        System.out.println("Parked at " + ticket.getSpot().getId()); // Show the allocated location.
        System.out.println("Fee: " + lot.unparkVehicle(ticket.getId())); // Simulate checkout and print the fee.
    }
}






