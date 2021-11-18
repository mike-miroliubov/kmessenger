package com.kite.kmessenger

import io.micronaut.runtime.Micronaut.*
fun main(args: Array<String>) {
	build()
	    .args(*args)
		.packages("com.kite.kmessenger")
		.start()
}

