/*
 * Copyright (c) 2026 TinyGears.org
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.tinygears.uomcalc;

import java.util.ArrayList;
import java.util.List;

import org.tinygears.uom.Quantity;
import org.tinygears.uom.Untyped;
import org.tinygears.uom.engine.Dimension;
import org.tinygears.uom.quantity.Acceleration;
import org.tinygears.uom.quantity.AmountOfSubstance;
import org.tinygears.uom.quantity.Angle;
import org.tinygears.uom.quantity.Area;
import org.tinygears.uom.quantity.CatalyticActivity;
import org.tinygears.uom.quantity.Dimensionless;
import org.tinygears.uom.quantity.ElectricCapacitance;
import org.tinygears.uom.quantity.ElectricCharge;
import org.tinygears.uom.quantity.ElectricConductance;
import org.tinygears.uom.quantity.ElectricCurrent;
import org.tinygears.uom.quantity.ElectricInductance;
import org.tinygears.uom.quantity.ElectricPotential;
import org.tinygears.uom.quantity.ElectricResistance;
import org.tinygears.uom.quantity.Energy;
import org.tinygears.uom.quantity.Force;
import org.tinygears.uom.quantity.Frequency;
import org.tinygears.uom.quantity.Illuminance;
import org.tinygears.uom.quantity.Length;
import org.tinygears.uom.quantity.LuminousFlux;
import org.tinygears.uom.quantity.LuminousIntensity;
import org.tinygears.uom.quantity.MagneticFlux;
import org.tinygears.uom.quantity.MagneticFluxDensity;
import org.tinygears.uom.quantity.Mass;
import org.tinygears.uom.quantity.Power;
import org.tinygears.uom.quantity.Pressure;
import org.tinygears.uom.quantity.RadiationDoseAbsorbed;
import org.tinygears.uom.quantity.RadiationDoseEffective;
import org.tinygears.uom.quantity.Radioactivity;
import org.tinygears.uom.quantity.SolidAngle;
import org.tinygears.uom.quantity.Speed;
import org.tinygears.uom.quantity.Temperature;
import org.tinygears.uom.quantity.Time;
import org.tinygears.uom.quantity.Volume;

/** Names the uom quantity types a quantity can be read as; several types share a dimension (Frequency and Radioactivity). */
final class QuantityTypes {
    private static final List<Class<? extends Quantity<?>>> TYPES = List.of(
            Acceleration.class, AmountOfSubstance.class, Angle.class, Area.class, CatalyticActivity.class,
            Dimensionless.class, ElectricCapacitance.class, ElectricCharge.class, ElectricConductance.class,
            ElectricCurrent.class, ElectricInductance.class, ElectricPotential.class, ElectricResistance.class,
            Energy.class, Force.class, Frequency.class, Illuminance.class, Length.class, LuminousFlux.class,
            LuminousIntensity.class, MagneticFlux.class, MagneticFluxDensity.class, Mass.class, Power.class,
            Pressure.class, RadiationDoseAbsorbed.class, RadiationDoseEffective.class, Radioactivity.class,
            SolidAngle.class, Speed.class, Temperature.class, Time.class, Volume.class);

    private QuantityTypes() {}

    /** The simple names of the types with the dimension of {@code q}, empty if it has none (an untyped product). */
    static List<String> namesOf(Untyped q) {
        List<String> names = new ArrayList<>();
        for (Class<? extends Quantity<?>> type : TYPES) {
            if (fits(q, type)) {
                names.add(type.getSimpleName());
            }
        }
        return names;
    }

    private static <T extends Quantity<T>> boolean fits(Untyped q, Class<? extends Quantity<?>> type) {
        @SuppressWarnings("unchecked") // every entry of TYPES is a type of itself
        Class<T> t = (Class<T>) type;
        try {
            q.as(t);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** The dimension in the engine's notation, such as {@code L^2·M·T^-2}; {@code 1} for a number. */
    static String dimensionOf(Untyped q) {
        Dimension d = q.unit().dimension();
        return d.equals(Dimension.NONE) ? "1" : d.toString();
    }
}
