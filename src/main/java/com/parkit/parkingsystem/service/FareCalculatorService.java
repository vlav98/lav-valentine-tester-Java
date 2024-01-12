package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket){
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }

        double inHour = ticket.getInTime().getTime();
        double outHour = ticket.getOutTime().getTime();
        double duration = (outHour - inHour)/(60*60*1000);

        switch (ticket.getParkingSpot().getParkingType()){
            case CAR: {
                double fare = (duration > 0.5) ? duration * Fare.CAR_RATE_PER_HOUR : 0;
                ticket.setPrice(fare);
                break;
            }
            case BIKE: {
                double fare = (duration > 0.5) ? duration * Fare.BIKE_RATE_PER_HOUR : 0;
                ticket.setPrice(fare);
                break;
            }
            default: throw new IllegalArgumentException("Unkown Parking Type");
        }
    }
}