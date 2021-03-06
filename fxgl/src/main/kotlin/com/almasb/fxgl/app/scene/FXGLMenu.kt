/*
 * FXGL - JavaFX Game Library. The MIT License (MIT).
 * Copyright (c) AlmasB (almaslvl@gmail.com).
 * See LICENSE for details.
 */

package com.almasb.fxgl.app.scene

import com.almasb.fxgl.core.util.InputPredicates
import com.almasb.fxgl.dsl.FXGL
import com.almasb.fxgl.dsl.getDisplay
import com.almasb.fxgl.dsl.getSettings
import com.almasb.fxgl.dsl.localize
import com.almasb.fxgl.logging.Logger
import com.almasb.fxgl.profile.SaveFile
import com.almasb.fxgl.profile.SaveLoadService
import com.almasb.fxgl.scene.SubScene
import java.util.function.Consumer

/**
 * This is a base class for main/game menus.
 * Custom menus can interact with FXGL by calling fireXXXX methods.
 *
 * @author Almas Baimagambetov (AlmasB) (almaslvl@gmail.com)
 */
abstract class FXGLMenu(protected val type: MenuType) : SubScene() {

    companion object {
        private val log = Logger.get("Menu")
    }

    protected val saveLoadService: SaveLoadService = FXGL.getSaveLoadService()

    protected val controller by lazy { FXGL.getGameController() }
    protected val appWidth by lazy { FXGL.getAppWidth() }
    protected val appHeight by lazy { FXGL.getAppHeight() }

    /**
     * Starts new game.
     */
    protected fun fireNewGame() {
        log.debug("fireNewGame()")

        controller.startNewGame()
    }

    /**
     * Loads the game state from last modified save file.
     */
    protected fun fireContinue() {
        log.debug("fireContinue()")

        // TODO:
        //    override fun loadGameFromLastSave() {
//        saveLoadService
//                .loadLastModifiedSaveFileTask(getSettings().profileName.value, getSettings().saveFileExt)
//                .then { saveLoadService.readSaveFileTask(it) }
//                .onSuccess { startLoadedGame(it.data) }
//                .runAsyncFXWithDialog(ProgressDialog(local.getLocalizedString("menu.loading") + "..."))
//    }
    }

    /**
     * Loads the game state from a [saveFile].
     */
    protected fun fireLoad(saveFile: SaveFile) {
        log.debug("fireLoad()")

        val text = localize("menu.loadSave") + " [${saveFile.name}]?\n" + localize("menu.unsavedProgress")

        getDisplay().showConfirmationBox(text) { yes ->
            if (yes) {
                controller.loadGame(saveFile.data)
            }
        }
    }

    /**
     * Can only be fired from game menu.
     * Saves current state of the game to a file whose name is provided by user.
     */
    protected fun fireSave() {
        log.debug("fireSave()")

        getDisplay().showInputBoxWithCancel(localize("menu.enterSaveName"), InputPredicates.ALPHANUM, Consumer { saveFileName ->

            if (saveFileName.isEmpty())
                return@Consumer

            val saveFile = SaveFile(saveFileName + "." + getSettings().saveFileExt)

            if (saveLoadService.saveFileExists(saveFile.name)) {
                getDisplay().showConfirmationBox(localize("menu.overwrite") +" [$saveFileName]?") { yes ->

                    if (yes)
                        doSave(saveFile)
                }
            } else {
                doSave(saveFile)
            }
        })
    }

    private fun doSave(saveFile: SaveFile) {
        controller.saveGame(saveFile.data)

        FXGL.getTaskService().runAsyncFXWithDialog(
                saveLoadService.writeTask(saveFile.name, saveFile.data),
                localize("menu.savingData") + ": ${saveFile.name}"
        )
    }

    /**
     * Deletes a given [saveFile].
     */
    protected fun fireDelete(saveFile: SaveFile) {
        log.debug("fireDelete()")

        // TODO:
//        saveLoadService.deleteSaveFileTask(saveFile)
//                .run()
    }

    /**
     * Can only be fired from game menu.
     * Will close the menu and unpause the game.
     */
    protected fun fireResume() {
        log.debug("fireResume()")

        FXGL.getSceneService().popSubScene()
    }

    /**
     * Shows an exit dialog.
     * If the user selects "yes", the game will exit.
     */
    protected fun fireExit() {
        log.debug("fireExit()")

        val text = localize("dialog.exitGame")

        getDisplay().showConfirmationBox(text) { yes ->
            if (yes)
                controller.exit()
        }
    }

    /**
     * Shows an "exit to main menu" dialog.
     * If the user selects "yes", the game will exit to main menu.
     */
    protected fun fireExitToMainMenu() {
        log.debug("fireExitToMainMenu()")

        val text = localize("menu.exitMainMenu") + "\n" +
                localize("menu.unsavedProgress")

        getDisplay().showConfirmationBox(text) { yes ->
            if (yes)
                controller.gotoMainMenu()
        }
    }
}
