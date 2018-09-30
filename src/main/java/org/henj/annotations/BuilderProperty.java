package org.henj.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Project henj
 * @Author Henri Joel SEDJAME
 * @Date 21/09/2018
 * @Class purposes : .......
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface BuilderProperty {
}
