
package com.incedo.market_data_service.controller;

import com.incedo.market_data_service.model.marketData;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@RestController
@CrossOrigin("*")
public class marketDataController {

    Random random = new Random();

    @GetMapping("/market-data")
public List<marketData> getMarketData(){

    List<marketData> marketDataList = new ArrayList<>();

    marketDataList.add(
            new marketData(
                    "Apple",
                    Math.round((210 + random.nextDouble()*15)*100.0)/100.0,
                    Math.round((-3 + random.nextDouble()*6)*100.0)/100.0
            )
    );

    marketDataList.add(
            new marketData(
                    "Microsoft",
                    Math.round((330 + random.nextDouble()*20)*100.0)/100.0,
                    Math.round((-2 + random.nextDouble()*5)*100.0)/100.0
            )
    );

    marketDataList.add(
            new marketData(
                    "NVIDIA",
                    Math.round((950 + random.nextDouble()*40)*100.0)/100.0,
                    Math.round((-4 + random.nextDouble()*8)*100.0)/100.0
            )
    );

    marketDataList.add(
            new marketData(
                    "Amazon",
                    Math.round((180 + random.nextDouble()*12)*100.0)/100.0,
                    Math.round((-2 + random.nextDouble()*5)*100.0)/100.0
            )
    );

    marketDataList.add(
            new marketData(
                    "Google",
                    Math.round((175 + random.nextDouble()*10)*100.0)/100.0,
                    Math.round((-2 + random.nextDouble()*4)*100.0)/100.0
            )
    );

    marketDataList.add(
            new marketData(
                    "Meta",
                    Math.round((500 + random.nextDouble()*25)*100.0)/100.0,
                    Math.round((-3 + random.nextDouble()*5)*100.0)/100.0
            )
    );

    marketDataList.add(
            new marketData(
                    "Tesla",
                    Math.round((180 + random.nextDouble()*15)*100.0)/100.0,
                    Math.round((-5 + random.nextDouble()*10)*100.0)/100.0
            )
    );

    marketDataList.add(
            new marketData(
                    "Netflix",
                    Math.round((610 + random.nextDouble()*25)*100.0)/100.0,
                    Math.round((-3 + random.nextDouble()*6)*100.0)/100.0
            )
    );

    marketDataList.add(
            new marketData(
                    "HDFC",
                    Math.round((1650 + random.nextDouble()*50)*100.0)/100.0,
                    Math.round((-1 + random.nextDouble()*3)*100.0)/100.0
            )
    );

    marketDataList.add(
            new marketData(
                    "Reliance",
                    Math.round((2900 + random.nextDouble()*60)*100.0)/100.0,
                    Math.round((-2 + random.nextDouble()*4)*100.0)/100.0
            )
    );

    marketDataList.add(
            new marketData(
                    "ICICI",
                    Math.round((1100 + random.nextDouble()*40)*100.0)/100.0,
                    Math.round((-2 + random.nextDouble()*4)*100.0)/100.0
            )
    );

    marketDataList.add(
            new marketData(
                    "Infosys",
                    Math.round((1500 + random.nextDouble()*35)*100.0)/100.0,
                    Math.round((-2 + random.nextDouble()*4)*100.0)/100.0
            )
    );

    marketDataList.add(
            new marketData(
                    "TCS",
                    Math.round((3900 + random.nextDouble()*70)*100.0)/100.0,
                    Math.round((-2 + random.nextDouble()*4)*100.0)/100.0
            )
    );

    marketDataList.add(
            new marketData(
                    "Wipro",
                    Math.round((520 + random.nextDouble()*20)*100.0)/100.0,
                    Math.round((-2 + random.nextDouble()*4)*100.0)/100.0
            )
    );

    marketDataList.add(
            new marketData(
                    "SBI",
                    Math.round((850 + random.nextDouble()*25)*100.0)/100.0,
                    Math.round((-2 + random.nextDouble()*5)*100.0)/100.0
            )
    );

    marketDataList.add(
            new marketData(
                    "Axis Bank",
                    Math.round((1180 + random.nextDouble()*35)*100.0)/100.0,
                    Math.round((-2 + random.nextDouble()*4)*100.0)/100.0
            )
    );

    marketDataList.add(
            new marketData(
                    "Adani Ports",
                    Math.round((1400 + random.nextDouble()*50)*100.0)/100.0,
                    Math.round((-3 + random.nextDouble()*6)*100.0)/100.0
            )
    );

    marketDataList.add(
            new marketData(
                    "Bajaj Finance",
                    Math.round((7200 + random.nextDouble()*120)*100.0)/100.0,
                    Math.round((-3 + random.nextDouble()*6)*100.0)/100.0
            )
    );

    marketDataList.add(
            new marketData(
                    "ITC",
                    Math.round((440 + random.nextDouble()*12)*100.0)/100.0,
                    Math.round((-1 + random.nextDouble()*3)*100.0)/100.0
            )
    );

    marketDataList.add(
            new marketData(
                    "Asian Paints",
                    Math.round((2900 + random.nextDouble()*60)*100.0)/100.0,
                    Math.round((-2 + random.nextDouble()*4)*100.0)/100.0
            )
    );

    return marketDataList;
}
}