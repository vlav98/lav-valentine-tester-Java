package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.when;

import java.util.Date;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    private static FareCalculatorService fareCalculatorService;

    private final String registrationNumber = "ABCDEF";

    @BeforeAll
    private static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
        fareCalculatorService = new FareCalculatorService();
    }

    @BeforeEach
    private void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(registrationNumber);
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    private static void tearDown(){
        dataBasePrepareService.clearDataBaseEntries();
    }

    @Test
    public void testParkingACar(){
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService);

        Ticket expectedTicket = new Ticket();
        expectedTicket.setVehicleRegNumber(registrationNumber);
        expectedTicket.setParkingSpot(parkingService.getNextParkingNumberIfAvailable());
        expectedTicket.setPrice(0);

        // WHEN
        parkingService.processIncomingVehicle();
        var ticket = ticketDAO.getTicket(registrationNumber);

        //THEN
        assertEquals(ticket.getPrice(), expectedTicket.getPrice());
        assertEquals(ticket.getParkingSpot(), expectedTicket.getParkingSpot());
        assertEquals(ticket.getVehicleRegNumber(), expectedTicket.getVehicleRegNumber());
        var parkingSpot = parkingSpotDAO.getParkingSpot(ticket.getParkingSpot().getId());
        assertFalse(parkingSpot.isAvailable());
    }

    @Test
    public void testParkingLotExit(){
        testParkingACar();
        Date outTime = new Date();
        outTime.setTime( System.currentTimeMillis() + (  60 * 60 * 1000) );
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService);
        parkingService.processExitingVehicle(outTime);

        var ticket = ticketDAO.getTicket(registrationNumber);
        var outDate = ticket.getOutTime();
        var farePrice = ticket.getPrice();

        assertNotNull(outDate);
        assertNotEquals(0, farePrice);
    }

    @Test
    public void testParkingLotExitRecurringUser() {
        testParkingACar();
        Date outTime = new Date();
        outTime.setTime( System.currentTimeMillis() + (  60 * 60 * 1000) );
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService);
        parkingService.processExitingVehicle(outTime);

        var firstTicket = ticketDAO.getTicket(registrationNumber);

        parkingService.processIncomingVehicle();
        outTime.setTime( System.currentTimeMillis() + (  60 * 60 * 1000) );
        parkingService.processExitingVehicle(outTime);

        var receivedFare = ticketDAO.getTicket(registrationNumber);
        assertTrue(firstTicket.getPrice() > receivedFare.getPrice());
        assertNotEquals(receivedFare.getPrice(), firstTicket.getPrice());

    }

}
