package htb.devarea;

import javax.jws.WebService;

/* JADX INFO: loaded from: employee-service.jar:htb/devarea/EmployeeService.class */
@WebService(name = "EmployeeService", targetNamespace = "http://devarea.htb/")
public interface EmployeeService {
    String submitReport(Report report);
}