package htb.devarea;

/* JADX INFO: loaded from: employee-service.jar:htb/devarea/Report.class */
public class Report {
    private String employeeName;
    private String department;
    private String content;
    private boolean confidential;

    public String getEmployeeName() {
        return this.employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getDepartment() {
        return this.department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isConfidential() {
        return this.confidential;
    }

    public void setConfidential(boolean confidential) {
        this.confidential = confidential;
    }

    public String toString() {
        return "Report{employeeName='" + this.employeeName + "', department='" + this.department + "', content='" + this.content + "', confidential=" + this.confidential + '}';
    }
}