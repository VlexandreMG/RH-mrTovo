package com.mrtovo.backend.service.employee;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mrtovo.backend.entity.Employee.Employee;
import com.mrtovo.backend.repository.employee.EmployeeRepository;

@Service
public class EmployeeService {
    @Autowired
    private EmployeeRepository employeeRepository;

    public List<Employee> getAll() {
        return employeeRepository.findAll();
    }

    public Employee getById(Long id) {
        return employeeRepository.findById(id).orElse(null);
    }

    public Employee save(Employee employee) {
        return employeeRepository.save(employee);
    }

    public Employee update(Long id, Employee employee) {
        Employee existingEmployee = getById(id);
        if (existingEmployee == null) {
            return null;
        }

        existingEmployee.setNom(employee.getNom());
        existingEmployee.setPrenom(employee.getPrenom());
        existingEmployee.setEmail(employee.getEmail());
        existingEmployee.setTelephone(employee.getTelephone());
        existingEmployee.setAdresse(employee.getAdresse());
        return employeeRepository.save(existingEmployee);
    }

    public boolean delete(Long id) {
        if (!employeeRepository.existsById(id)) {
            return false;
        }

        employeeRepository.deleteById(id);
        return true;
    }
}