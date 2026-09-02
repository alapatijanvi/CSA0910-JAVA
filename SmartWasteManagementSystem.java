import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.text.SimpleDateFormat;

/* ============================================================
   Smart Municipal Waste Collection and Route Alert System
   CSA09 - Programming in Java
   ============================================================
   Demonstrates:
   - OOP: Encapsulation, Inheritance, Polymorphism (CO1)
   - Collections Framework: ArrayList, HashSet, HashMap,
     Hashtable, Generics, Iterator/ListIterator (CO2)
   - Exception Handling: built-in + user defined (CO3)
   - Multithreading: priorities, synchronization,
     inter-thread communication via wait/notify (CO3)
   ============================================================ */

/* ------------------- CUSTOM EXCEPTIONS ------------------- */

class InvalidResidentIdException extends Exception {
    public InvalidResidentIdException(String message) { super(message); }
}

class DuplicateRequestException extends Exception {
    public DuplicateRequestException(String message) { super(message); }
}

class VehicleUnavailableException extends Exception {
    public VehicleUnavailableException(String message) { super(message); }
}

class InvalidInputException extends Exception {
    public InvalidInputException(String message) { super(message); }
}

/* ------------------- ENTITY: RESIDENT (CO1) ------------------- */

class Resident {
    private final String residentId;
    private String name;
    private String address;
    private String phone;
    private String binId;

    public Resident(String residentId, String name, String address, String phone, String binId) {
        this.residentId = residentId;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.binId = binId;
    }

    public String getResidentId() { return residentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getBinId() { return binId; }
    public void setBinId(String binId) { this.binId = binId; }

    @Override
    public String toString() {
        return String.format("Resident[ID=%s, Name=%s, Address=%s, Phone=%s, BinID=%s]",
                residentId, name, address, phone, binId);
    }
}

/* ------------------- ENTITY: BIN (CO1 - Abstract base, Inheritance) ------------------- */

abstract class Bin {
    protected final String binId;
    protected String location;
    protected double capacityLitres;
    protected double currentLevelPercent; // 0-100

    public Bin(String binId, String location, double capacityLitres, double currentLevelPercent) {
        this.binId = binId;
        this.location = location;
        this.capacityLitres = capacityLitres;
        this.currentLevelPercent = currentLevelPercent;
    }

    public String getBinId() { return binId; }
    public String getLocation() { return location; }
    public double getCurrentLevelPercent() { return currentLevelPercent; }

    public synchronized void setCurrentLevelPercent(double level) {
        this.currentLevelPercent = Math.max(0, Math.min(100, level));
    }

    // Polymorphic behaviour - differs per bin category
    public abstract int getCollectionPriority();      // lower number = higher priority
    public abstract double getOverflowThreshold();     // % level considered overflow
    public abstract String getBinType();

    public boolean isOverflowing() {
        return currentLevelPercent >= getOverflowThreshold();
    }

    @Override
    public String toString() {
        return String.format("%-10s [%-11s] Loc=%-12s Level=%5.1f%% Threshold=%.0f%% Priority=%d",
                binId, getBinType(), location, currentLevelPercent, getOverflowThreshold(), getCollectionPriority());
    }
}

class OrganicBin extends Bin {
    public OrganicBin(String binId, String location, double capacityLitres, double currentLevelPercent) {
        super(binId, location, capacityLitres, currentLevelPercent);
    }
    @Override public int getCollectionPriority() { return 1; }        // decomposes fast - highest priority
    @Override public double getOverflowThreshold() { return 75.0; }
    @Override public String getBinType() { return "ORGANIC"; }
}

class RecyclableBin extends Bin {
    public RecyclableBin(String binId, String location, double capacityLitres, double currentLevelPercent) {
        super(binId, location, capacityLitres, currentLevelPercent);
    }
    @Override public int getCollectionPriority() { return 2; }
    @Override public double getOverflowThreshold() { return 90.0; }
    @Override public String getBinType() { return "RECYCLABLE"; }
}

class HazardousBin extends Bin {
    public HazardousBin(String binId, String location, double capacityLitres, double currentLevelPercent) {
        super(binId, location, capacityLitres, currentLevelPercent);
    }
    @Override public int getCollectionPriority() { return 0; }        // most critical
    @Override public double getOverflowThreshold() { return 60.0; }
    @Override public String getBinType() { return "HAZARDOUS"; }
}

/* ------------------- ENTITY: VEHICLE (CO1 - Inheritance/Polymorphism) ------------------- */

abstract class Vehicle {
    protected final String vehicleId;
    protected String driverName;
    protected double capacityLitres;
    protected boolean available;

