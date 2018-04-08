package com.temas.protocols.benchmark.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.github.javafaker.Faker
import java.time.LocalDate
import java.time.ZoneId


@JsonIgnoreProperties(ignoreUnknown = true)
open class GetUserResponse (val users: List<User>)

@JsonIgnoreProperties(ignoreUnknown = true)
data class User(val firstName: String,
                val secondName: String,
                val birthDate: LocalDate,
                val age: Short,
                val city: String)

object Generator {
    private val faker = Faker()
    fun generateUsers(num: Int): List<User> {
        return (1..num).map {
            User(faker.name().firstName(),
                    faker.name().lastName(),
                    faker.date().birthday().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                    faker.number().numberBetween(16, 99).toShort(),
                    faker.address().city())
        }
    }

}


