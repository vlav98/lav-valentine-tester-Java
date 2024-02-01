package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

    private static ParkingService parkingService;

    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static ParkingSpotDAO parkingSpotDAO;
    @Mock
    private static TicketDAO ticketDAO;
    @Mock
    private static FareCalculatorService fareCalculatorService;

    private static final String REGISTRATION_NUMBER = "ABCDEF";

    private Ticket ticket;
    private ParkingSpot parkingSpot;

    @BeforeEach
    private void setUpPerTest() {
        try {
            lenient().when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(REGISTRATION_NUMBER);

            parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);
            ticket = new Ticket();
            ticket.setInTime(new Date(System.currentTimeMillis() - (60*60*1000)));
            ticket.setParkingSpot(parkingSpot);
            ticket.setVehicleRegNumber(REGISTRATION_NUMBER);

            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService);
        } catch (Exception e) {
            e.printStackTrace();
            throw  new RuntimeException("Failed to set up test mock objects");
        }
    }

    @Test
    public void processExitingVehicleTest(){
        //GIVEN
        when(ticketDAO.getNbTicket(REGISTRATION_NUMBER)).thenReturn(2);
        when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

        //WHEN
        parkingService.processExitingVehicle(new Date());

        //THEN
        verify(ticketDAO).getNbTicket(REGISTRATION_NUMBER);
        verify(parkingSpotDAO).updateParking(any(ParkingSpot.class));
        verify(fareCalculatorService).calculateFare(any(Ticket.class), eq(true));
    }

    @Test
    public void testProcessIncomingVehicle() {
        when(ticketDAO.getNbTicket(REGISTRATION_NUMBER)).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);
        when(ticketDAO.saveTicket(any(Ticket.class))).thenReturn(true);
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

        parkingService.processIncomingVehicle();

        verify(ticketDAO).getNbTicket(REGISTRATION_NUMBER);
        verify(ticketDAO).saveTicket(any(Ticket.class));
        verify(parkingSpotDAO).updateParking(any(ParkingSpot.class));
    }

    @Test
    public void processExitingVehicleTestUnableUpdate() {
        //GIVEN
        when(ticketDAO.getNbTicket(REGISTRATION_NUMBER)).thenReturn(1);
        when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);

        //WHEN
        parkingService.processExitingVehicle(new Date());

        //THEN
        verify(ticketDAO).getNbTicket(REGISTRATION_NUMBER);
        verify(ticketDAO).getTicket(REGISTRATION_NUMBER);
        verify(ticketDAO).updateTicket(any(Ticket.class));
        verify(parkingSpotDAO, times(0)).updateParking(parkingSpot);
    }

    @Test
    public void testGetNextParkingNumberIfAvailable() {
        // GIVEN
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);
        ParkingSpot expectedParkingSpot = new ParkingSpot(1, ParkingType.CAR, true);

        // WHEN
        ParkingSpot receivedParkingSpot = parkingService.getNextParkingNumberIfAvailable();

        // THEN
        assertEquals(expectedParkingSpot, receivedParkingSpot);
    }

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberNotFound() {
        // GIVEN
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(-1);

        //WHEN
        ParkingSpot receivedParkingSpot = parkingService.getNextParkingNumberIfAvailable();

        //THEN
        assertNull(receivedParkingSpot);
    }

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument() {
        //GIVEN
        when(inputReaderUtil.readSelection()).thenReturn(3);

        //WHEN
        ParkingSpot receivedParkingSpot = parkingService.getNextParkingNumberIfAvailable();

        //THEN
        assertNull(receivedParkingSpot);
    }
}