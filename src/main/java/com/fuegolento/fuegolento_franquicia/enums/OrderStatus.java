package com.fuegolento.fuegolento_franquicia.enums;

/**
 * Represents the different states an order can have
 * during its lifecycle.
 */
public enum OrderStatus {

    /**
     * Order is in the cart but not processed yet.
     */
    PENDING,

    /**
     * Order is sent to the kitchen.
     */
    SENT_TO_KITCHEN,

    /**
     * Order is being prepared in the kitchen.
     */
    IN_PROGRESS,

    /**
     * Order is ready to be delivered or picked up.
     */
    READY,

    /**
     * Order has been delivered.
     * At this point the order is considered closed and the total price is fixed.
     */
    DELIVERED,

    /**
     * Order was cancelled before being completed.
     */
    CANCELLED
}
