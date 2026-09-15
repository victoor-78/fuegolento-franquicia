package com.fuegolento.fuegolento_franquicia.sampleData;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.sql.rowset.serial.SerialBlob;

import com.fuegolento.fuegolento_franquicia.enums.Allergen;
import com.fuegolento.fuegolento_franquicia.enums.DishCategory;
import com.fuegolento.fuegolento_franquicia.enums.OrderStatus;
import com.fuegolento.fuegolento_franquicia.model.Dish;
import com.fuegolento.fuegolento_franquicia.model.Image;
import com.fuegolento.fuegolento_franquicia.model.Order;
import com.fuegolento.fuegolento_franquicia.model.OrderItem;
import com.fuegolento.fuegolento_franquicia.model.Restaurant;
import com.fuegolento.fuegolento_franquicia.model.User;
import com.fuegolento.fuegolento_franquicia.repository.DishRepository;
import com.fuegolento.fuegolento_franquicia.repository.OrderRepository;
import com.fuegolento.fuegolento_franquicia.repository.RestaurantRepository;
import com.fuegolento.fuegolento_franquicia.repository.UserRepository;
import com.fuegolento.fuegolento_franquicia.service.StockService;

/**
 * Loads sample data into the database when the application starts.
 * Inserts users + dishes (with images as BLOB) + orders with realistic timestamps
 * for dashboard charts (daily revenue, orders per time slot, user registrations).
 */
@Service
public class SampleAllData {

    private final UserRepository userRepository;
    private final DishRepository dishRepository;
    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;
    private final PasswordEncoder passwordEncoder;
    private final StockService stockService;
    private final OrderHistorySeeder orderHistorySeeder;

    public SampleAllData(
            UserRepository userRepository,
            DishRepository dishRepository,
            OrderRepository orderRepository,
            RestaurantRepository restaurantRepository,
            PasswordEncoder passwordEncoder,
            StockService stockService,
            OrderHistorySeeder orderHistorySeeder
    ) {
        this.userRepository = userRepository;
        this.dishRepository = dishRepository;
        this.orderRepository = orderRepository;
        this.restaurantRepository = restaurantRepository;
        this.passwordEncoder = passwordEncoder;
        this.stockService = stockService;
        this.orderHistorySeeder = orderHistorySeeder;
    }

