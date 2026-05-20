package com.incedo.market_data_service.controller;

import com.incedo.market_data_service.model.marketData;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
public class marketDataController {

    @GetMapping("/market-data")
    public List<marketData> getMarketData() {

        return List.of(

                new marketData(
                        "NVIDIA",
                        920,
                        4.5
                ),

                new marketData(
                        "Apple",
                        210,
                        1.8
                ),

                new marketData(
                        "Microsoft",
                        425,
                        0.9
                ),

                new marketData(
                        "Reliance",
                        2850,
                        3.4
                ),

                new marketData(
                        "HDFC",
                        1680,
                        1.1
                ),

                new marketData(
                        "Amazon",
                        3500,
                        2.5
                )

        );
    }
}