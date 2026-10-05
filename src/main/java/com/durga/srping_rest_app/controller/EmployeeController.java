package com.durga.srping_rest_app.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {

	
	@GetMapping
	public String getMessage() {
		return "Welcome to spring boot get mapping ";
	}
	
	@GetMapping("/status")
	public String getStatus() {
		
		return "spring boot get api is working fine ";
	}
	
}

