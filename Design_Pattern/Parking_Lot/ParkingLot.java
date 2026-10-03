import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/*
 * Parking Lot LLD - runnable single-file example.
 * Main domain flow: EntryGate -> ParkingLot/Floor/Spot -> Ticket -> ExitGate -> payment.
 */

enum VehicleType {
    MOTORCYCLE,
    CAR,
    TRUCK
}

enum SpotType {
    MOTORCYCLE,
    COMPACT,
    LARGE
}

enum TicketStatus {
    ACTIVE,
    PAID
}

final class Vehicle {
    private final String licensePlate;
    private final VehicleType type;

    Vehicle(String licensePlate, VehicleType type) {
        if (licensePlate == null || licensePlate.trim().isEmpty()) {
            throw new IllegalArgumentException("License plate cannot be blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("Vehicle type is required");
        }
        this.licensePlate = licensePlate.trim().toUpperCase(Locale.ROOT);
        this.type = type;
    }

    String getLicensePlate() {
        return licensePlate;
    }

    VehicleType getType() {
        return type;
    }
}

final class ParkingSpot {
    private final String id;
    private final int floorNumber;
    private final SpotType type;
    private Vehicle vehicle;

    ParkingSpot(String id, int floorNumber, SpotType type) {
        this.id = id;
        this.floorNumber = floorNumber;
        this.type = type;
    }

    synchronized boolean canFit(Vehicle candidate) {
        if (vehicle != null) {
            return false;
        }
        switch (candidate.getType()) {
            case MOTORCYCLE:
                return true;
            case CAR:
                return type == SpotType.COMPACT || type == SpotType.LARGE;
            case TRUCK:
                return type == SpotType.LARGE;
            default:
                return false;
        }
    }

    synchronized boolean park(Vehicle candidate) {
        if (!canFit(candidate)) {
            return false;
        }
        vehicle = candidate;
        return true;
    }

    synchronized void removeVehicle(Vehicle expectedVehicle) {
        if (vehicle == null || !vehicle.getLicensePlate().equals(expectedVehicle.getLicensePlate())) {
            throw new IllegalStateException("Vehicle is not parked in spot " + id);
        }
        vehicle = null;
    }

    synchronized boolean isAvailable() {
        return vehicle == null;
    }

    String getId() {
        return id;
    }

    int getFloorNumber() {
        return floorNumber;
    }

    SpotType getType() {
        return type;
    }
}

final class ParkingFloor {
    private final int floorNumber;
    private final List<ParkingSpot> spots = new ArrayList<>();

    ParkingFloor(int floorNumber, int motorcycleSpotCount, int compactSpotCount, int largeSpotCount) {
        if (floorNumber < 1 || motorcycleSpotCount < 0 || compactSpotCount < 0 || largeSpotCount < 0) {
            throw new IllegalArgumentException("Floor number must be positive and spot counts cannot be negative");
        }
        addSpots(SpotType.MOTORCYCLE, motorcycleSpotCount, "M");
        addSpots(SpotType.COMPACT, compactSpotCount, "C");
        addSpots(SpotType.LARGE, largeSpotCount, "L");
    }

    private void addSpots(SpotType type, int count, String prefix) {
        for (int i = 1; i <= count; i++) {
            spots.add(new ParkingSpot("F" + floorNumber + "-" + prefix + i, floorNumber, type));
        }
    }

    synchronized ParkingSpot parkVehicle(Vehicle vehicle) {
        for (SpotType preferredType : preferredSpotsFor(vehicle.getType())) {
            for (ParkingSpot spot : spots) {
                if (spot.getType() == preferredType && spot.park(vehicle)) {
                    return spot;
                }
            }
        }
        return null;
    }

    private List<SpotType> preferredSpotsFor(VehicleType type) {
        switch (type) {
            case MOTORCYCLE:
                return listOf(SpotType.MOTORCYCLE, SpotType.COMPACT, SpotType.LARGE);
            case CAR:
                return listOf(SpotType.COMPACT, SpotType.LARGE);
            case TRUCK:
                return listOf(SpotType.LARGE);
            default:
                throw new IllegalArgumentException("Unsupported vehicle type: " + type);
        }
    }

    private List<SpotType> listOf(SpotType first, SpotType... remaining) {
        List<SpotType> types = new ArrayList<>();
        types.add(first);
        Collections.addAll(types, remaining);
        return types;
    }

    synchronized int getAvailableSpotCount() {
        int count = 0;
        for (ParkingSpot spot : spots) {
            if (spot.isAvailable()) {
                count++;
            }
        }
        return count;
    }

    synchronized int getAvailableSpotCount(SpotType type) {
        int count = 0;
        for (ParkingSpot spot : spots) {
            if (spot.getType() == type && spot.isAvailable()) {
                count++;
            }
        }
        return count;
    }

