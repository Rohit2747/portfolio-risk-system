package com.incedo.portfolioservice.controller;

import com.incedo.portfolioservice.model.portfolio;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Portfolio Service Running";
    }

    @GetMapping("/portfolios")
public List<portfolio> getPortfolios() {

    List<portfolio> portfolios = new java.util.ArrayList<>();

    for (int i = 1; i <= 100; i++) {

        double value = 50000 + (i * 2500);

        String risk;

        if (value < 100000) {
            risk = "LOW";
        } else if (value < 200000) {
            risk = "MEDIUM";
        } else {
            risk = "HIGH";
        }

        portfolios.add(
                new portfolio(
                        i,
                        "Client-" + i,
                        value,
                        risk
                )
        );
    }

    return portfolios;
}
}