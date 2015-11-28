/*
 * The MIT License (MIT)
 *
 * FXGL - JavaFX Game Library
 *
 * Copyright (c) 2015 AlmasB (almaslvl@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.almasb.fxgl.event;

import java.security.Key;
import java.util.Optional;
import java.util.logging.Logger;

import com.almasb.fxgl.GameScene;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.settings.UserProfile;
import com.almasb.fxgl.settings.UserProfileSavable;
import com.almasb.fxgl.util.FXGLLogger;
import com.almasb.fxgl.util.WorldStateListener;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.input.*;

/**
 * Provides access to mouse state and allows binding of actions
 * to key and mouse events
 *
 * @author Almas Baimagambetov (AlmasB) (almaslvl@gmail.com)
 */
public final class InputManager implements WorldStateListener, UserProfileSavable {

    private static final Logger log = FXGLLogger.getLogger("FXGL.InputManager");

    /**
     * Holds mouse state information
     */
    private Mouse mouse = new Mouse();

    /**
     * Contains a list of user actions and keys/mouse buttons which trigger those
     * actions.
     */
    private ObservableList<InputBinding> bindings = FXCollections.observableArrayList();

    /**
     * @return unmodifiable view of the input bindings registered
     */
    public ObservableList<InputBinding> getBindings() {
        return FXCollections.unmodifiableObservableList(bindings);
    }

    /**
     * Currently active actions.
     */
    private ObservableList<UserAction> currentActions = FXCollections.observableArrayList();

    private GameScene gameScene;