    int getFloorNumber() {
        return floorNumber;
    }
}

final class ParkingTicket {
    private final String id;
    private final Vehicle vehicle;
    private final ParkingSpot spot;
    private final Instant entryTime;
    private TicketStatus status;
    private Instant exitTime;

    ParkingTicket(Vehicle vehicle, ParkingSpot spot, Instant entryTime) {
        this.id = UUID.randomUUID().toString();
        this.vehicle = vehicle;
        this.spot = spot;
        this.entryTime = entryTime;
        this.status = TicketStatus.ACTIVE;
    }

    void markPaid(Instant exitTime) {
        if (status != TicketStatus.ACTIVE) {
            throw new IllegalStateException("Ticket is not active");
        }
        this.exitTime = exitTime;
        this.status = TicketStatus.PAID;
    }

    String getId() {
        return id;
    }

    Vehicle getVehicle() {
        return vehicle;
    }

    ParkingSpot getSpot() {
        return spot;
    }

    Instant getEntryTime() {
        return entryTime;
    }

    TicketStatus getStatus() {
        return status;
    }

    Instant getExitTime() {
        return exitTime;
    }
}

final class ParkingReceipt {
    private final String ticketId;
    private final String licensePlate;
    private final String spotId;
    private final Instant entryTime;
    private final Instant exitTime;
    private final BigDecimal amountPaid;

    ParkingReceipt(ParkingTicket ticket, Instant exitTime, BigDecimal amountPaid) {
        this.ticketId = ticket.getId();
        this.licensePlate = ticket.getVehicle().getLicensePlate();
        this.spotId = ticket.getSpot().getId();
        this.entryTime = ticket.getEntryTime();
        this.exitTime = exitTime;
        this.amountPaid = amountPaid;
    }

    String getTicketId() {
        return ticketId;
    }

    String getLicensePlate() {
        return licensePlate;
    }

    String getSpotId() {
        return spotId;
    }

    Instant getEntryTime() {
        return entryTime;
    }

    Instant getExitTime() {
        return exitTime;
    }

    BigDecimal getAmountPaid() {
        return amountPaid;
    }
}

interface FeeCalculator {
    BigDecimal calculateFee(ParkingTicket ticket, Instant exitTime);
}

final class HourlyFeeCalculator implements FeeCalculator {
    private final Map<VehicleType, BigDecimal> hourlyRates;

    HourlyFeeCalculator(Map<VehicleType, BigDecimal> hourlyRates) {
        EnumMap<VehicleType, BigDecimal> copy = new EnumMap<>(VehicleType.class);
        copy.putAll(hourlyRates);
        for (VehicleType type : VehicleType.values()) {
            if (!copy.containsKey(type) || copy.get(type).signum() < 0) {
                throw new IllegalArgumentException("A non-negative hourly rate is required for " + type);
            }
        }
        this.hourlyRates = Collections.unmodifiableMap(copy);
    }

    static HourlyFeeCalculator defaultRates() {
        Map<VehicleType, BigDecimal> rates = new EnumMap<>(VehicleType.class);
        rates.put(VehicleType.MOTORCYCLE, new BigDecimal("2.00"));
        rates.put(VehicleType.CAR, new BigDecimal("5.00"));
        rates.put(VehicleType.TRUCK, new BigDecimal("10.00"));
        return new HourlyFeeCalculator(rates);
    }

    @Override
    public BigDecimal calculateFee(ParkingTicket ticket, Instant exitTime) {
        if (exitTime.isBefore(ticket.getEntryTime())) {
            throw new IllegalArgumentException("Exit time cannot be before entry time");
        }
        long seconds = Duration.between(ticket.getEntryTime(), exitTime).getSeconds();
        long billableHours = Math.max(1L, (seconds + 3599L) / 3600L);
        return hourlyRates.get(ticket.getVehicle().getType())
                .multiply(BigDecimal.valueOf(billableHours))
                .setScale(2, RoundingMode.HALF_UP);
    }
}

interface PaymentProcessor {
    boolean processPayment(String ticketId, BigDecimal amount);
}

final class ParkingLot {
    private final List<ParkingFloor> floors;
    private final Map<String, ParkingTicket> ticketsById = new HashMap<>();
    private final Map<String, String> activeTicketByLicensePlate = new HashMap<>();
    private final Clock clock;

    ParkingLot(List<ParkingFloor> floors) {
        this(floors, Clock.systemDefaultZone());
    }

    ParkingLot(List<ParkingFloor> floors, Clock clock) {
        if (floors == null || floors.isEmpty()) {
            throw new IllegalArgumentException("A parking lot must have at least one floor");
        }
        this.floors = new ArrayList<>(floors);
        if (clock == null) {
            throw new IllegalArgumentException("Clock is required");
        }
        this.clock = clock;
    }