    @PostConstruct
    public void init() {

        // ---------------------------
        // RESTAURANTS
        // ---------------------------
        if (restaurantRepository.count() == 0) {
            restaurantRepository.saveAll(List.of(
                makeRestaurant("Fuego Lento Chamberí",          "Calle Luchana 14",           "Madrid",    "Madrid",    "Chamberí",    "28010", 40.4344, -3.7020, "910 000 001"),
                makeRestaurant("Fuego Lento Retiro",            "Calle Menéndez Pelayo 45",   "Madrid",    "Madrid",    "Retiro",      "28009", 40.4132, -3.6810, "910 000 002"),
                makeRestaurant("Fuego Lento Salamanca",         "Calle Serrano 72",           "Madrid",    "Madrid",    "Salamanca",   "28006", 40.4287, -3.6870, "910 000 003"),
                makeRestaurant("Fuego Lento Malasaña",          "Calle Fuencarral 98",        "Madrid",    "Madrid",    "Malasaña",    "28004", 40.4261, -3.7031, "910 000 004"),
                makeRestaurant("Fuego Lento Vallecas",          "Avenida de la Albufera 110", "Madrid",    "Madrid",    "Vallecas",    "28038", 40.3874, -3.6463, "910 000 005"),
                makeRestaurant("Fuego Lento Carabanchel",       "Calle Oporto 23",            "Madrid",    "Madrid",    "Carabanchel", "28025", 40.3868, -3.7281, "910 000 006"),
                makeRestaurant("Fuego Lento Hortaleza",         "Calle Silvano 55",           "Madrid",    "Madrid",    "Hortaleza",   "28033", 40.4760, -3.6510, "910 000 007"),
                makeRestaurant("Fuego Lento Moncloa",           "Avenida Complutense 3",      "Madrid",    "Madrid",    "Moncloa",     "28040", 40.4460, -3.7261, "910 000 008"),
                makeRestaurant("Fuego Lento Arganzuela",        "Paseo de las Delicias 60",   "Madrid",    "Madrid",    "Arganzuela",  "28045", 40.3999, -3.6985, "910 000 009"),
                makeRestaurant("Fuego Lento Tetuán",            "Calle Bravo Murillo 180",    "Madrid",    "Madrid",    "Tetuán",      "28039", 40.4617, -3.7028, "910 000 010"),
                makeRestaurant("Fuego Lento Latina",            "Calle Toledo 85",            "Madrid",    "Madrid",    "Latina",      "28005", 40.4065, -3.7194, "910 000 011"),
                makeRestaurant("Fuego Lento Vicálvaro",         "Calle Alcalá 512",           "Madrid",    "Madrid",    "Vicálvaro",   "28032", 40.4089, -3.6071, "910 000 012"),
                makeRestaurant("Fuego Lento Barajas",           "Avenida de Logroño 200",     "Madrid",    "Madrid",    "Barajas",     "28042", 40.4757, -3.5794, "910 000 013"),
                makeRestaurant("Fuego Lento Barcelona Eixample","Carrer de Balmes 87",        "Barcelona", "Barcelona", "Eixample",    "08008", 41.3940,  2.1580, "930 000 001"),
                makeRestaurant("Fuego Lento Sevilla Centro",    "Calle Sierpes 34",           "Sevilla",   "Sevilla",   "Casco Antiguo","41004",37.3896, -5.9920, "950 000 001"),
                makeRestaurant("Fuego Lento Valencia Ruzafa",   "Calle Cuba 44",              "Valencia",  "Valencia",  "Ruzafa",      "46006", 39.4617, -0.3780, "960 000 001"),
                makeRestaurant("Fuego Lento Bilbao Indautxu",   "Alameda Recalde 19",         "Bilbao",    "Bizkaia",   "Indautxu",    "48009", 43.2620, -2.9385, "940 000 001"),
                makeRestaurant("Fuego Lento Zaragoza Centro",   "Calle Alfonso I 23",         "Zaragoza",  "Zaragoza",  "Centro",      "50003", 41.6548, -0.8791, "970 000 001")
            ));
        }

        List<Restaurant> restaurants = restaurantRepository.findAll();
        Random random = new Random();

        // ---------------------------
        // USERS (ensure base users + create many clients + fix null createdAt)
        // ---------------------------

        // 1) Fix existing users with null createdAt (in case they were created before adding the field)
        List<User> existing = userRepository.findAll();
        boolean updatedAny = false;
        for (User u : existing) {
            if (u.getCreatedAt() == null) {
                // Put a reasonable default (now - random days) so charts look realistic
                u.setCreatedAt(LocalDateTime.now().minusDays(new Random().nextInt(30) + 1));
                updatedAny = true;
            }
        }
        if (updatedAny) {
            userRepository.saveAll(existing);
        }

        // 2) Ensure base accounts exist (upsert-style)
        User user = ensureUser(
                "user",
                "user@fuegolento.com",
                LocalDate.of(2000, 5, 10),
                "user123",
                false,
                LocalDateTime.now().minusDays(25).withHour(12).withMinute(15),
                "USER"
        );

        User admin = ensureUser(
                "admin",
                "admin@fuegolento.com",
                LocalDate.of(1995, 1, 1),
                "admin123",
                false,
                LocalDateTime.now().minusDays(60).withHour(10).withMinute(0),
                "USER", "ADMIN"
        );

        User maria = ensureUser(
                "maria",
                "maria@fuegolento.com",
                LocalDate.of(2001, 2, 18),
                "maria123",
                false,
                LocalDateTime.now().minusDays(18).withHour(18).withMinute(30),
                "USER"
        );

        User juan = ensureUser(
                "juan",
                "juan@fuegolento.com",
                LocalDate.of(1999, 9, 7),
                "juan123",
                false,
                LocalDateTime.now().minusDays(10).withHour(21).withMinute(5),
                "USER"
        );

        User lucia = ensureUser(
                "lucia",
                "lucia@fuegolento.com",
                LocalDate.of(2002, 11, 25),
                "lucia123",
                true, // banned
                LocalDateTime.now().minusDays(5).withHour(9).withMinute(40),
                "USER"
        );

        // 3) Create many extra clients for "registrations over time" chart
        // Only create them if they don't exist already (safe to run multiple times).
        Random rnd = new Random(42);
        int clientsToCreate = 35;

        for (int i = 1; i <= clientsToCreate; i++) {
            String username = String.format("client%02d", i);
            String email = username + "@fuegolento.com";

            if (userRepository.findByUsername(username).isPresent()) {
                continue;
            }

            // Spread registrations across the last ~40 days, with realistic hours
            int daysAgo = 1 + rnd.nextInt(40);
            int hour = pickHourForRegistrations(rnd); // more likely evening
            int minute = rnd.nextInt(60);

            User u = new User(
                    username,
                    email,
                    LocalDate.of(1990 + rnd.nextInt(15), 1 + rnd.nextInt(12), 1 + rnd.nextInt(28)),
                    passwordEncoder.encode("client123"),
                    "USER"
            );
            u.setCreatedAt(LocalDateTime.now().minusDays(daysAgo).withHour(hour).withMinute(minute).withSecond(0).withNano(0));
            userRepository.save(u);
        }

        // ---------------------------
        // DISHES + IMAGES (BLOB)
        // ---------------------------
        if (dishRepository.count() == 0) {

            // ===== Existing dishes =====

            Dish chuleton = new Dish(
                    DishCategory.MEAT,
                    "Chuletón a la piedra",
                    "Corte premium servido con piedra caliente y guarnición.",
                    List.of(),
                    new BigDecimal("24.90"),
                    true
            );
            setImageIfExists(chuleton, "static/img/dishes/chuleton-piedra.png");

            Dish costillar = new Dish(
                    DishCategory.MEAT,
                    "Costillar a la brasa",
                    "Cocción lenta, glaseado suave y sabor ahumado.",
                    List.of(),
                    new BigDecimal("19.50"),
                    true
            );
            setImageIfExists(costillar, "static/img/dishes/costillar-brasa.png");

            Dish croquetas = new Dish(
                    DishCategory.STARTER,
                    "Croquetas caseras",
                    "Cremosas, crujientes y recién hechas.",
                    List.of(Allergen.GLUTEN, Allergen.MILK, Allergen.EGG),
                    new BigDecimal("7.50"),
                    true
            );
            setImageIfExists(croquetas, "static/img/dishes/croquetas.png");

            Dish tartaQueso = new Dish(
                    DishCategory.DESSERT,
                    "Tarta de queso",
                    "Horneada, cremosa y con base de galleta.",
                    List.of(Allergen.MILK, Allergen.EGG, Allergen.GLUTEN),
                    new BigDecimal("6.50"),
                    true
            );
            setImageIfExists(tartaQueso, "static/img/dishes/tarta-queso.png");

            Dish entrecot = new Dish(
                    DishCategory.MEAT,
                    "Entrecot a la piedra",
                    "Carne tierna para terminar al punto en mesa.",
                    List.of(),
                    new BigDecimal("18.90"),
                    true
            );
            setImageIfExists(entrecot, "static/img/dishes/entrecot-piedra.png");

            Dish secreto = new Dish(
                    DishCategory.MEAT,
                    "Secreto ibérico a la brasa",
                    "Jugoso y dorado al carbón, con toque de sal en escamas.",
                    List.of(),
                    new BigDecimal("16.90"),
                    true
            );
            setImageIfExists(secreto, "static/img/dishes/secreto-iberico.png");

            Dish ensalada = new Dish(
                    DishCategory.STARTER,
                    "Ensalada de temporada",
                    "Fresca, ligera y perfecta para compartir.",
                    List.of(),
                    new BigDecimal("6.90"),
                    true
            );
            setImageIfExists(ensalada, "static/img/dishes/ensalada-temporada.png");

            Dish agua = new Dish(
                    DishCategory.DRINK,
                    "Agua mineral",
                    "Agua fría (con o sin gas).",
                    List.of(),
                    new BigDecimal("2.00"),
                    true
            );
            setImageIfExists(agua, "static/img/dishes/agua-mineral.png");

            // ===== NEW MEATS (sin picaña) =====

            Dish tomahawk = new Dish(
                    DishCategory.MEAT,
                    "Tomahawk a la brasa",
                    "Tomahawk a la brasa (aprox. 1 kg), ideal para compartir.",
                    List.of(),
                    new BigDecimal("44.90"),
                    true
            );
            setImageIfExists(tomahawk, "static/img/dishes/tomahawk.png");

            Dish presaIberica = new Dish(
                    DishCategory.MEAT,
                    "Presa ibérica al carbón",
                    "Presa ibérica marcada a la brasa, muy jugosa.",
                    List.of(),
                    new BigDecimal("19.90"),
                    true
            );
            setImageIfExists(presaIberica, "static/img/dishes/presa-iberica.png");

            Dish parrillada = new Dish(
                    DishCategory.MEAT,
                    "Parrillada Fuego Lento",
                    "Selección de carnes a la brasa para 2 personas.",
                    List.of(),
                    new BigDecimal("39.90"),
                    true
            );
            setImageIfExists(parrillada, "static/img/dishes/parrillada.png");

            Dish burgerAngus = new Dish(
                    DishCategory.MEAT,
                    "Hamburguesa Black Angus",
                    "200g Black Angus, cheddar, bacon y salsa de la casa.",
                    List.of(Allergen.GLUTEN, Allergen.MILK, Allergen.EGG),
                    new BigDecimal("13.90"),
                    true
            );
            setImageIfExists(burgerAngus, "static/img/dishes/hamburguesa-angus.png");

            // ===== NEW STARTERS (sin morcilla) =====

            Dish provolone = new Dish(
                    DishCategory.STARTER,
                    "Provolone al horno",
                    "Queso provolone fundido con orégano y toque de brasa.",
                    List.of(Allergen.MILK),
                    new BigDecimal("8.90"),
                    true
            );
            setImageIfExists(provolone, "static/img/dishes/provolone.png");

            Dish chorizoCriollo = new Dish(
                    DishCategory.STARTER,
                    "Chorizo criollo a la brasa",
                    "Chorizo criollo marcado a la brasa, servido caliente.",
                    List.of(),
                    new BigDecimal("7.90"),
                    true
            );
            setImageIfExists(chorizoCriollo, "static/img/dishes/chorizo-criollo.png");

            Dish torreznos = new Dish(
                    DishCategory.STARTER,
                    "Torreznos crujientes",
                    "Torreznos tradicionales, crujientes y dorados.",
                    List.of(),
                    new BigDecimal("8.50"),
                    true
            );
            setImageIfExists(torreznos, "static/img/dishes/torreznos.png");

            Dish patatasCheddarBacon = new Dish(
                    DishCategory.STARTER,
                    "Patatas cheddar y bacon",
                    "Patatas con cheddar fundido y bacon crujiente.",
                    List.of(Allergen.MILK),
                    new BigDecimal("9.50"),
                    true
            );
            setImageIfExists(patatasCheddarBacon, "static/img/dishes/patatas-cheddar-bacon.png");

            // ===== NEW DESSERTS =====

            Dish brownie = new Dish(
                    DishCategory.DESSERT,
                    "Brownie con nueces y helado",
                    "Brownie templado con nueces y bola de helado.",
                    List.of(Allergen.GLUTEN, Allergen.EGG, Allergen.MILK, Allergen.NUTS),
                    new BigDecimal("6.90"),
                    true
            );
            setImageIfExists(brownie, "static/img/dishes/brownie.png");

            Dish coulant = new Dish(
                    DishCategory.DESSERT,
                    "Coulant de chocolate",
                    "Bizcocho de chocolate con corazón fundido.",
                    List.of(Allergen.GLUTEN, Allergen.EGG, Allergen.MILK),
                    new BigDecimal("6.50"),
                    true
            );
            setImageIfExists(coulant, "static/img/dishes/coulant-chocolate.png");

            Dish flan = new Dish(
                    DishCategory.DESSERT,
                    "Flan de huevo",
                    "Flan tradicional de huevo con caramelo.",
                    List.of(Allergen.EGG, Allergen.MILK),
                    new BigDecimal("4.90"),
                    true
            );
            setImageIfExists(flan, "static/img/dishes/flan-huevo.png");

            Dish cookie = new Dish(
                    DishCategory.DESSERT,
                    "Cookie caliente",
                    "Cookie templada con chips de chocolate.",
                    List.of(Allergen.GLUTEN, Allergen.EGG, Allergen.MILK),
                    new BigDecimal("5.90"),
                    true
            );
            setImageIfExists(cookie, "static/img/dishes/cookie-caliente.png");

            // ===== NEW DRINKS (sin sangría) =====

            Dish ribera = new Dish(
                    DishCategory.DRINK,
                    "Tinto Ribera del Duero",
                    "Copa de Ribera del Duero (tinto).",
                    List.of(),
                    new BigDecimal("3.80"),
                    true
            );
            setImageIfExists(ribera, "static/img/dishes/vino-ribera.png");

            Dish rioja = new Dish(
                    DishCategory.DRINK,
                    "Rioja crianza",
                    "Copa de Rioja crianza (tinto).",
                    List.of(),
                    new BigDecimal("3.60"),
                    true
            );
            setImageIfExists(rioja, "static/img/dishes/vino-crianza.png");

            Dish vinoCasa = new Dish(
                    DishCategory.DRINK,
                    "Vino tinto de la casa",
                    "Copa de vino tinto de la casa.",
                    List.of(),
                    new BigDecimal("2.90"),
                    true
            );
            setImageIfExists(vinoCasa, "static/img/dishes/vino-de-la-casa.png");

            Dish vinoBlanco = new Dish(
                    DishCategory.DRINK,
                    "Vino blanco Pescadito (semidulce)",
                    "Vino blanco semidulce, fresco y suave.",
                    List.of(),
                    new BigDecimal("3.40"),
                    true
            );
            setImageIfExists(vinoBlanco, "static/img/dishes/vino-blanco.png");

            Dish cervezaCasa = new Dish(
                    DishCategory.DRINK,
                    "Cerveza de la casa",
                    "Cerveza rubia bien fría.",
                    List.of(),
                    new BigDecimal("2.70"),
                    true
            );
            setImageIfExists(cervezaCasa, "static/img/dishes/cerveza-de-la-casa.png");

            Dish cervezaLimon = new Dish(
                    DishCategory.DRINK,
                    "Cerveza con limón",
                    "Cerveza con limón, refrescante y muy fría.",
                    List.of(),
                    new BigDecimal("2.90"),
                    true
            );
            setImageIfExists(cervezaLimon, "static/img/dishes/cerveza-limon.png");

            List<Dish> allDishes = List.of(
                    // old
                    chuleton, costillar, croquetas, tartaQueso,
                    entrecot, secreto, ensalada, agua,
                    // meats
                    tomahawk, presaIberica, parrillada, burgerAngus,
                    // starters
                    provolone, chorizoCriollo, torreznos, patatasCheddarBacon,
                    // desserts
                    brownie, coulant, flan, cookie,
                    // drinks
                    ribera, rioja, vinoCasa, vinoBlanco, cervezaCasa, cervezaLimon
            );
            dishRepository.saveAll(allDishes);
        }

        // ---------------------------
        // ORDERS — kitchen board demo (non-delivered)
        // ---------------------------
        boolean kitchenBoardEmpty = orderRepository.countByStatus(OrderStatus.SENT_TO_KITCHEN) == 0
                && orderRepository.countByStatus(OrderStatus.IN_PROGRESS) == 0
                && orderRepository.countByStatus(OrderStatus.READY) == 0;

        if (kitchenBoardEmpty) {

            Dish presa = findDishExact("Presa ibérica al carbón");
            Dish parrillada = findDishExact("Parrillada Fuego Lento");
            Dish burger = findDishExact("Hamburguesa Black Angus");

            Dish provolone = findDishExact("Provolone al horno");
            Dish torreznos = findDishExact("Torreznos crujientes");
            Dish patatas = findDishExact("Patatas cheddar y bacon");

            Dish cerveza = findDishExact("Cerveza de la casa");
            Dish cervezaLimon = findDishExact("Cerveza con limón");
            Dish rioja = findDishExact("Rioja crianza");

            Order o1 = new Order(user);
            o1.setStatus(OrderStatus.SENT_TO_KITCHEN);
            o1.setTableNumber(7);
            o1.setCustomerNote("Presa al punto, por favor.");
            o1.setCreatedAt(LocalDateTime.now().minusMinutes(35));
            o1.setRestaurant(restaurants.get(random.nextInt(restaurants.size())));
            addItem(o1, presa, 1);
            addItem(o1, provolone, 1);
            addItem(o1, cerveza, 2);
            orderRepository.save(o1);

            Order o2 = new Order(maria);
            o2.setStatus(OrderStatus.IN_PROGRESS);
            o2.setTableNumber(3);
            o2.setCustomerNote("Traer torreznos para compartir.");
            o2.setCreatedAt(LocalDateTime.now().minusMinutes(55));
            o2.setRestaurant(restaurants.get(random.nextInt(restaurants.size())));
            addItem(o2, parrillada, 1);
            addItem(o2, torreznos, 1);
            addItem(o2, cervezaLimon, 2);
            orderRepository.save(o2);

            Order o3 = new Order(juan);
            o3.setStatus(OrderStatus.READY);
            o3.setTableNumber(12);
            o3.setCustomerNote("Con patatas extra.");
            o3.setCreatedAt(LocalDateTime.now().minusMinutes(75));
            o3.setRestaurant(restaurants.get(random.nextInt(restaurants.size())));
            addItem(o3, burger, 2);
            addItem(o3, patatas, 1);
            addItem(o3, rioja, 2);
            orderRepository.save(o3);
        }

        // ---------------------------
        // ORDERS — a year of DELIVERED/CANCELLED history for the dashboard and the Data Warehouse ETL
        // ---------------------------
        List<User> usersPool = new ArrayList<>();
        usersPool.add(user);
        usersPool.add(maria);
        usersPool.add(juan);
        usersPool.add(admin);

        // Add some of the client users to the pool
        for (int i = 1; i <= 20; i++) {
            String uname = String.format("client%02d", i);
            userRepository.findByUsername(uname).ifPresent(usersPool::add);
        }

        List<Dish> allDishes = dishRepository.findAll();
        orderHistorySeeder.seedHistoricalOrders(restaurants, usersPool, allDishes);

        // ---------------------------
        // STOCK — ensure every (restaurant, dish) pair has a stock entry (available=true by default)
        // ---------------------------
        stockService.ensureStockEntriesExist(restaurants, allDishes);
    }