    public Vehicle(String vehicleId, String driverName, double capacityLitres) {
        this.vehicleId = vehicleId;
        this.driverName = driverName;
        this.capacityLitres = capacityLitres;
        this.available = true;
    }

    public String getVehicleId() { return vehicleId; }
    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public abstract String getVehicleType();
    public abstract boolean canCollect(Bin bin); // polymorphic compatibility rule

    @Override
    public String toString() {
        return String.format("%-10s [%-11s] Driver=%-10s Capacity=%.0fL Available=%s",
                vehicleId, getVehicleType(), driverName, capacityLitres, available);
    }
}

class CompactorVehicle extends Vehicle {
    public CompactorVehicle(String vehicleId, String driverName, double capacityLitres) {
        super(vehicleId, driverName, capacityLitres);
    }
    @Override public String getVehicleType() { return "COMPACTOR"; }
    @Override public boolean canCollect(Bin bin) {
        return bin instanceof OrganicBin || bin instanceof RecyclableBin;
    }
}

class RecyclingVehicle extends Vehicle {
    public RecyclingVehicle(String vehicleId, String driverName, double capacityLitres) {
        super(vehicleId, driverName, capacityLitres);
    }
    @Override public String getVehicleType() { return "RECYCLER"; }
    @Override public boolean canCollect(Bin bin) {
        return bin instanceof RecyclableBin;
    }
}

class HazardousVehicle extends Vehicle {
    public HazardousVehicle(String vehicleId, String driverName, double capacityLitres) {
        super(vehicleId, driverName, capacityLitres);
    }
    @Override public String getVehicleType() { return "HAZMAT"; }
    @Override public boolean canCollect(Bin bin) {
        return bin instanceof HazardousBin;
    }
}

/* ------------------- ENTITY: ROUTE (CO1) ------------------- */

class Route {
    private final String routeId;
    private String routeName;
    private List<String> binIds;       // ArrayList of bin ids on this route
    private String assignedVehicleId;
    private String scheduledTime;

    public Route(String routeId, String routeName, String scheduledTime) {
        this.routeId = routeId;
        this.routeName = routeName;
        this.binIds = new ArrayList<>();
        this.scheduledTime = scheduledTime;
        this.assignedVehicleId = null;
    }

    public String getRouteId() { return routeId; }
    public String getRouteName() { return routeName; }
    public List<String> getBinIds() { return binIds; }
    public void addBin(String binId) { binIds.add(binId); }
    public String getAssignedVehicleId() { return assignedVehicleId; }
    public void setAssignedVehicleId(String assignedVehicleId) { this.assignedVehicleId = assignedVehicleId; }
    public String getScheduledTime() { return scheduledTime; }

    @Override
    public String toString() {
        return String.format("%-8s %-14s Time=%-8s Vehicle=%-8s Bins=%s",
                routeId, routeName, scheduledTime,
                (assignedVehicleId == null ? "NONE" : assignedVehicleId), binIds);
    }
}

/* ------------------- ENTITY: COLLECTION REQUEST ------------------- */

class CollectionRequest {
    private final String requestId;
    private final String residentId;
    private final String binId;
    private String status; // PENDING, ASSIGNED, COMPLETED, CANCELLED, WAITLISTED
    private final String timestamp;

    public CollectionRequest(String requestId, String residentId, String binId, String status) {
        this.requestId = requestId;
        this.residentId = residentId;
        this.binId = binId;
        this.status = status;
        this.timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
    }

    public String getRequestId() { return requestId; }
    public String getResidentId() { return residentId; }
    public String getBinId() { return binId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return String.format("%-8s Resident=%-8s Bin=%-8s Status=%-11s Time=%s",
                requestId, residentId, binId, status, timestamp);
    }
}

/* ------------------- ENTITY: ALERT (CO1 - Inheritance/Polymorphism) ------------------- */

abstract class Alert {
    protected final String alertId;
    protected final String message;
    protected final String timestamp;

