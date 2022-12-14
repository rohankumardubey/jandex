/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.jandex;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * An annotation instance represents a specific usage of an annotation on a
 * target. It contains a set of values, as well as a reference to the target
 * itself (e.g. class, field, method, etc).
 *
 * <p>
 * <b>Thread-Safety</b>
 * </p>
 * This class is immutable and can be shared between threads without safe
 * publication.
 *
 * @author Jason T. Greene
 *
 */
public final class AnnotationInstance {
    static final NameComparator NAME_COMPARATOR = new NameComparator();
    static final AnnotationInstance[] EMPTY_ARRAY = new AnnotationInstance[0];
    static final DotName RETENTION = new DotName(DotName.JAVA_LANG_ANNOTATION_NAME, "Retention", true, false);

    private final DotName name;
    private final AnnotationTarget target;
    private final AnnotationValue[] values;
    private final boolean runtimeVisible;

    static class NameComparator implements Comparator<AnnotationInstance> {
        public int compare(AnnotationInstance instance1, AnnotationInstance instance2) {
            return instance1.name().compareTo(instance2.name());
        }
    }

    private AnnotationInstance(DotName name, AnnotationTarget target, AnnotationValue[] values, boolean runtimeVisible) {
        this.name = name;
        this.target = target;
        this.values = values != null && values.length > 0 ? values : AnnotationValue.EMPTY_ARRAY;
        this.runtimeVisible = runtimeVisible;
    }

    static AnnotationInstance create(AnnotationInstance instance, AnnotationTarget target) {
        return new AnnotationInstance(instance.name, target, instance.values, instance.runtimeVisible);
    }

    /**
     * Creates a {@linkplain AnnotationInstanceBuilder builder} of annotation instances of given {@code annotationType}.
     * This allows creating an {@code AnnotationInstance} in a nicer way than using any of the {@code create()} methods.
     *
     * @param annotationType the type of annotation whose instance will be created, must not be {@code null}
     * @return a builder instance, never {@code null}
     */
    public static AnnotationInstanceBuilder builder(Class<? extends Annotation> annotationType) {
        if (annotationType == null) {
            throw new IllegalArgumentException("Annotation type can't be null");
        }
        DotName name = DotName.createSimple(annotationType.getName());
        boolean visible = annotationType.isAnnotationPresent(Retention.class)
                && annotationType.getAnnotation(Retention.class).value() == RetentionPolicy.RUNTIME;
        return new AnnotationInstanceBuilder(name, visible);
    }

    /**
     * Creates a {@linkplain AnnotationInstanceBuilder builder} of annotation instances of given {@code annotationType}.
     * This allows creating an {@code AnnotationInstance} in a nicer way than using any of the {@code create()} methods.
     *
     * @param annotationType the type of annotation whose instance will be created, must not be {@code null}
     * @return a builder instance, never {@code null}
     */
    public static AnnotationInstanceBuilder builder(ClassInfo annotationType) {
        if (annotationType == null) {
            throw new IllegalArgumentException("Annotation type can't be null");
        }
        DotName name = annotationType.name();
        boolean visible = annotationType.hasDeclaredAnnotation(RETENTION)
                && annotationType.declaredAnnotation(RETENTION).value().asString().equals("RUNTIME");
        return new AnnotationInstanceBuilder(name, visible);
    }

    /**
     * Creates a {@linkplain AnnotationInstanceBuilder builder} of annotation instances of given {@code annotationType}.
     * This allows creating an {@code AnnotationInstance} in a nicer way than using any of the {@code create()} methods.
     * <p>
     * The given {@code annotationType} is assumed to have the {@linkplain RetentionPolicy#RUNTIME runtime} retention.
     *
     * @param annotationType the type of annotation whose instance will be created, must not be {@code null}
     * @return a builder instance, never {@code null}
     */
    public static AnnotationInstanceBuilder builder(DotName annotationType) {
        return builder(annotationType, true);
    }

    /**
     * Creates a {@linkplain AnnotationInstanceBuilder builder} of annotation instances of given {@code annotationType}.
     * This allows creating an {@code AnnotationInstance} in a nicer way than using any of the {@code create()} methods.
     *
     * @param annotationType the type of annotation whose instance will be created, must not be {@code null}
     * @param runtimeVisible whether the annotation type has the {@linkplain RetentionPolicy#RUNTIME runtime} retention
     * @return a builder instance, never {@code null}
     */
    public static AnnotationInstanceBuilder builder(DotName annotationType, boolean runtimeVisible) {
        if (annotationType == null) {
            throw new IllegalArgumentException("Annotation type can't be null");
        }
        return new AnnotationInstanceBuilder(annotationType, runtimeVisible);
    }

