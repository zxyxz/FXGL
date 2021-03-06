/*
 * FXGL - JavaFX Game Library. The MIT License (MIT).
 * Copyright (c) AlmasB (almaslvl@gmail.com).
 * See LICENSE for details.
 */

package com.almasb.fxgl.animation

import com.almasb.fxgl.test.RunWithFX
import javafx.geometry.Point2D
import javafx.geometry.Point3D
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.CubicCurve
import javafx.scene.shape.QuadCurve
import javafx.scene.shape.Rectangle
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
@ExtendWith(RunWithFX::class)
class AnimatedValueTest {

    @Test
    fun `Point2D`() {
        val anim = AnimatedPoint2D(Point2D(0.0, 0.0), Point2D(100.0, 100.0))

        assertThat(anim.getValue(0.0), `is`(Point2D(0.0, 0.0)))
        assertThat(anim.getValue(1.0), `is`(Point2D(100.0, 100.0)))
        assertThat(anim.getValue(0.5), `is`(Point2D(50.0, 50.0)))
    }

    @Test
    fun `Double`() {
        val anim = AnimatedValue<Double>(100.0, 200.0)

        assertThat(anim.getValue(0.0), `is`(100.0))
        assertThat(anim.getValue(1.0), `is`(200.0))
        assertThat(anim.getValue(0.5), `is`(150.0))
    }

    @Test
    fun `Bezier quad`() {
        val anim = AnimatedQuadBezierPoint2D(QuadCurve(0.0, 0.0, 15.0, 3.0, 100.0, 100.0))

        assertThat(anim.getValue(0.0), `is`(Point2D(0.0, 0.0)))
        assertThat(anim.getValue(1.0), `is`(Point2D(100.0, 100.0)))
        assertThat(anim.getValue(0.5), `is`(Point2D(32.5, 26.5)))
    }

    @Test
    fun `Bezier cubic`() {
        val anim = AnimatedCubicBezierPoint2D(CubicCurve(0.0, 0.0, 15.0, 3.0, 55.0, 33.0, 100.0, 100.0))

        assertThat(anim.getValue(0.0), `is`(Point2D(0.0, 0.0)))
        assertThat(anim.getValue(1.0), `is`(Point2D(100.0, 100.0)))
        assertThat(anim.getValue(0.5), `is`(Point2D(38.75, 26.0)))
    }
	
    @Test
    fun `Bezier quad3D`() {
        val anim = AnimatedQuadBezierPoint3D(QuadCurve(0.0, 0.0, 15.0, 3.0, 100.0, 100.0))

        assertThat(anim.getValue(0.0), `is`(Point3D(0.0, 0.0, 0.0)))
        assertThat(anim.getValue(1.0), `is`(Point3D(100.0, 100.0, 0.0)))
        assertThat(anim.getValue(0.5), `is`(Point3D(32.5, 26.5, 0.0)))
    }

    @Test
    fun `Bezier cubic3D`() {
        val anim = AnimatedCubicBezierPoint3D(CubicCurve(0.0, 0.0, 15.0, 3.0, 55.0, 33.0, 100.0, 100.0))

        assertThat(anim.getValue(0.0), `is`(Point3D(0.0, 0.0, 0.0)))
        assertThat(anim.getValue(1.0), `is`(Point3D(100.0, 100.0, 0.0)))
        assertThat(anim.getValue(0.5), `is`(Point3D(38.75, 26.0, 0.0)))
    }

    @Test
    fun `Color`() {
        val anim = AnimatedColor(Color.BLACK, Color.WHITE)

        assertThat(anim.getValue(0.0), `is`(Color.BLACK))
        assertThat(anim.getValue(1.0), `is`(Color.WHITE))
        assertThat(anim.getValue(0.5), `is`(Color.color(0.5, 0.5, 0.5)))
    }

    @Test
    fun `Path`() {
        val rect = Rectangle(160.0, 50.0)

        val anim = AnimatedPath(rect)

        assertThat(anim.getValue(0.0), `is`(Point3D(0.0, 0.13125, 0.0)))
        assertThat(anim.getValue(1.0), `is`(Point3D(0.0, 0.0, 0.0)))
        assertThat(anim.getValue(0.5), `is`(Point3D(160.0, 50.0, 0.0)))
    }
}