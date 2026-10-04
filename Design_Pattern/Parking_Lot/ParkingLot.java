

enum VehicleType{
   CAR,
   TRUCK,
   BIKE
}

class Vehicle{
    private final String plateNumber;
    private final VehicleType type;

    public Vehicle(String plateNumber,VehicleType type){
        this.plateNumber = plateNumber;
        this.type = type;
    }
    
    String getPlateNumber(){
        return plateNumber;
    }
    VehicleType getType(){
        return type;
    }
}

class ParkingSpot {
    private final String id;
    private final int floorNo;
    private final VehicleType type;
    private Vehicle parkedVehicle;

    ParkingSpot(String id,int floorNo,VehicleType type){
        this.id = id;
        this.floorNo = floorNo;
        this.type = type;
    }
    synchronized boolean tryPark(Vehicle v){
        if(parkedVehicle != null || v.getType() != type)return false;
          
          parkedVehicle =v;
          return true;
    }

    synchronized void unpark(){
        parkedVehicle = null;
    }
    synchronized boolean isFree(){
        return parkedVehicle == null;
    }

}





