package com.mrtovo.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mrtovo.backend.entity.Employee.Employee;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
}