    public Alert(String alertId, String message) {
        this.alertId = alertId;
        this.message = message;
        this.timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
    }

    // Polymorphic - different alert categories notify differently
    public abstract void notifyAlert();

    @Override
    public String toString() {
        return "[" + timestamp + "] " + alertId + ": " + message;
    }
}

class OverflowAlert extends Alert {
    public OverflowAlert(String alertId, String message) { super(alertId, message); }
    @Override
    public void notifyAlert() {
        System.out.println(">>> [OVERFLOW ALERT] " + this);
    }
}

class DelayAlert extends Alert {
    public DelayAlert(String alertId, String message) { super(alertId, message); }
    @Override
    public void notifyAlert() {
        System.out.println(">>> [DELAY ALERT] " + this);
    }
}

/* ==========================================================
   CORE SYSTEM (CO2 - Collection Framework usage)
   ========================================================== */

class WasteManagementSystem {

    // Collections Framework usage across the system
    private List<Resident> residents = new ArrayList<>();                 // ArrayList
    private Map<String, Bin> bins = new HashMap<>();                      // HashMap
    private Map<String, Vehicle> vehicles = new HashMap<>();              // HashMap
    private Map<String, Route> routes = new HashMap<>();                  // HashMap
    private Set<String> registeredIds = new HashSet<>();                  // HashSet (uniqueness)
    private Hashtable<String, CollectionRequest> activeRequests = new Hashtable<>(); // thread-safe Hashtable
    private LinkedList<CollectionRequest> waitlist = new LinkedList<>();  // used with ListIterator

    // Shared structures for inter-thread communication
    final Object alertLock = new Object();
    private List<Alert> alertQueue = new ArrayList<>();
    private List<Alert> alertHistory = new ArrayList<>();

    private AtomicInteger requestCounter = new AtomicInteger(1000);
    private AtomicInteger alertCounter = new AtomicInteger(1);
    private int completedCollections = 0;

    /* ---------------- Registration ---------------- */

    public void registerResident(Resident r) throws InvalidInputException {
        if (r.getResidentId() == null || r.getResidentId().isEmpty())
            throw new InvalidInputException("Resident ID cannot be empty.");
        if (registeredIds.contains(r.getResidentId()))
            throw new InvalidInputException("Resident ID already exists: " + r.getResidentId());
        residents.add(r);
        registeredIds.add(r.getResidentId());
    }

    public void registerVehicle(Vehicle v) throws InvalidInputException {
        if (vehicles.containsKey(v.getVehicleId()))
            throw new InvalidInputException("Vehicle ID already exists: " + v.getVehicleId());
        vehicles.put(v.getVehicleId(), v);
    }

    public void registerBin(Bin b) throws InvalidInputException {
        if (bins.containsKey(b.getBinId()))
            throw new InvalidInputException("Bin ID already exists: " + b.getBinId());
        bins.put(b.getBinId(), b);
    }

    public void registerRoute(Route r) {
        routes.put(r.getRouteId(), r);
    }

    /* ---------------- Display ---------------- */

    public void displayRouteSchedules() {
        System.out.println("----- ROUTE SCHEDULES -----");
        for (Route r : routes.values()) System.out.println(r);
    }

    public void displayBinStatus() {
        System.out.println("----- BIN FILL STATUS -----");
        // Iterator used explicitly for traversal (CO2)
        Iterator<Map.Entry<String, Bin>> it = bins.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Bin> entry = it.next();
            Bin b = entry.getValue();
            String flag = b.isOverflowing() ? "  <-- OVERFLOW" : "";
            System.out.println(b + flag);
        }
    }

    /* ---------------- Collection Requests ---------------- */

