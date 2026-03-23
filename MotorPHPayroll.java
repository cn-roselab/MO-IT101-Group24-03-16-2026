import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.text.NumberFormat;
import java.util.*;

public class MotorPHPayroll {

    static final String EMPLOYEE_FILE = "MotorPHemployees.csv";
    static final String ATTENDANCE_FILE = "MotorPHemployeeattendance.csv";

    static List<String> empId = new ArrayList<>();
    static List<String> fullName = new ArrayList<>();
    static List<String> birthday = new ArrayList<>();
    static List<Double> hourlyRate = new ArrayList<>();

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        if (!login(sc)) {
            System.out.println("Incorrect username and/or password.");
            return;
        }

        loadEmployees();

        System.out.print("Enter username again to determine role: ");
        String role = sc.next();

        if (role.equals("employee")) {
            employeeMenu(sc);
        } else if (role.equals("payroll_staff")) {
            payrollMenu(sc);
        }

        sc.close();
    }

    // LOGIN
    public static boolean login(Scanner sc) {

        System.out.print("Enter username: ");
        String username = sc.next();

        System.out.print("Enter password: ");
        String password = sc.next();

        return (username.equals("employee") || username.equals("payroll_staff"))
                && password.equals("12345");
    }

    // LOAD EMPLOYEES
    public static void loadEmployees() {

        try {
            BufferedReader br = new BufferedReader(new FileReader(EMPLOYEE_FILE));
            String line;
            br.readLine();

            while ((line = br.readLine()) != null) {

                String[] data = line.split(",");

                empId.add(data[0]);
                fullName.add(data[2] + " " + data[1]);
                birthday.add(data[3]);
                hourlyRate.add(Double.parseDouble(data[data.length - 1]));
            }

            br.close();

        } catch (Exception e) {
            System.out.println("Error loading employee file.");
        }
    }

    // EMPLOYEE VIEW
    public static void employeeMenu(Scanner sc) {

        System.out.print("Enter employee number: ");
        String id = sc.next();

        int index = findEmployee(id);

        if (index == -1) {
            System.out.println("Employee number does not exist.");
        } else {

            System.out.println("Employee #: " + empId.get(index));
            System.out.println("Name: " + fullName.get(index));
            System.out.println("Birthday: " + birthday.get(index));
            System.out.printf("Hourly Rate: ₱%.2f%n", hourlyRate.get(index));
        }
    }

    // PAYROLL MENU
    public static void payrollMenu(Scanner sc) {

        System.out.println("1. One employee");
        System.out.println("2. All employees");
        int option = sc.nextInt();

        System.out.print("Enter month (6-12): ");
        int month = sc.nextInt();

        printMainTitle();
        printPayPeriod(Month.of(month).toString());

        if (option == 1) {

            System.out.print("Enter employee number: ");
            String id = sc.next();

            int index = findEmployee(id);

            if (index == -1) {
                System.out.println("Employee not found.");
            } else {
                processEmployeeDetailed(index, month);
            }

        } else {

            // CUTOFF 1
            printCutoffTitle("CUTOFF 1 (1–15)");
            printHeader();

            for (int i = 0; i < empId.size(); i++) {
                processSummary(i, month, 1);
            }

            // CUTOFF 2
            printCutoffTitle("CUTOFF 2 (16–END)");
            printHeader();

            for (int i = 0; i < empId.size(); i++) {
                processSummary(i, month, 2);
            }
        }
    }

    // FIND EMPLOYEE
    public static int findEmployee(String id) {
        for (int i = 0; i < empId.size(); i++) {
            if (empId.get(i).equals(id))
                return i;
        }
        return -1;
    }

    // COMPUTE GROSS
    public static double computeGross(double hours, double rate) {
        return hours * rate;
    }

    // ONE EMPLOYEE (DETAILED VIEW)
    public static void processEmployeeDetailed(int index, int month) {

        NumberFormat php =
                NumberFormat.getCurrencyInstance(new Locale("en", "PH"));

        System.out.println("\nEmployee #: " + empId.get(index));
        System.out.println("Name: " + fullName.get(index));
        System.out.println("Birthday: " + birthday.get(index));
        System.out.println("Hourly Rate: " + php.format(hourlyRate.get(index)));

        double hours1 = computeHours(empId.get(index), month, 1, 15);
        double gross1 = computeGross(hours1, hourlyRate.get(index));

        System.out.println("\n================================");
        System.out.println("          MOTORPH PAYSLIP");
        System.out.println("================================");
        System.out.println("Period: " + Month.of(month) + " (Cutoff 1)");
        System.out.printf("Hours Worked: %.2f%n", hours1);
        System.out.println("Gross Pay: " + php.format(gross1));
        System.out.println("Net Pay: " + php.format(gross1));

        double hours2 = computeHours(empId.get(index), month, 16, 31);
        double gross2 = computeGross(hours2, hourlyRate.get(index));

        double sss = MotorPHApplyDeductions.computeSSSDeduction(gross2);
        double philhealth = MotorPHApplyDeductions.computePhilHealthDeduction(gross2);
        double pagibig = MotorPHApplyDeductions.computePagIbigDeduction(gross2);
        double tax = MotorPHApplyDeductions.computeIncomeTaxDeduction(gross2);

        double totalDeduction = sss + philhealth + pagibig + tax;
        double net2 = MotorPHApplyDeductions.computeNetPay(gross2);

        System.out.println("\n================================");
        System.out.println("          MOTORPH PAYSLIP");
        System.out.println("================================");
        System.out.println("Period: " + Month.of(month) + " (Cutoff 2)");

        System.out.printf("Hours Worked: %.2f%n", hours2);
        System.out.println("Gross Pay: " + php.format(gross2));

        System.out.println("\nDEDUCTIONS");
        System.out.println("SSS: " + php.format(sss));
        System.out.println("PhilHealth: " + php.format(philhealth));
        System.out.println("Pag-IBIG: " + php.format(pagibig));
        System.out.println("Tax: " + php.format(tax));

        System.out.println("Total Deduction: " + php.format(totalDeduction));
        System.out.println("Net Pay: " + php.format(net2));
    }

    // SUMMARY
    public static void processSummary(int index, int month, int cutoff) {

        NumberFormat php =
                NumberFormat.getCurrencyInstance(new Locale("en", "PH"));

        int start = (cutoff == 1) ? 1 : 16;
        int end = (cutoff == 1) ? 15 : 31;

        double hours = computeHours(empId.get(index), month, start, end);
        double gross = computeGross(hours, hourlyRate.get(index));

        double deduction = 0;
        double net = gross;

        if (cutoff == 2) {
            deduction = MotorPHApplyDeductions.computeSSSDeduction(gross)
                    + MotorPHApplyDeductions.computePhilHealthDeduction(gross)
                    + MotorPHApplyDeductions.computePagIbigDeduction(gross)
                    + MotorPHApplyDeductions.computeIncomeTaxDeduction(gross);

            net = MotorPHApplyDeductions.computeNetPay(gross);
        }

        System.out.printf(
                "%-8s %-30s %-12s %-10.2f %-15s %-15s %-15s%n",
                empId.get(index),
                fullName.get(index),
                birthday.get(index),
                hours,
                php.format(gross),
                php.format(deduction),
                php.format(net)
        );
    }

    // HOURS
    public static double computeHours(String id, int month, int startDay, int endDay) {

        double total = 0;

        try {
            BufferedReader br = new BufferedReader(new FileReader(ATTENDANCE_FILE));
            String line;
            br.readLine();

            DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("M/d/yyyy");
            DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("H:mm");

            while ((line = br.readLine()) != null) {

                String[] data = line.split(",");

                if (!data[0].equals(id)) continue;

                LocalDate date = LocalDate.parse(data[3], dateFormat);

                if (date.getMonthValue() != month) continue;

                int day = date.getDayOfMonth();

                if (day < startDay || day > endDay) continue;

                LocalTime in = LocalTime.parse(data[4], timeFormat);
                LocalTime out = LocalTime.parse(data[5], timeFormat);

                if (in.isBefore(LocalTime.of(8, 0)))
                    in = LocalTime.of(8, 0);

                if (out.isAfter(LocalTime.of(17, 0)))
                    out = LocalTime.of(17, 0);

                double hours = ChronoUnit.MINUTES.between(in, out) / 60.0;

                if (hours >= 5) {
                    hours -= 1;
                }

                total += Math.max(hours, 0);
            }

            br.close();

        } catch (Exception e) {
            System.out.println("Error reading attendance file.");
        }

        return total;
    }

    // UI METHODS
    public static void printMainTitle() {
        System.out.println("\n==============================================================================================================");
        System.out.println("                                          MOTORPH PAYROLL SUMMARY");
        System.out.println("==============================================================================================================\n");
    }

    public static void printPayPeriod(String month) {
        System.out.println("PAY PERIOD: " + month.toUpperCase());
    }

    public static void printCutoffTitle(String title) {
        System.out.println();
        System.out.println("           " + title);
        System.out.println();
    }

    public static void printHeader() {
        System.out.printf(
                "%-8s %-30s %-12s %-10s %-15s %-15s %-15s\n",
                "Emp #",
                "Name",
                "Birthday",
                "Hours",
                "Gross Pay",
                "Deductions",
                "Net Pay"
        );

        System.out.println("------------------------------------------------------------------------------------------------------------");
    }
}
