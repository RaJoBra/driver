package de.jbgb.driver.rest.patch

import de.jbgb.driver.entity.CarType
import de.jbgb.driver.entity.Driver

object DriverPatcher {
    fun patch(driver: Driver, operation: List<PatchOperation>): Driver {
        val replaceOps = operation.filter { "replace" == it.op }
        var driverUpdated = replaceOps(driver, replaceOps)

        val addOps = operation.filter { "add" == it.op }
        driverUpdated = addCar(driverUpdated, addOps)

        val removeOps = operation.filter { "remove" == it.op }
        return removeCar(driverUpdated, removeOps)
    }

    private fun replaceOps(driver: Driver, ops: Collection<PatchOperation>): Driver {
        var driverUpdated = driver
        ops.forEach { (_, path, value) ->
            when (path) {
                "/surname" -> {
                    driverUpdated = replaceSurname(driverUpdated, value)
                }
                "/email" -> {
                    driverUpdated = replaceEmail(driverUpdated, value)
                }
            }
        }
        return driverUpdated
    }

    private fun replaceSurname(driver: Driver, surname: String) = driver.copy(surname = surname)

    private fun replaceEmail(driver: Driver, email: String) = driver.copy(email = email)

    private fun addCar(driver: Driver, ops: Collection<PatchOperation>): Driver {
        if (ops.isEmpty()) {
            return driver
        }
        var driverUpdated = driver
        ops.filter { "/Car" == it.path }
            .forEach { driverUpdated = addCar(it, driverUpdated) }
        return driverUpdated
    }

    private fun addCar(op: PatchOperation, driver: Driver): Driver {
        val carStr = op.value
        val car = CarType.build(carStr)
            ?: throw InvalidCarException(carStr)
        val cars = if (driver.car == null)
            mutableListOf()
        else driver.car.toMutableList()
        cars.add(car)
        return driver.copy(car = cars)
    }

    private fun removeCar(driver: Driver, ops: List<PatchOperation>): Driver {
        if (ops.isEmpty()) {
            return driver
        }
        var driverUpdated = driver
        ops.filter { "/car" == it.path }
            .forEach { driverUpdated = removeCar(it, driver) }
        return driverUpdated
    }

    private fun removeCar(op: PatchOperation, driver: Driver): Driver {
        val carStr = op.value
        val car = CarType.build(carStr)
            ?: InvalidCarException(carStr)
        val cars = driver.car?.filter { it != car }
        return driver.copy(car = cars) //   }
    }

    class InvalidCarException(value: String) : RuntimeException("$value is no valid car")
}