    public InputManager(GameScene gameScene) {
        this.gameScene = gameScene;

        currentActions.addListener((ListChangeListener.Change<? extends UserAction> c) -> {
            while (c.next()) {
                if (!processActions)
                    continue;

                if (c.wasAdded()) {
                    c.getAddedSubList().forEach(UserAction::onActionBegin);
                } else if (c.wasRemoved()) {
                    c.getRemoved().forEach(UserAction::onActionEnd);
                }
            }
        });

        gameScene.addEventHandler(KeyEvent.KEY_PRESSED, event -> handlePressed(new Trigger(event)));
        gameScene.addEventHandler(KeyEvent.KEY_RELEASED, event -> handleReleased(new Trigger(event)));

        gameScene.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> handlePressed(new Trigger(event)));
        gameScene.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> handleReleased(new Trigger(event)));

        gameScene.addEventHandler(MouseEvent.MOUSE_PRESSED, mouse::update);
        gameScene.addEventHandler(MouseEvent.MOUSE_DRAGGED, mouse::update);
        gameScene.addEventHandler(MouseEvent.MOUSE_RELEASED, mouse::update);
        gameScene.addEventHandler(MouseEvent.MOUSE_MOVED, mouse::update);
    }

    /**
     * Handle pressed event for given trigger.
     *
     * @param trigger the trigger
     */
    private void handlePressed(Trigger trigger) {
        bindings.stream()
                .filter(binding -> {
                    return binding.isTriggered(trigger);
                })
                .map(InputBinding::getAction)
                .filter(action -> !currentActions.contains(action))
                .forEach(currentActions::add);
    }

    /**
     * Handle released event for given trigger
     *
     * @param trigger the trigger
     */
    private void handleReleased(Trigger trigger) {
        bindings.stream()
                .filter(binding -> {
                    if (trigger.key == null) {
                        return binding.isTriggered(trigger.btn);
                    } else {
                        KeyCode key = trigger.key;
                        switch (key) {
                            case CONTROL:
                                return binding.getModifier() == InputModifier.CTRL;
                            case SHIFT:
                                return binding.getModifier() == InputModifier.SHIFT;
                            case ALT:
                                return binding.getModifier() == InputModifier.ALT;
                        }

                        return binding.isTriggered(trigger.key);
                    }
                })
                .map(InputBinding::getAction)
                .forEach(currentActions::remove);
    }

    private boolean processActions = true;

    /**
     * Setting to false will not run any actions bound to key/mouse press.
     * The events will still continue to be registered.
     *
     * @param b process actions flag
     */
    public void setProcessActions(boolean b) {
        processActions = b;
    }

    /**
     * Clears all input, that is releases all key presses and mouse clicks
     * for a single frame
     */
    public void clearAllInput() {
        log.finer("Clearing active input actions");

        currentActions.clear();
        mouse.leftPressed = false;
        mouse.rightPressed = false;
    }

    /**
     * Bind given action to a mouse button.
     *
     * @param action the action to bind
     * @param btn the mouse button
     * @throws IllegalArgumentException if action with same name exists
     */
    public void addAction(UserAction action, MouseButton btn) {
        addAction(action, btn, InputModifier.NONE);
    }

    /**
     * Bind given action to a mouse button with special modifier key.
     *
     * @param action the action to bind
     * @param btn the mouse button
     * @param modifier the key modifier
     * @throws IllegalArgumentException if action with same name exists
     */
    public void addAction(UserAction action, MouseButton btn, InputModifier modifier) {
        if (findBindingByAction(action).isPresent()) {
            throw new IllegalArgumentException("Action with name \"" + action.getName()
                    + "\" already exists");
        }

        InputBinding binding = new InputBinding(action, btn, modifier);

        bindings.add(binding);
        log.finer("Registered new binding: " + binding);
    }

    /**
     * Bind given action to a keyboard key.
     *
     * @param action the action to bind
     * @param key the key
     * @throws IllegalArgumentException if action with same name exists
     */
    public void addAction(UserAction action, KeyCode key) {
        addAction(action, key, InputModifier.NONE);
    }

    /**
     * Bind given action to a keyboard key with special modifier key.
     *
     * @param action the action to bind
     * @param key the key
     * @param modifier the key modifier
     * @throws IllegalArgumentException if action with same name exists
     */
    public void addAction(UserAction action, KeyCode key, InputModifier modifier) {
        if (findBindingByAction(action).isPresent()) {
            throw new IllegalArgumentException("Action with name \"" + action.getName()
                    + "\" already exists");
        }

        InputBinding binding = new InputBinding(action, key, modifier);

        bindings.add(binding);
        log.finer("Registered new binding: " + binding);
    }

    /**
     * Find binding for given action.
     *
     * @param action the user action
     * @return input binding
     */
    private Optional<InputBinding> findBindingByAction(UserAction action) {
        return bindings.stream()
                .filter(binding -> binding.getAction().equals(action))
                .findAny();
    }

    /**
     * @param key the key to check
     * @return true if an action is already bound to given key
     */
    private boolean isKeyBound(KeyCode key) {
        return bindings.stream()
                .anyMatch(binding -> binding.isTriggered(key));
    }

    /**
     * @param btn the mouse button to check
     * @return true if an action is already bound to given button
     */
    private boolean isButtonBound(MouseButton btn) {
        return bindings.stream()
                .anyMatch(binding -> binding.isTriggered(btn));
    }

    /**
     * Rebinds an action to given key.
     *
     * @param action the user action
     * @param key the key to rebind to
     * @return true if rebound, false if action not found or
     * there is another action bound to key
     */
    public boolean rebind(UserAction action, KeyCode key) {
        Optional<InputBinding> maybeBinding = findBindingByAction(action);
        if (!maybeBinding.isPresent() || isKeyBound(key))
            return false;

        maybeBinding.get().setTrigger(key);
        return true;
    }

    /**
     * Rebinds an action to given mouse button.
     *
     * @param action the user action
     * @param btn the mouse button
     * @return true if rebound, false if action not found or
     * there is another action bound to mouse button
     */
    public boolean rebind(UserAction action, MouseButton btn) {
        Optional<InputBinding> maybeBinding = findBindingByAction(action);
        if (!maybeBinding.isPresent() || isButtonBound(btn))
            return false;

        maybeBinding.get().setTrigger(btn);
        return true;
    }

    /**
     * Returns mouse object that contains constantly updated
     * data about mouse state.
     *
     * @return mouse object
     */
    public Mouse getMouse() {
        return mouse;
    }

    @Override
    public void save(UserProfile profile) {
        log.finer("Saving data to profile");

        UserProfile.Bundle bundle = new UserProfile.Bundle("input");
        for (InputBinding binding : getBindings()) {
            bundle.put(binding.getAction().getName(), binding.triggerNameProperty().get());
        }

        bundle.log();
        profile.putBundle(bundle);
    }

    @Override
    public void load(UserProfile profile) {
        log.finer("Loading data from profile");

        UserProfile.Bundle bundle = profile.getBundle("input");
        bundle.log();

        for (InputBinding binding : getBindings()) {
            String triggerName = bundle.get(binding.getAction().getName());
            int plusIndex = triggerName.indexOf("+");
            if (plusIndex != -1) {
                triggerName = triggerName.substring(plusIndex + 1);
            }

            binding.removeTriggers();
            try {
                KeyCode key = KeyCode.getKeyCode(triggerName);
                binding.setTrigger(key);
            } catch (Exception ignored) {
                try {
                    MouseButton btn = MouseButton.valueOf(triggerName);
                    binding.setTrigger(btn);
                } catch (Exception e) {
                    log.warning("Undefined trigger name: " + triggerName);
                    throw new IllegalArgumentException("Corrupt or incompatible user profile: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Holds mouse state information
     *
     * @author Almas Baimagambetov (AlmasB) (almaslvl@gmail.com)
     */
    public final class Mouse {
        private Mouse() {
        }

        /**
         *
         * @return mouse x in game coordinate system
         */
        public double getGameX() {
            return getGameXY().getX();
        }

        /**
         *
         * @return mouse y in game coordinate system
         */
        public double getGameY() {
            return getGameXY().getY();
        }

        /**
         *
         * @return cursor point in game coordinate system
         */
        public Point2D getGameXY() {
            return gameScene.screenToGame(getScreenXY());
        }

        /**
         * Hold the value of gameX and y coordinate of the mouse cursor
         * in the current frame (tick) within the screen coordinate system
         */
        private double screenX, screenY;

        /**
         *
         * @return mouse x in screen coordinate system
         */
        public double getScreenX() {
            return screenX;
        }

        /**
         *
         * @return mouse y in screen coordinate system
         */
        public double getScreenY() {
            return screenY;
        }

        /**
         *
         * @return cursor point in screen coordinate system
         */
        public Point2D getScreenXY() {
            return new Point2D(screenX, screenY);
        }

        /**
         * Hold the state of left and right
         * mouse buttons in the current frame (tick)
         */
        private boolean leftPressed, rightPressed;

        /**
         *
         * @return true iff left mouse button is pressed
         */
        public boolean isLeftPressed() {
            return leftPressed;
        }

        /**
         *
         * @return true iff right mouse button is pressed
         */
        public boolean isRightPressed() {
            return rightPressed;
        }

        /**
         * The last internal event
         */
        private MouseEvent event;

        /**
         * Update state of mouse with data from JavaFX mouse event.
         */
        private void update(MouseEvent event) {
            this.event = event;
            this.screenX = event.getSceneX();
            this.screenY = event.getSceneY();

            if (leftPressed) {
                if (event.getButton() == MouseButton.PRIMARY && isReleased(event)) {
                    leftPressed = false;
                }
            } else {
                leftPressed = event.getButton() == MouseButton.PRIMARY && isPressed(event);
            }

            if (rightPressed) {
                if (event.getButton() == MouseButton.SECONDARY && isReleased(event)) {
                    rightPressed = false;
                }
            } else {
                rightPressed = event.getButton() == MouseButton.SECONDARY && isPressed(event);
            }
        }

        private boolean isPressed(MouseEvent event) {
            return event.getEventType() == MouseEvent.MOUSE_PRESSED
                    || event.getEventType() == MouseEvent.MOUSE_DRAGGED;
        }

        private boolean isReleased(MouseEvent event) {
            return event.getEventType() == MouseEvent.MOUSE_RELEASED
                    || event.getEventType() == MouseEvent.MOUSE_MOVED;
        }

        /**
         * It's unlikely that you'll need this.
         *
         * @return last JavaFX mouse event
         */
        public MouseEvent getEvent() {
            return event;
        }
    }

    /**
     * Convenience wrapper for input types.
     */
    static class Trigger {
        KeyCode key;
        MouseButton btn;

        boolean ctrl, shift, alt;

        Trigger(InputEvent event) {
            if (event instanceof KeyEvent) {
                KeyEvent e = (KeyEvent) event;
                key = e.getCode();
                ctrl = e.isControlDown();
                shift = e.isShiftDown();
                alt = e.isAltDown();
            } else if (event instanceof MouseEvent) {
                MouseEvent e = (MouseEvent) event;
                btn = e.getButton();
                ctrl = e.isControlDown();
                shift = e.isShiftDown();
                alt = e.isAltDown();
            } else {
                throw new IllegalArgumentException("Unknown event type: " + event);
            }
        }
    }

    @Override
    public void onEntityAdded(Entity entity) {
    }

    @Override
    public void onEntityRemoved(Entity entity) {
    }

    @Override
    public void onWorldUpdate() {
        if (processActions) {
            currentActions.forEach(UserAction::onAction);
        }
    }

    @Override
    public void onWorldReset() {
        clearAllInput();
    }
}