package com.fuegolento.fuegolento_franquicia.sampleData;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fuegolento.fuegolento_franquicia.enums.OrderStatus;
import com.fuegolento.fuegolento_franquicia.model.Dish;
import com.fuegolento.fuegolento_franquicia.model.Order;
import com.fuegolento.fuegolento_franquicia.model.OrderItem;
import com.fuegolento.fuegolento_franquicia.model.Restaurant;
import com.fuegolento.fuegolento_franquicia.model.User;
import com.fuegolento.fuegolento_franquicia.repository.OrderRepository;

/**
 * Seeds a full year of DELIVERED/CANCELLED order history so the dashboard
 * and downstream Data Warehouse ETL have enough volume and variety for
 * monthly/quarterly and per-restaurant analysis.
 *
 * Kept as its own Spring bean (not a private method on SampleAllData) so
 * {@code @Transactional} actually applies: SampleAllData.init() calls this
 * through the injected proxy, which is required for the transactional
 * advice to kick in (a self-invoked private method would silently skip it).
 */
@Service
public class OrderHistorySeeder {

    private static final int TOTAL_DAYS = 365;
    private static final int DELIVERED_TARGET = 1500;
    private static final double CANCELLED_RATE = 0.065; // within the requested 5-8%
    private static final int RESERVED_ORDERS_PER_RESTAURANT = 10;
    private static final long SEED = 99L;

    private final OrderRepository orderRepository;

