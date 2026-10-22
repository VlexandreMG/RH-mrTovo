package com.mrtovo.backend.controllers.employee;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mrtovo.backend.entity.Employee.Employee;
import com.mrtovo.backend.service.employee.EmployeeService;

@RestController
@RequestMapping("/api/employee")
@CrossOrigin(origins = "http://localhost:5173")
public class EmployeeControllers {
    @Autowired
    private EmployeeService employeeService;

    @GetMapping
    public List<Employee> getAll() {
        return employeeService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Employee> getById(@PathVariable Long id) {
        Employee employee = employeeService.getById(id);
        return employee == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(employee);
    }

    @PostMapping
    public ResponseEntity<Employee> save(@RequestBody Employee employee) {
        if (!isValid(employee)) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.save(employee));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Employee> update(
            @PathVariable Long id,
            @RequestBody Employee employee) {
        if (!isValid(employee)) {
            return ResponseEntity.badRequest().build();
        }

        Employee updatedEmployee = employeeService.update(id, employee);
        return updatedEmployee == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(updatedEmployee);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return employeeService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    private boolean isValid(Employee employee) {
        return employee != null
                && isNotBlank(employee.getNom())
                && isNotBlank(employee.getPrenom())
                && isNotBlank(employee.getEmail())
                && isNotBlank(employee.getTelephone())
                && isNotBlank(employee.getAdresse());
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}