    synchronized ParkingTicket parkVehicle(Vehicle vehicle) {
        if (vehicle == null) {
            throw new IllegalArgumentException("Vehicle is required");
        }
        if (activeTicketByLicensePlate.containsKey(vehicle.getLicensePlate())) {
            throw new IllegalStateException("This vehicle already has an active parking ticket");
        }

        for (ParkingFloor floor : floors) {
            ParkingSpot spot = floor.parkVehicle(vehicle);
            if (spot != null) {
                ParkingTicket ticket = new ParkingTicket(vehicle, spot, clock.instant());
                ticketsById.put(ticket.getId(), ticket);
                activeTicketByLicensePlate.put(vehicle.getLicensePlate(), ticket.getId());
                return ticket;
            }
        }
        throw new IllegalStateException("No suitable parking spot is available");
    }

    synchronized ParkingReceipt exitVehicle(
            String ticketId,
            FeeCalculator feeCalculator,
            PaymentProcessor paymentProcessor) {
        ParkingTicket ticket = ticketsById.get(ticketId);
        if (ticket == null) {
            throw new IllegalArgumentException("Ticket was not found: " + ticketId);
        }
        if (ticket.getStatus() != TicketStatus.ACTIVE) {
            throw new IllegalStateException("Ticket has already been paid and closed");
        }

        Instant exitTime = clock.instant();
        BigDecimal fee = feeCalculator.calculateFee(ticket, exitTime);
        if (!paymentProcessor.processPayment(ticket.getId(), fee)) {
            throw new IllegalStateException("Payment failed; ticket and parking spot remain active");
        }

        ticket.getSpot().removeVehicle(ticket.getVehicle());
        ticket.markPaid(exitTime);
        activeTicketByLicensePlate.remove(ticket.getVehicle().getLicensePlate());
        return new ParkingReceipt(ticket, exitTime, fee);
    }

    synchronized int getAvailableSpotCount() {
        int count = 0;
        for (ParkingFloor floor : floors) {
            count += floor.getAvailableSpotCount();
        }
        return count;
    }

    synchronized int getAvailableSpotCount(int floorNumber, SpotType type) {
        for (ParkingFloor floor : floors) {
            if (floor.getFloorNumber() == floorNumber) {
                return floor.getAvailableSpotCount(type);
            }
        }
        throw new IllegalArgumentException("Floor was not found: " + floorNumber);
    }
}

final class EntryGate {
    private final ParkingLot parkingLot;

    EntryGate(ParkingLot parkingLot) {
        this.parkingLot = parkingLot;
    }

    ParkingTicket issueTicket(Vehicle vehicle) {
        return parkingLot.parkVehicle(vehicle);
    }
}

final class ExitGate {
    private final ParkingLot parkingLot;
    private final FeeCalculator feeCalculator;
    private final PaymentProcessor paymentProcessor;

    ExitGate(ParkingLot parkingLot, FeeCalculator feeCalculator, PaymentProcessor paymentProcessor) {
        this.parkingLot = parkingLot;
        this.feeCalculator = feeCalculator;
        this.paymentProcessor = paymentProcessor;
    }

    ParkingReceipt processExit(String ticketId) {
        return parkingLot.exitVehicle(ticketId, feeCalculator, paymentProcessor);
    }
}

class ParkingLotDesign {
    public static void main(String[] args) {
        List<ParkingFloor> floors = new ArrayList<>();
        floors.add(new ParkingFloor(1, 2, 3, 1));
        floors.add(new ParkingFloor(2, 1, 2, 2));

        ParkingLot parkingLot = new ParkingLot(floors);
        EntryGate entryGate = new EntryGate(parkingLot);
        FeeCalculator feeCalculator = HourlyFeeCalculator.defaultRates();
        PaymentProcessor paymentProcessor = (ticketId, amount) -> {
            System.out.println("Payment accepted for ticket " + ticketId + ": $" + amount);
            return true;
        };
        ExitGate exitGate = new ExitGate(parkingLot, feeCalculator, paymentProcessor);

        Vehicle car = new Vehicle("ABC-123", VehicleType.CAR);
        ParkingTicket ticket = entryGate.issueTicket(car);
        System.out.println("Vehicle " + car.getLicensePlate() + " parked at "
                + ticket.getSpot().getId() + " on floor " + ticket.getSpot().getFloorNumber());
        System.out.println("Available spots: " + parkingLot.getAvailableSpotCount());

        ParkingReceipt receipt = exitGate.processExit(ticket.getId());
        System.out.println("Exit complete. Receipt: " + receipt.getTicketId()
                + ", spot " + receipt.getSpotId()
                + ", amount $" + receipt.getAmountPaid());
        System.out.println("Available spots: " + parkingLot.getAvailableSpotCount());
    }
}
