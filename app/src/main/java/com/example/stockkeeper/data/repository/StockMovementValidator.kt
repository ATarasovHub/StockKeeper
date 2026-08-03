package com.example.stockkeeper.data.repository

object StockMovementValidator {
    fun requirePositiveQuantity(quantity: Int) {
        require(quantity > 0) { "Quantity must be greater than zero" }
    }

    fun requireSufficientStock(currentBalance: Int, requestedQuantity: Int) {
        requirePositiveQuantity(requestedQuantity)
        require(currentBalance >= requestedQuantity) {
            "Insufficient stock: available $currentBalance, requested $requestedQuantity"
        }
    }

    fun requireNonZeroAdjustment(quantityDelta: Int) {
        require(quantityDelta != 0) { "Adjustment must not be zero" }
    }
}