    public CollectionRequest assignCollectionRequest(String residentId, String binId)
            throws InvalidResidentIdException, DuplicateRequestException, VehicleUnavailableException {

        if (!registeredIds.contains(residentId))
            throw new InvalidResidentIdException("No resident found with ID: " + residentId);

        // check duplicate active (non-completed/cancelled) request for same bin
        for (CollectionRequest cr : activeRequests.values()) {
            if (cr.getBinId().equals(binId) &&
                    (cr.getStatus().equals("PENDING") || cr.getStatus().equals("ASSIGNED"))) {
                throw new DuplicateRequestException("An active request already exists for Bin " + binId);
            }
        }

        Bin bin = bins.get(binId);
        if (bin == null) throw new VehicleUnavailableException("Bin not found: " + binId);

        // find a compatible, available vehicle (polymorphic canCollect check)
        Vehicle chosen = null;
        for (Vehicle v : vehicles.values()) {
            if (v.isAvailable() && v.canCollect(bin)) { chosen = v; break; }
        }

        String reqId = "REQ" + requestCounter.getAndIncrement();
        if (chosen == null) {
            // No vehicle available -> waitlist the request instead of failing hard
            CollectionRequest cr = new CollectionRequest(reqId, residentId, binId, "WAITLISTED");
            waitlist.add(cr);
            throw new VehicleUnavailableException(
                    "No available/compatible vehicle for Bin " + binId + ". Request " + reqId + " added to waitlist.");
        }

        chosen.setAvailable(false);
        CollectionRequest cr = new CollectionRequest(reqId, residentId, binId, "ASSIGNED");
        activeRequests.put(reqId, cr);
        return cr;
    }

    public void completeRequest(String requestId) throws InvalidInputException {
        CollectionRequest cr = activeRequests.get(requestId);
        if (cr == null) throw new InvalidInputException("Request not found: " + requestId);
        cr.setStatus("COMPLETED");
        completedCollections++;
        // free the vehicle serving this bin type back (simplistic release: free first unavailable compatible vehicle)
        Bin bin = bins.get(cr.getBinId());
        if (bin != null) {
            for (Vehicle v : vehicles.values()) {
                if (!v.isAvailable() && v.canCollect(bin)) { v.setAvailable(true); break; }
            }
            bin.setCurrentLevelPercent(0); // emptied
        }
    }

    public void cancelCollectionRequest(String requestId) throws InvalidInputException {
        CollectionRequest cr = activeRequests.get(requestId);
        if (cr == null) throw new InvalidInputException("No active request with ID: " + requestId);
        if (cr.getStatus().equals("COMPLETED"))
            throw new InvalidInputException("Cannot cancel a completed request.");
        cr.setStatus("CANCELLED");
        // release vehicle
        Bin bin = bins.get(cr.getBinId());
        if (bin != null) {
            for (Vehicle v : vehicles.values()) {
                if (!v.isAvailable() && v.canCollect(bin)) { v.setAvailable(true); break; }
            }
        }
        activeRequests.remove(requestId);
    }

    /* ---------------- Waitlist management using ListIterator (CO2) ---------------- */

    public void processWaitlist() {
        System.out.println("----- PROCESSING WAITLIST -----");
        if (waitlist.isEmpty()) {
            System.out.println("Waitlist is empty.");
            return;
        }
        ListIterator<CollectionRequest> lit = waitlist.listIterator();
        while (lit.hasNext()) {
            CollectionRequest cr = lit.next();
            Bin bin = bins.get(cr.getBinId());
            if (bin == null) continue;
            Vehicle chosen = null;
            for (Vehicle v : vehicles.values()) {
                if (v.isAvailable() && v.canCollect(bin)) { chosen = v; break; }
            }
            if (chosen != null) {
                chosen.setAvailable(false);
                cr.setStatus("ASSIGNED");
                activeRequests.put(cr.getRequestId(), cr);
                lit.remove(); // remove from waitlist safely via iterator
                System.out.println("Waitlisted request " + cr.getRequestId() + " assigned to vehicle " + chosen.getVehicleId());
            }
        }
    }

    public void viewWaitlist() {
        System.out.println("----- CURRENT WAITLIST (" + waitlist.size() + ") -----");
        for (CollectionRequest cr : waitlist) System.out.println(cr);
    }

    /* ---------------- Search / Update using Iterator (CO2) ---------------- */

    public Resident searchResident(String residentId) throws InvalidResidentIdException {
        Iterator<Resident> it = residents.iterator();
        while (it.hasNext()) {
            Resident r = it.next();
            if (r.getResidentId().equals(residentId)) return r;
        }
        throw new InvalidResidentIdException("Resident not found: " + residentId);
    }

    public void updateResidentPhone(String residentId, String newPhone) throws InvalidResidentIdException {
        Resident r = searchResident(residentId);
        r.setPhone(newPhone);
    }