    private User ensureUser(String username,
                            String email,
                            LocalDate birthDate,
                            String rawPassword,
                            boolean banned,
                            LocalDateTime createdAt,
                            String... roles) {

        Optional<User> existing = userRepository.findByUsername(username);
        if (existing.isPresent()) {
            User u = existing.get();

            // Keep existing data, but ensure createdAt is not null and banned is set if needed
            if (u.getCreatedAt() == null && createdAt != null) {
                u.setCreatedAt(createdAt);
            }
            if (banned && !u.isBanned()) {
                u.setBanned(true);
            }
            return userRepository.save(u);
        }

        User u = new User(
                username,
                email,
                birthDate,
                passwordEncoder.encode(rawPassword),
                roles
        );
        if (createdAt != null) u.setCreatedAt(createdAt);
        u.setBanned(banned);
        return userRepository.save(u);
    }

    private int pickHourForRegistrations(Random rnd) {
        // More likely in the afternoon/evening
        int roll = rnd.nextInt(100);
        if (roll < 10) return 10 + rnd.nextInt(3);   // 10-12
        if (roll < 45) return 16 + rnd.nextInt(4);   // 16-19
        if (roll < 90) return 20 + rnd.nextInt(3);   // 20-22
        return 13 + rnd.nextInt(3);                  // 13-15
    }