    public OrderHistorySeeder(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public void seedHistoricalOrders(List<Restaurant> restaurants, List<User> usersPool, List<Dish> allDishes) {
        if (restaurants.isEmpty() || usersPool.isEmpty() || allDishes.isEmpty()) {
            return;
        }
        // Idempotent: only seed once, regardless of how the target volume changes later.
        if (orderRepository.countByStatus(OrderStatus.DELIVERED) > 0) {
            return;
        }

        Random r = new Random(SEED);

        int cancelledTarget = (int) Math.round(DELIVERED_TARGET * CANCELLED_RATE);
        int totalOrders = DELIVERED_TARGET + cancelledTarget;

        // Exact counts of each status, shuffled so they interleave across the timeline.
        List<OrderStatus> statusSequence = new ArrayList<>(totalOrders);
        for (int i = 0; i < DELIVERED_TARGET; i++) statusSequence.add(OrderStatus.DELIVERED);
        for (int i = 0; i < cancelledTarget; i++) statusSequence.add(OrderStatus.CANCELLED);
        Collections.shuffle(statusSequence, r);

        // Restaurant assignment: guaranteed floor per restaurant, then weighted (big/medium/small sedes) for the rest.
        List<Restaurant> restaurantAssignment = new ArrayList<>(totalOrders);
        int reserved = Math.min(totalOrders, restaurants.size() * RESERVED_ORDERS_PER_RESTAURANT);
        for (int i = 0; i < reserved; i++) {
            restaurantAssignment.add(restaurants.get(i % restaurants.size()));
        }
        double[] restaurantCumulative = cumulativeSum(buildRestaurantWeights(restaurants, r));
        for (int i = reserved; i < totalOrders; i++) {
            restaurantAssignment.add(restaurants.get(pickWeightedIndex(restaurantCumulative, r)));
        }

        double[] dishCumulative = cumulativeSum(buildDishWeights(allDishes, r));
        double[] dayCumulative = cumulativeSum(buildDayWeights(TOTAL_DAYS));

        for (int i = 0; i < totalOrders; i++) {
            OrderStatus status = statusSequence.get(i);
            Restaurant restaurant = restaurantAssignment.get(i);
            User orderUser = usersPool.get(r.nextInt(usersPool.size()));

            int daysAgo = pickWeightedIndex(dayCumulative, r);
            int hour = pickHourForOrders(r);
            int minute = r.nextInt(60);

            LocalDateTime createdAt = LocalDateTime.now()
                    .minusDays(daysAgo)
                    .withHour(hour)
                    .withMinute(minute)
                    .withSecond(0)
                    .withNano(0);

            Order order = new Order(orderUser);
            order.setStatus(status);
            order.setTableNumber(1 + r.nextInt(12));
            order.setCustomerNote(r.nextInt(10) < 2 ? "Sin cebolla, por favor." : null);
            order.setCreatedAt(createdAt);
            order.setRestaurant(restaurant);

            // 2-5 items, with quantities 1-2, drawn from a popularity-weighted product pool.
            int itemsCount = 2 + r.nextInt(4);
            for (int k = 0; k < itemsCount; k++) {
                Dish dish = allDishes.get(pickWeightedIndex(dishCumulative, r));
                int qty = 1 + (r.nextInt(10) < 3 ? 1 : 0);
                addItem(order, dish, qty);
            }

            // Business rule (see Order javadoc): total_price is only a closing snapshot for DELIVERED.
            if (status == OrderStatus.DELIVERED) {
                BigDecimal total = order.calculateTotalFromItems();
                order.setTotalPriceSnapshot(total.setScale(2, RoundingMode.HALF_UP));
            }

            orderRepository.save(order);
        }
    }

    /**
     * Assigns each restaurant to a "large" (x5), "medium" (x2.5) or "small" (x1) tier
     * so restaurant comparisons in the dashboard/DW show real variation, instead of a
     * uniform split across all sedes. Tier membership is derived from the seeded Random,
     * so it is reproducible but not tied to the order restaurants happen to be listed in.
     */
    private double[] buildRestaurantWeights(List<Restaurant> restaurants, Random rnd) {
        int n = restaurants.size();
        List<Integer> shuffledIndexes = new ArrayList<>();
        for (int i = 0; i < n; i++) shuffledIndexes.add(i);
        Collections.shuffle(shuffledIndexes, rnd);

        int largeCount = Math.max(1, n / 6);
        int mediumCount = Math.max(1, n / 3);

        double[] weights = new double[n];
        for (int rank = 0; rank < n; rank++) {
            int restaurantIndex = shuffledIndexes.get(rank);
            if (rank < largeCount) {
                weights[restaurantIndex] = 5.0;
            } else if (rank < largeCount + mediumCount) {
                weights[restaurantIndex] = 2.5;
            } else {
                weights[restaurantIndex] = 1.0;
            }
        }
        return weights;
    }

    /**
     * Assigns each dish a "best-seller" (x6), "popular" (x3) or "regular" (x1) weight
     * so product analysis shows real variation instead of every dish selling equally.
     */
    private double[] buildDishWeights(List<Dish> dishes, Random rnd) {
        int n = dishes.size();
        List<Integer> shuffledIndexes = new ArrayList<>();
        for (int i = 0; i < n; i++) shuffledIndexes.add(i);
        Collections.shuffle(shuffledIndexes, rnd);

        int bestSellerCount = Math.max(1, Math.round(n * 0.2f));
        int popularCount = Math.max(1, Math.round(n * 0.3f));

        double[] weights = new double[n];
        for (int rank = 0; rank < n; rank++) {
            int dishIndex = shuffledIndexes.get(rank);
            if (rank < bestSellerCount) {
                weights[dishIndex] = 6.0;
            } else if (rank < bestSellerCount + popularCount) {
                weights[dishIndex] = 3.0;
            } else {
                weights[dishIndex] = 1.0;
            }
        }
        return weights;
    }

    /**
     * Weight per daysAgo (0 = today, TOTAL_DAYS-1 = oldest day): heavier on
     * Friday/Saturday/Sunday, and a mild linear upward trend towards the present
     * so time-series charts show a visible growth trend, not flat noise.
     */
    private double[] buildDayWeights(int totalDays) {
        double[] weights = new double[totalDays];
        LocalDate today = LocalDate.now();
        for (int daysAgo = 0; daysAgo < totalDays; daysAgo++) {
            DayOfWeek dow = today.minusDays(daysAgo).getDayOfWeek();
            double weekdayFactor = switch (dow) {
                case FRIDAY -> 1.5;
                case SATURDAY -> 2.0;
                case SUNDAY -> 1.8;
                default -> 1.0;
            };
            double recencyFactor = 0.6 + (totalDays - 1 - daysAgo) / (double) (totalDays - 1) * 0.8;
            weights[daysAgo] = weekdayFactor * recencyFactor;
        }
        return weights;
    }

    private double[] cumulativeSum(double[] weights) {
        double[] cumulative = new double[weights.length];
        double running = 0;
        for (int i = 0; i < weights.length; i++) {
            running += weights[i];
            cumulative[i] = running;
        }
        return cumulative;
    }

    /**
     * Weighted random pick: returns an index i such that P(i) is proportional to weights[i],
     * via inverse-CDF sampling over the precomputed cumulative sums.
     */
    private int pickWeightedIndex(double[] cumulativeWeights, Random rnd) {
        double total = cumulativeWeights[cumulativeWeights.length - 1];
        double x = rnd.nextDouble() * total;
        int lo = 0, hi = cumulativeWeights.length - 1;
        while (lo < hi) {
            int mid = (lo + hi) / 2;
            if (cumulativeWeights[mid] < x) lo = mid + 1; else hi = mid;
        }
        return lo;
    }

    private int pickHourForOrders(Random rnd) {
        // Lunch / dinner peaks
        int roll = rnd.nextInt(100);
        if (roll < 50) return 13 + rnd.nextInt(3);   // 13-15
        if (roll < 90) return 20 + rnd.nextInt(3);   // 20-22
        return 17 + rnd.nextInt(3);                  // 17-19
    }

    private void addItem(Order order, Dish dish, int quantity) {
        OrderItem item = new OrderItem(dish, quantity, dish.getPrice());
        order.addItem(item);
    }
}
