package de.jbgb.driver.rest.patch

data class PatchOperation(val op: String, val path: String, val value: String)
