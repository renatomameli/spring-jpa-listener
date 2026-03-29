package com.mameli.jpalistener.demo

import com.mameli.jpalistener.annotation.TrackedEntity
import com.mameli.jpalistener.listener.TrackingEntityListener
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "products")
@EntityListeners(TrackingEntityListener::class)
@TrackedEntity
class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
) {
    var name: String = ""
    var price: BigDecimal = BigDecimal.ZERO
    var description: String? = null
}