    /**
     * Construct a new mock annotation instance. The passed values array will be defensively copied.
     * It is assumed that the annotation is {@link #runtimeVisible()}.
     *
     * @param name the name of the annotation instance
     * @param target the thing the annotation is declared on
     * @param values the values of this annotation instance
     * @return the new mock Annotation Instance
     */
    public static AnnotationInstance create(DotName name, AnnotationTarget target, AnnotationValue[] values) {
        return create(name, true, target, values);
    }

    /**
     * Construct a new mock annotation instance. The passed values array will be defensively copied.
     *
     * @param name the name of the annotation instance
     * @param visible whether the annotation is visible at runtime via the reflection API
     * @param target the thing the annotation is declared on
     * @param values the values of this annotation instance
     * @return the new mock Annotation Instance
     */
    public static AnnotationInstance create(DotName name, boolean visible, AnnotationTarget target,
            AnnotationValue[] values) {
        if (name == null)
            throw new IllegalArgumentException("Name can't be null");

        if (values == null)
            throw new IllegalArgumentException("Values can't be null");

        values = values.clone();

        // Sort entries so they can be binary searched
        Arrays.sort(values, new Comparator<AnnotationValue>() {
            public int compare(AnnotationValue o1, AnnotationValue o2) {
                return o1.name().compareTo(o2.name());
            }
        });

        return new AnnotationInstance(name, target, values, visible);
    }

    /**
     * Construct a new mock annotation instance. The passed values list will be defensively copied.
     * It is assumed that the annotation is {@link #runtimeVisible()}.
     *
     * @param name the name of the annotation instance
     * @param target the thing the annotation is declared on
     * @param values the values of this annotation instance
     * @return the new mock Annotation Instance
     */
    public static AnnotationInstance create(DotName name, AnnotationTarget target, List<AnnotationValue> values) {
        return create(name, true, target, values);
    }

    /**
     * Construct a new mock annotation instance. The passed values list will be defensively copied.
     *
     * @param name the name of the annotation instance
     * @param visible whether the annotation is visible at runtime via the reflection API
     * @param target the thing the annotation is declared on
     * @param values the values of this annotation instance
     * @return the new mock Annotation Instance
     */
    public static AnnotationInstance create(DotName name, boolean visible, AnnotationTarget target,
            List<AnnotationValue> values) {
        if (name == null)
            throw new IllegalArgumentException("Name can't be null");

        if (values == null)
            throw new IllegalArgumentException("Values can't be null");

        return create(name, visible, target, values.toArray(AnnotationValue.EMPTY_ARRAY));
    }

    static AnnotationInstance binarySearch(AnnotationInstance[] annotations, DotName name) {
        // only `name` is significant in `key`, the rest can be arbitrary
        AnnotationInstance key = new AnnotationInstance(name, null, null, false);
        int i = Arrays.binarySearch(annotations, key, AnnotationInstance.NAME_COMPARATOR);
        return i >= 0 ? annotations[i] : null;
    }

    /**
     * The name of this annotation in DotName form.
     *
     * @return the name of this annotation
     */
    public DotName name() {
        return name;
    }

    /**
     * The Java element that this annotation was declared on. This can be
     * a class, a field, a method, or a method parameter. In addition it may
     * be null if this instance is a nested annotation, in which case there is
     * no target.
     *
     * @return the target this annotation instance refers to
     */
    public AnnotationTarget target() {
        return target;
    }

    /**
     * Returns a value that corresponds with the specified parameter name.
     * If the parameter was not specified by this instance then null is
     * returned. Note that this also applies to a defaulted parameter,
     * which is not recorded in the target class.
     *
     * @param name the parameter name
     * @return the value of the specified parameter, or null if not provided
     */
    public AnnotationValue value(final String name) {
        int result = Arrays.binarySearch(values, name, new Comparator<Object>() {
            public int compare(Object o1, Object o2) {
                return ((AnnotationValue) o1).name().compareTo(name);
            }
        });
        return result >= 0 ? values[result] : null;
    }

    /**
     * Returns the value that is associated with the special default "value"
     * parameter.
     *
     * @return the "value" value
     */
    public AnnotationValue value() {
        return value("value");
    }

    /**
     * Returns a value that corresponds with the specified parameter name,
     * accounting for its default value. Since an annotation's defaults are
     * only stored on the annotation's defining class, and not usages of the
     * annotation, an index containing the Annotation class must be provided
     * as a parameter. If the index does not contain the defining annotation
     * class, then an <code>IllegalArgumentException</code> will be thrown to
     * prevent non-deterministic results.
     *
     * <p>
     * If the parameter was not specified by this instance, then the
     * annotation's <code>ClassInfo</code> is checked for a default value.
     * If there is a default, that value is returned. Otherwise null is
     * returned.
     * </p>
     *
     * @param index the index containing the defining annotation class
     * @param name the name of the annotation parameter
     * @return the value of the specified parameter, the default, or null
     * @throws IllegalArgumentException if index does not contain the defining
     *         annotation class
     * @since 2.1
     */
    public AnnotationValue valueWithDefault(IndexView index, String name) {
        ClassInfo definition = index.getClassByName(this.name);
        if (definition == null) {
            throw new IllegalArgumentException("Index did not contain annotation definition: " + this.name);
        }

        AnnotationValue result = value(name);
        if (result != null) {
            return result;
        }

        MethodInfo method = definition.method(name);
        return method == null ? null : method.defaultValue();
    }