    public Vehicle searchVehicle(String vehicleId) throws InvalidInputException {
        Vehicle v = vehicles.get(vehicleId);
        if (v == null) throw new InvalidInputException("Vehicle not found: " + vehicleId);
        return v;
    }

    /* ---------------- Alerts (shared queue, used by threads) ---------------- */

    public void raiseAlert(Alert a) {
        synchronized (alertLock) {
            alertQueue.add(a);
            alertHistory.add(a);
            alertLock.notifyAll(); // inter-thread communication
        }
    }

    public List<Alert> drainAlertQueue() {
        synchronized (alertLock) {
            List<Alert> copy = new ArrayList<>(alertQueue);
            alertQueue.clear();
            return copy;
        }
    }

    public Object getAlertLock() { return alertLock; }
    public boolean hasPendingAlerts() { synchronized (alertLock) { return !alertQueue.isEmpty(); } }

    /* ---------------- Getters for report/threads ---------------- */

    public Map<String, Bin> getBins() { return bins; }
    public Map<String, Vehicle> getVehicles() { return vehicles; }
    public List<Resident> getResidents() { return residents; }
    public Map<String, Route> getRoutes() { return routes; }
    public Hashtable<String, CollectionRequest> getActiveRequests() { return activeRequests; }
    public String nextAlertId() { return "ALT" + alertCounter.getAndIncrement(); }

    /* ---------------- Report Generation ---------------- */

    public void generateReport() {
        System.out.println("\n================= COLLECTION & ROUTE-EFFICIENCY REPORT =================");
        System.out.printf("Total Residents Registered : %d%n", residents.size());
        System.out.printf("Total Vehicles Registered  : %d%n", vehicles.size());
        System.out.printf("Total Bins Monitored       : %d%n", bins.size());
        System.out.printf("Completed Collections       : %d%n", completedCollections);

        long overflowing = bins.values().stream().filter(Bin::isOverflowing).count();
        System.out.printf("Bins Currently Overflowing  : %d%n", overflowing);

        long availableVehicles = vehicles.values().stream().filter(Vehicle::isAvailable).count();
        System.out.printf("Vehicles Available           : %d / %d%n", availableVehicles, vehicles.size());

        System.out.printf("Active Requests              : %d%n", activeRequests.size());
        System.out.printf("Waitlisted Requests          : %d%n", waitlist.size());
        System.out.printf("Total Alerts Raised          : %d%n", alertHistory.size());

        double efficiency = vehicles.isEmpty() ? 0 :
                (100.0 * (vehicles.size() - (vehicles.size() - availableVehicles)) / vehicles.size());
        System.out.printf("Fleet Utilisation             : %.1f%%%n", 100.0 - efficiency + 0.0);

        System.out.println("\n-- Alert History --");
        for (Alert a : alertHistory) System.out.println(a);
        System.out.println("===========================================================================\n");
    }
}

/* ==========================================================
   MULTITHREADING (CO3): Bin Monitor + Route Alert threads
   ========================================================== */

class BinMonitorThread extends Thread {
    private final WasteManagementSystem system;
    private volatile boolean running = true;

    public BinMonitorThread(WasteManagementSystem system) {
        super("BinMonitorThread");
        this.system = system;
        setPriority(Thread.MAX_PRIORITY); // higher priority - monitoring is critical
    }

    public void stopMonitoring() { running = false; }