    private Dish findDishExact(String exactName) {
        return dishRepository.findByNameContainingIgnoreCase(exactName).stream()
                .filter(d -> d.getName().equalsIgnoreCase(exactName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Dish not found: " + exactName));
    }

    private void addItem(Order order, Dish dish, int quantity) {
        OrderItem item = new OrderItem(dish, quantity, dish.getPrice());
        order.addItem(item);
    }

    private Restaurant makeRestaurant(String name, String address, String city, String province,
                                      String district, String postalCode,
                                      double latitude, double longitude, String phone) {
        Restaurant r = new Restaurant();
        r.setName(name);
        r.setAddress(address);
        r.setCity(city);
        r.setProvince(province);
        r.setDistrict(district);
        r.setPostalCode(postalCode);
        r.setLatitude(latitude);
        r.setLongitude(longitude);
        r.setPhone(phone);
        r.setActive(true);
        return r;
    }

    private void setImageIfExists(Dish dish, String classpathLocation) {
        Image img = imageFromStaticSafe(classpathLocation);
        if (img != null) {
            dish.setImage(img);
        }
    }

    /**
     * Loads an image from classpath (resources/static/...) and stores it in an Image entity as BLOB.
     * If the file does not exist, returns null (so the app does not crash).
     */
    private Image imageFromStaticSafe(String classpathLocation) {
        try {
            ClassPathResource res = new ClassPathResource(classpathLocation);
            if (!res.exists()) return null;

            byte[] bytes = res.getInputStream().readAllBytes();
            Image img = new Image();
            img.setImageFile(new SerialBlob(bytes));
            return img;
        } catch (Exception e) {
            return null;
        }
    }
}