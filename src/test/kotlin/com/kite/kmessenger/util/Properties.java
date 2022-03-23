package com.kite.kmessenger.util;

import io.micronaut.context.annotation.Property;

public @interface Properties {
    Property[] value();
}