    /**
     * Returns the value that is associated with the special default "value"
     * parameter, also accounting for a value default. Since an annotation's
     * defaults are only stored on the annotation's defining class, and not
     * usages of the annotation, an index containing the Annotation class must
     * be provided as a parameter. If the index does not contain the defining
     * annotation class, then an <code>IllegalArgumentException</code> will be
     * thrown to prevent non-deterministic results.
     *
     * <p>
     * If the "value" parameter was not specified by this instance, then the
     * annotation's <code>ClassInfo</code> is checked for a default value.
     * If there is a default, that value is returned. Otherwise null is
     * returned.
     * </p>
     *
     * @param index the index containing the defining annotation class
     * @return the "value" value, or its default, or null
     * @throws IllegalArgumentException if index does not contain the defining
     *         annotation class
     * @since 2.1
     */
    public AnnotationValue valueWithDefault(IndexView index) {
        return valueWithDefault(index, "value");
    }

    /**
     * Returns a list of all parameter values on this annotation instance,
     * including default values id defined. Since an annotation's defaults are
     * only stored on the annotation's defining class, and not usages of the
     * annotation, an index containing the Annotation class must be provided as
     * a parameter. If the index does not contain the defining annotation class,
     * then an <code>IllegalArgumentException</code> will be thrown to prevent
     * non-deterministic results.
     *
     * <p>
     * The order of this list is undefined.
     * </p>
     *
     * @return the parameter values of this annotation
     * @throws IllegalArgumentException if index does not contain the defining
     *         annotation class
     * @since 2.1
     */
    public List<AnnotationValue> valuesWithDefaults(IndexView index) {
        ClassInfo definition = index.getClassByName(this.name);
        if (definition == null) {
            throw new IllegalArgumentException("Index did not contain annotation definition: " + this.name);
        }

        List<MethodInfo> methods = definition.methods();
        ArrayList<AnnotationValue> result = new ArrayList<AnnotationValue>(methods.size());
        for (MethodInfo method : methods) {
            AnnotationValue value = value(method.name());
            if (value == null) {
                value = method.defaultValue();
            }
            if (value != null) {
                result.add(value);
            }
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * Returns a list of all parameter values on this annotation instance.
     * While random access is allowed, the ordering algorithm
     * of the list should not be relied upon. Although it will
     * be consistent for the life of this instance.
     *
     * @return the parameter values of this annotation
     */
    public List<AnnotationValue> values() {
        return Collections.unmodifiableList(Arrays.asList(values));
    }

    AnnotationValue[] valueArray() {
        return values;
    }

    /**
     * Returns true if this annotation uses RetentionPolicy.RUNTIME and is
     * visible to runtime reflection.
     *
     * @since 3.0
     */
    public boolean runtimeVisible() {
        return this.runtimeVisible;
    }

    /**
     * Returns an optionally simplified string that represents this annotation instance.
     * If simplified the output is smaller but missing information, such as the package
     * qualifier.
     *
     * @param simple whether to provide a simpler string representation
     * @return a string representation for this object
     * @since 2.0
     */
    public String toString(boolean simple) {
        StringBuilder builder = new StringBuilder("@").append(simple ? name.local() : name);

        if (simple && values.length == 1 && values[0].name().equals("value")) {
            builder.append("(");
            builder.append(values[0].toString(false));
            builder.append(')');
        } else if (values.length > 0) {
            builder.append("(");
            for (int i = 0; i < values.length; i++) {
                builder.append(values[i]);
                if (i < values.length - 1)
                    builder.append(",");
            }
            builder.append(')');
        }

        return builder.toString();
    }

    /**
     * Returns a string representation for this annotation instance. This method is equivalent
     * to calling {@link #toString(boolean)} with a value of {@code true}.
     *
     * @return a simple string representation for this annotation instance
     */
    public String toString() {
        return toString(true);
    }

    // runtime visibility is ignored for the purpose of equality and hash code, because
    // the annotation type identity (the name) already includes that information

    /**
     * Returns whether this annotation instance is equal to another instance.
     * Two annotation instances are equal if their names and values of their members are equal,
     * and they share the exact same {@code AnnotationTarget} instance. The latter restriction
     * may be softened in future versions.
     *
     * @param o the annotation instance to compare to.
     * @return true if equal, false if not
     *
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        AnnotationInstance instance = (AnnotationInstance) o;

        return target == instance.target && name.equals(instance.name) && Arrays.equals(values, instance.values);
    }

    /**
     * Returns a hash code representing this object.
     *
     * @return the hash code of this object
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + Arrays.hashCode(values);

        return result;
    }
}
