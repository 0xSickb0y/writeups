package htb.devarea;

/* JADX INFO: loaded from: employee-service.jar:htb/devarea/EmployeeServiceImpl.class */
public class EmployeeServiceImpl implements EmployeeService {
    @Override // htb.devarea.EmployeeService
    public String submitReport(Report report) {
        String str;
        if (report.isConfidential()) {
            str = "Report marked confidential. Thank you, " + report.getEmployeeName();
        } else {
            str = "Report received from " + report.getEmployeeName();
        }
        String greeting = str;
        return greeting + ". Department: " + report.getDepartment() + ". Content: " + report.getContent();
    }
}