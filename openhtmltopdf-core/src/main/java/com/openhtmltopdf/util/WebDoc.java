package com.openhtmltopdf.util;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;


/**
 * Lets us specify a url where the type, method, etc
 * is documented. Specified as an annotation so we can use
 * IDE features to find instances and maybe tooling can use it too.
 */
@Retention(SOURCE)
@Target({ TYPE, FIELD, METHOD, CONSTRUCTOR })
public @interface WebDoc {
    /**
     * A url where docs can be found.
     */
    public String value();
}
