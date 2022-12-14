/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
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

/**
 * Represents a Java array type declaration.
 *
 * @since 2.0
 * @author Jason T. Greene
 */
public final class ArrayType extends Type {
    private final Type component;
    private final int dimensions;
    private int hash;

    /**
     * Create a new mock array type instance with the specified component
     * and dimensions.
     *
     * @param component the array component
     * @param dimensions the number of dimensions of this array
     * @return the new mock array type instance
     * @since 2.1
     */
    public static ArrayType create(Type component, int dimensions) {
        return new ArrayType(component, dimensions);
    }

    ArrayType(Type component, int dimensions) {
        this(component, dimensions, null);
    }

    ArrayType(Type component, int dimensions, AnnotationInstance[] annotations) {
        super(DotName.OBJECT_NAME, annotations);
        this.dimensions = dimensions;
        this.component = component;
    }

    /**
     * Returns the component type of the array. For example, {@code String[]} has
     * a component type of {@code String}.
     * <p>
     * It is possible for an {@code ArrayType} to have another {@code ArrayType}
     * as its component type. This happens when an array has some of its dimensions
     * annotated (e.g. {@code String[] @Ann []}). In such case, having multiple nested
     * {@code ArrayType}s is necessary to faithfully represent the annotations.
     *
     * @return the component type
     */
    public Type component() {
        return component;
    }

    @Override
    public DotName name() {
        StringBuilder builder = new StringBuilder();

        Type type = this;
        while (type.kind() == Kind.ARRAY) {
            int dimensions = type.asArrayType().dimensions;
            while (dimensions-- > 0) {
                builder.append('[');
            }
            type = type.asArrayType().component;
        }

        // here, `type` is an element type of the array, i.e., never array
        if (type.kind() == Kind.PRIMITIVE) {
            builder.append(type.asPrimitiveType().toCode());
        } else {
            // This relies on name() representing the erased type name
            // For historical 1.x reasons, we follow the Java reflection format
            // instead of the Java descriptor format.
            builder.append('L').append(type.name().toString()).append(';');
        }

        return DotName.createSimple(builder.toString());
    }

    /**
     * The number of dimensions this array type has. For example, this method would return 2
     * for an array type of {@code String[][]}.
     * <p>
     * Note that an {@code ArrayType} may have another {@code ArrayType} as its component type
     * (see {@link #component()}). For example, {@code String[] @Ann []} is an array type
     * with 1 dimension and a component type of another array type, also with 1 dimension.
     *
     * @return the number of dimensions of this array type
     */
    public int dimensions() {
        return dimensions;
    }

    @Override
    public Kind kind() {
        return Kind.ARRAY;
    }

    @Override
    public ArrayType asArrayType() {
        return this;
    }

    @Override
    Type copyType(AnnotationInstance[] newAnnotations) {
        return new ArrayType(component, dimensions, newAnnotations);
    }

    Type copyType(Type component, int dimensions) {
        return new ArrayType(component, dimensions, annotationArray());
    }

    @Override
    String toString(boolean simple) {
        StringBuilder builder = new StringBuilder();

        appendRootComponent(builder, true);
        appendArraySyntax(builder);

        return builder.toString();
    }

    private void appendRootComponent(StringBuilder builder, boolean simple) {
        if (component.kind() == Kind.ARRAY) {
            component.asArrayType().appendRootComponent(builder, simple);
        } else {
            builder.append(component.toString(simple));
        }
    }

    private void appendArraySyntax(StringBuilder builder) {
        if (annotationArray().length > 0) {
            builder.append(' ');
            appendAnnotations(builder);
        }
        for (int i = 0; i < dimensions; i++) {
            builder.append("[]");
        }
        if (component.kind() == Kind.ARRAY) {
            component.asArrayType().appendArraySyntax(builder);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ArrayType)) {
            return false;
        }
        ArrayType arrayType = (ArrayType) o;

        return super.equals(o) && dimensions == arrayType.dimensions && component.equals(arrayType.component);
    }

    @Override
    public int hashCode() {
        int hash = this.hash;
        if (hash != 0) {
            return hash;
        }

        hash = super.hashCode();
        hash = 31 * hash + component.hashCode();
        hash = 31 * hash + dimensions;
        return this.hash = hash;
    }

    @Override
    public boolean internEquals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ArrayType)) {
            return false;
        }
        ArrayType arrayType = (ArrayType) o;

        return super.internEquals(o) && dimensions == arrayType.dimensions && component.internEquals(arrayType.component);
    }

    @Override
    public int internHashCode() {
        int hash = super.internHashCode();
        hash = 31 * hash + component.internHashCode();
        hash = 31 * hash + dimensions;
        return hash;
    }
}
