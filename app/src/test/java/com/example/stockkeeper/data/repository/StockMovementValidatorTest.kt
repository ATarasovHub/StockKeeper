package com.example.stockkeeper.data.repository

import org.junit.Assert.assertThrows
import org.junit.Test

class StockMovementValidatorTest {
    @Test
    fun positiveQuantity_isAccepted() {
        StockMovementValidator.requirePositiveQuantity(1)
    }

    @Test
    fun zeroQuantity_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            StockMovementValidator.requirePositiveQuantity(0)
        }
    }

    @Test
    fun quantityWithinBalance_isAccepted() {
        StockMovementValidator.requireSufficientStock(
            currentBalance = 10,
            requestedQuantity = 10,
        )
    }

    @Test
    fun quantityAboveBalance_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            StockMovementValidator.requireSufficientStock(
                currentBalance = 3,
                requestedQuantity = 4,
            )
        }
    }

    @Test
    fun zeroAdjustment_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            StockMovementValidator.requireNonZeroAdjustment(0)
        }
    }
}
