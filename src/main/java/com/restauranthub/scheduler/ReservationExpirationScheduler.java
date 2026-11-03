package com.restauranthub.scheduler;

import com.restauranthub.service.HallReservationService;
import com.restauranthub.service.ParkingReservationService;
import com.restauranthub.service.RoomReservationService;
import com.restauranthub.service.TableReservationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReservationExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpirationScheduler.class);

    private final TableReservationService tableReservationService;
    private final RoomReservationService roomReservationService;
    private final HallReservationService hallReservationService;
    private final ParkingReservationService parkingReservationService;

    // Runs every 5 minutes to release expired reservation holds
    @Scheduled(cron = "0 */5 * * * *")
    public void cleanupExpiredHolds() {
        log.debug("Executing scheduled cleanup of expired reservation holds...");

        int tables = tableReservationService.releaseExpiredHolds();
        int rooms = roomReservationService.releaseExpiredHolds();
        int halls = hallReservationService.releaseExpiredHolds();
        int parking = parkingReservationService.releaseExpiredHolds();

        int totalReleased = tables + rooms + halls + parking;
        if (totalReleased > 0) {
            log.info("Scheduled cleanup finished: Released {} expired temporary holds (Tables: {}, Rooms: {}, Halls: {}, Parking: {}).",
                    totalReleased, tables, rooms, halls, parking);
        }
    }
}
