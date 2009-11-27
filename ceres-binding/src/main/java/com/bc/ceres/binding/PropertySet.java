package com.bc.ceres.binding;

import java.beans.PropertyChangeListener;

/**
 * A loose aggregation of properties. Properties can be added to and removed from this set.
 * Property change events are fired whenever property values change.
 * <p/>
 * The {@link PropertySet} interface is based on the well-known <i>Property List</i> design pattern.
 *
 * @author Norman Fomferra
 * @see Property
 * @since 0.10
 */
public interface PropertySet {
    /**
     * Gets all properties currently contained in this bundle.
     * @return The array of properties, which may be empty.
     */
    Property[] getProperties();

    /**
     * Gets the named property.
     * @param name The property name  (case sensitive).
     * @return The property, or {@code null} if the property does not exist.
     */
    Property getProperty(String name);

    /**
     * Adds a property to this bundle.
     * @param property The property.
     */
    void addProperty(Property property);

    /**
     * Adds the given properties to this bundle.
     * @param properties The properties to be added.
     */
    void addProperties(Property ... properties);

    /**
     * Removes a property from this bundle.
     * @param property The property.
     */
    void removeProperty(Property property);

    /**
     * Removes the given properties from this bundle.
     * @param properties The properties to be removed.
     */
    void removeProperties(Property[] properties);

    /**
     * Gets the value of the named property.
     *
     * @param name The property name.
     *
     * @return The property value.
     */
    Object getValue(String name);

    /**
     * Sets the value of the named property.
     *
     * @param name  The property name.
     * @param value The new property value.
     *
     * @throws IllegalArgumentException If the value is illegal.
     *                                  The cause will always be a {@link ValidationException}.
     */
    void setValue(String name, Object value) throws IllegalArgumentException;

    /**
     * Gets the descriptor for the named property.
     *
     * @param name The property name (case sensitive).
     *
     * @return The descriptor, or {@code null} if the property is unknown.
     */
    PropertyDescriptor getDescriptor(String name);

    /**
     * Adds a property change listener to this bundle.
     * @param l The listener.
     */
    void addPropertyChangeListener(PropertyChangeListener l);

    /**
     * Adds a dedicated property change listener to this bundle.
     * @param name The property name.
     * @param l The listener.
     */
    void addPropertyChangeListener(String name, PropertyChangeListener l);

    /**
     * Removes a property change listener from this bundle.
     * @param l The listener.
     */
    void removePropertyChangeListener(PropertyChangeListener l);

    /**
     * Removes a dedicated property change listener from this bundle.
     * @param name The property name.
     * @param l The listener.
     */
    void removePropertyChangeListener(String name, PropertyChangeListener l);
}
