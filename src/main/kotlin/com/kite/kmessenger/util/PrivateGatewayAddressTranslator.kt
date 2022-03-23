package com.kite.kmessenger.util

import com.datastax.oss.driver.api.core.addresstranslation.AddressTranslator
import com.datastax.oss.driver.api.core.context.DriverContext
import java.net.InetSocketAddress

class PrivateGatewayAddressTranslator(val context: DriverContext) : AddressTranslator {
    override fun close() {
        // No-op
    }

    override fun translate(address: InetSocketAddress): InetSocketAddress {
        return InetSocketAddress("127.0.0.1", address.port)
    }
}