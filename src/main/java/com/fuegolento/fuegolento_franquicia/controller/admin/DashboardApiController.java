package com.fuegolento.fuegolento_franquicia.controller.admin;

import com.fuegolento.fuegolento_franquicia.model.Restaurant;
import com.fuegolento.fuegolento_franquicia.repository.OrderRepository;
import com.fuegolento.fuegolento_franquicia.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin/dashboard")
public class DashboardApiController {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public DashboardApiController(OrderRepository orderRepository,
                                  UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    // --------- DTOs (simple) ---------
    public record PointDto(String label, BigDecimal value) {}
    public record CountDto(String label, Long value) {}

    /**
     * Revenue per day (DELIVERED orders) for last N days.
     * Example: /api/admin/dashboard/revenue-daily?days=14
     */
    @GetMapping("/revenue-daily")
    public List<PointDto> revenueDaily(@RequestParam(defaultValue = "14") int days, HttpSession session) {

        LocalDateTime from = LocalDateTime.now().minusDays(Math.max(days, 1));
        Restaurant restaurant = (Restaurant) session.getAttribute("selectedRestaurant");
        List<OrderRepository.DailyRevenueRow> rows = (restaurant != null)
                ? orderRepository.revenueByDaySinceForRestaurant(from, restaurant)
                : orderRepository.revenueByDaySince(from);

        // Fill missing days with 0 so the chart looks continuous
        Map<LocalDate, BigDecimal> map = new HashMap<>();
        for (OrderRepository.DailyRevenueRow r : rows) {
            LocalDate day = r.getDay().toLocalDate();
            map.put(day, r.getRevenue() == null ? BigDecimal.ZERO : r.getRevenue());
        }

        List<PointDto> result = new ArrayList<>();
        LocalDate start = from.toLocalDate();
        LocalDate end = LocalDate.now();

        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            BigDecimal value = map.getOrDefault(d, BigDecimal.ZERO);
            result.add(new PointDto(d.toString(), value));
        }

        return result;
    }

    /**
     * Number of delivered orders by hour (0..23) for last N days.
     * Example: /api/admin/dashboard/orders-by-hour?days=14
     */
    @GetMapping("/orders-by-hour")
    public List<CountDto> ordersByHour(@RequestParam(defaultValue = "14") int days, HttpSession session) {

        LocalDateTime from = LocalDateTime.now().minusDays(Math.max(days, 1));
        Restaurant restaurant = (Restaurant) session.getAttribute("selectedRestaurant");
        List<OrderRepository.HourCountRow> rows = (restaurant != null)
                ? orderRepository.deliveredCountByHourSinceForRestaurant(from, restaurant)
                : orderRepository.deliveredCountByHourSince(from);

        long[] counts = new long[24];
        for (OrderRepository.HourCountRow r : rows) {
            Integer h = r.getHour();
            if (h != null && h >= 0 && h <= 23) {
                counts[h] = r.getCount() == null ? 0 : r.getCount();
            }
        }

        List<CountDto> result = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            result.add(new CountDto(String.format("%02d:00", h), counts[h]));
        }
        return result;
    }

    /**
     * User registrations per day for last N days.
     * Example: /api/admin/dashboard/users-registrations?days=40
     */
    @GetMapping("/users-registrations")
    public List<CountDto> userRegistrations(@RequestParam(defaultValue = "40") int days) {

        LocalDateTime from = LocalDateTime.now().minusDays(Math.max(days, 1));
        List<UserRepository.DailyUserCountRow> rows = userRepository.registrationsByDaySince(from);

        Map<LocalDate, Long> map = new HashMap<>();
        for (UserRepository.DailyUserCountRow r : rows) {
            LocalDate day = r.getDay().toLocalDate();
            map.put(day, r.getCount() == null ? 0L : r.getCount());
        }

        List<CountDto> result = new ArrayList<>();
        LocalDate start = from.toLocalDate();
        LocalDate end = LocalDate.now();

        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            result.add(new CountDto(d.toString(), map.getOrDefault(d, 0L)));
        }

        return result;
    }
}