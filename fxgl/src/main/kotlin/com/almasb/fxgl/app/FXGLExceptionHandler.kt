/*
 * FXGL - JavaFX Game Library. The MIT License (MIT).
 * Copyright (c) AlmasB (almaslvl@gmail.com).
 * See LICENSE for details.
 */

package com.almasb.fxgl.app

import com.almasb.fxgl.service.ExceptionHandler

/**
 * Default exception handler.
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
class FXGLExceptionHandler : ExceptionHandler {

    override fun handle(e: Throwable) {
        FXGL.getDisplay().showErrorBox(e)
    }
}