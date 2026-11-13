package com.shakil.pulsequeue.model;

import jakarta.persistence.*;

@Entity
@Table(name = "service_counters")
public class ServiceCounter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String counterNumber; // e.g., "Counter 1", "Room 102"

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Department department;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "staff_user_id")
    private User assignedStaff;

    private boolean active = true;

    public ServiceCounter() {}

    public ServiceCounter(String counterNumber, Department department, User assignedStaff) {
        this.counterNumber = counterNumber;
        this.department = department;
        this.assignedStaff = assignedStaff;
        this.active = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCounterNumber() { return counterNumber; }
    public void setCounterNumber(String counterNumber) { this.counterNumber = counterNumber; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public User getAssignedStaff() { return assignedStaff; }
    public void setAssignedStaff(User assignedStaff) { this.assignedStaff = assignedStaff; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