    @Override
    public void run() {
        int cycles = 0;
        while (running && cycles < 4) {
            for (Bin bin : system.getBins().values()) {
                synchronized (bin) { // synchronized access to shared bin object
                    // simulate gradual fill increase
                    double newLevel = bin.getCurrentLevelPercent() + (5 + new Random().nextInt(10));
                    bin.setCurrentLevelPercent(newLevel);
                    if (bin.isOverflowing()) {
                        String id = system.nextAlertId();
                        Alert alert = new OverflowAlert(id,
                                "Bin " + bin.getBinId() + " (" + bin.getBinType() + ") at " + bin.getLocation()
                                        + " has reached " + String.format("%.1f", bin.getCurrentLevelPercent()) + "% fill.");
                        system.raiseAlert(alert); // producer -> notifies AlertThread
                    }
                }
            }
            cycles++;
            try { Thread.sleep(150); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        // signal completion
        synchronized (system.getAlertLock()) { system.getAlertLock().notifyAll(); }
    }
}

class RouteAlertThread extends Thread {
    private final WasteManagementSystem system;
    private volatile boolean running = true;
    private int idleChecks = 0;

    public RouteAlertThread(WasteManagementSystem system) {
        super("RouteAlertThread");
        this.system = system;
        setPriority(Thread.NORM_PRIORITY); // lower priority than monitor
    }

    public void stopAlerting() { running = false; }

    @Override
    public void run() {
        while (running) {
            synchronized (system.getAlertLock()) {
                while (!system.hasPendingAlerts() && running) {
                    try {
                        system.getAlertLock().wait(400); // wait for notification / inter-thread comm
                        idleChecks++;
                    } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                    if (idleChecks > 3) { running = false; break; }
                }
            }
            List<Alert> pending = system.drainAlertQueue();
            for (Alert a : pending) {
                a.notifyAlert(); // polymorphic dispatch
            }
            if (!pending.isEmpty()) idleChecks = 0;
        }
        System.out.println("[" + getName() + "] Alert monitoring cycle complete.\n");
    }
}

/* ==========================================================
   MAIN - Menu driven interface
   ========================================================== */

public class SmartWasteManagementSystem {
    private static final Scanner sc = new Scanner(System.in);
    private static final WasteManagementSystem system = new WasteManagementSystem();

    public static void main(String[] args) {
        seedDemoData();
        boolean exit = false;
        while (!exit) {
            printMenu();
            String choice = sc.nextLine().trim();
            try {
                switch (choice) {
                    case "1": registerResidentFlow(); break;
                    case "2": registerVehicleFlow(); break;
                    case "3": system.displayRouteSchedules(); system.displayBinStatus(); break;
                    case "4": assignRequestFlow(); break;
                    case "5": cancelRequestFlow(); break;
                    case "6": searchUpdateFlow(); break;
                    case "7": system.processWaitlist(); system.viewWaitlist(); break;
                    case "8": system.generateReport(); break;
                    case "9": runConcurrentSimulation(); break;
                    case "10": completeRequestFlow(); break;
                    case "0": exit = true; System.out.println("Exiting system. Goodbye!"); break;
                    default: System.out.println("Invalid option. Please choose a valid menu number.");
                }
            } catch (InvalidResidentIdException | DuplicateRequestException |
                     VehicleUnavailableException | InvalidInputException appEx) {
                System.out.println("[APPLICATION ERROR] " + appEx.getMessage());
            } catch (NumberFormatException nfe) {
                System.out.println("[INPUT ERROR] Please enter a valid number.");
            } catch (Exception e) {
                System.out.println("[UNEXPECTED ERROR] " + e.getMessage());
            }
        }
        sc.close();
    }

    private static void printMenu() {
        System.out.println("\n===== SMART MUNICIPAL WASTE COLLECTION & ROUTE ALERT SYSTEM =====");
        System.out.println("1. Register Resident");
        System.out.println("2. Register Vehicle");
        System.out.println("3. Display Route Schedule & Bin Status");
        System.out.println("4. Assign Collection Request");
        System.out.println("5. Cancel Collection Request");
        System.out.println("6. Search / Update Resident or Vehicle");
        System.out.println("7. Process & View Waitlist");
        System.out.println("8. Generate Collection & Route-Efficiency Report");
        System.out.println("9. Run Concurrent Bin-Monitoring & Alert Simulation");
        System.out.println("10. Complete a Collection Request");
        System.out.println("0. Exit");
        System.out.print("Enter choice: ");
    }

    private static void registerResidentFlow() throws InvalidInputException {
        System.out.print("Resident ID: "); String id = sc.nextLine().trim();
        System.out.print("Name: "); String name = sc.nextLine().trim();
        System.out.print("Address: "); String addr = sc.nextLine().trim();
        System.out.print("Phone: "); String phone = sc.nextLine().trim();
        System.out.print("Assigned Bin ID: "); String binId = sc.nextLine().trim();
        system.registerResident(new Resident(id, name, addr, phone, binId));
        System.out.println("Resident registered successfully.");
    }

    private static void registerVehicleFlow() throws InvalidInputException {
        System.out.print("Vehicle ID: "); String id = sc.nextLine().trim();
        System.out.print("Driver Name: "); String driver = sc.nextLine().trim();
        System.out.print("Type (1-Compactor 2-Recycler 3-Hazmat): "); String type = sc.nextLine().trim();
        System.out.print("Capacity (L): "); double cap = Double.parseDouble(sc.nextLine().trim());
        Vehicle v;
        switch (type) {
            case "2": v = new RecyclingVehicle(id, driver, cap); break;
            case "3": v = new HazardousVehicle(id, driver, cap); break;
            default:  v = new CompactorVehicle(id, driver, cap);
        }
        system.registerVehicle(v);
        System.out.println("Vehicle registered successfully.");
    }

    private static void assignRequestFlow() throws InvalidResidentIdException, DuplicateRequestException, VehicleUnavailableException {
        System.out.print("Resident ID: "); String rid = sc.nextLine().trim();
        System.out.print("Bin ID: "); String bid = sc.nextLine().trim();
        CollectionRequest cr = system.assignCollectionRequest(rid, bid);
        System.out.println("Request assigned: " + cr);
    }

    private static void completeRequestFlow() throws InvalidInputException {
        System.out.print("Request ID to mark COMPLETED: "); String rid = sc.nextLine().trim();
        system.completeRequest(rid);
        System.out.println("Request " + rid + " marked as completed.");
    }

    private static void cancelRequestFlow() throws InvalidInputException {
        System.out.print("Request ID to cancel: "); String rid = sc.nextLine().trim();
        system.cancelCollectionRequest(rid);
        System.out.println("Request " + rid + " cancelled.");
    }

    private static void searchUpdateFlow() throws InvalidResidentIdException, InvalidInputException {
        System.out.print("1-Search/Update Resident  2-Search Vehicle: ");
        String opt = sc.nextLine().trim();
        if (opt.equals("1")) {
            System.out.print("Resident ID: "); String rid = sc.nextLine().trim();
            Resident r = system.searchResident(rid);
            System.out.println("Found: " + r);
            System.out.print("Update phone number? (y/n): ");
            if (sc.nextLine().trim().equalsIgnoreCase("y")) {
                System.out.print("New phone: "); String ph = sc.nextLine().trim();
                system.updateResidentPhone(rid, ph);
                System.out.println("Updated: " + system.searchResident(rid));
            }
        } else {
            System.out.print("Vehicle ID: "); String vid = sc.nextLine().trim();
            Vehicle v = system.searchVehicle(vid);
            System.out.println("Found: " + v);
        }
    }

    private static void runConcurrentSimulation() throws InterruptedException {
        System.out.println("\n--- Starting concurrent bin-monitoring & alert simulation ---");
        BinMonitorThread monitor = new BinMonitorThread(system);
        RouteAlertThread alerter = new RouteAlertThread(system);
        alerter.start();
        monitor.start();
        monitor.join();
        alerter.join();
        System.out.println("--- Simulation complete ---");
    }

    /* Seed some demo data so the menu can be exercised immediately */
    private static void seedDemoData() {
        try {
            system.registerBin(new OrganicBin("B1", "MainSt", 500, 40));
            system.registerBin(new RecyclableBin("B2", "ParkAve", 400, 55));
            system.registerBin(new HazardousBin("B3", "IndZone", 200, 30));

            system.registerVehicle(new CompactorVehicle("V1", "Ravi", 1000));
            system.registerVehicle(new RecyclingVehicle("V2", "Suresh", 800));
            system.registerVehicle(new HazardousVehicle("V3", "Kumar", 300));

            Route r1 = new Route("R1", "NorthRoute", "08:00");
            r1.addBin("B1"); r1.addBin("B2");
            system.registerRoute(r1);
            Route r2 = new Route("R2", "IndRoute", "09:30");
            r2.addBin("B3");
            system.registerRoute(r2);

            system.registerResident(new Resident("RES1", "Anita", "12 MainSt", "9000000001", "B1"));
            system.registerResident(new Resident("RES2", "Vikram", "5 ParkAve", "9000000002", "B2"));
        } catch (InvalidInputException e) {
            System.out.println("Seed error: " + e.getMessage());
        }
    }
}
