package com.daniilgrbic.telemouseapp;

import android.util.Log;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;

public class Controller extends MainActivity{

    int clickX, clickY, x, y, scroll;
    boolean isDown, dragging;
    long lastDown, lastUp, newClick, lastClick, lastM;

    public Controller() {
        isDown = false;
        dragging = false;
        lastDown = 0;
        lastUp = 0;
        newClick = 0;
        lastClick = -10000;
        scroll = -1;
        lastM = System.currentTimeMillis();
    }

    public String scrolled(MotionEvent motionEvent) {
        if(motionEvent.getPointerCount() == 1) {
            String ys = Integer.toString((int) (1.5f * (- scroll + (int) motionEvent.getY())));
            scroll = (int) motionEvent.getY();
            int action = motionEvent.getActionMasked();
            if(action == MotionEvent.ACTION_MOVE) {
                if(Math.abs(Integer.parseInt(ys)) < 150) {
                    return "SCROLL."+ys;
                }
            }
        }
        return null;
    }

    public String movedCursor(MotionEvent motionEvent) {
        if(motionEvent.getPointerCount() == 1) {
            String xs = Integer.toString(- x + (int) motionEvent.getX());
            String ys = Integer.toString(- y + (int) motionEvent.getY());
            x = (int) motionEvent.getX();
            y = (int) motionEvent.getY();
            int action = motionEvent.getActionMasked();
            if(action == MotionEvent.ACTION_DOWN) {
                if(!isDown) {
                    lastDown = System.currentTimeMillis();
                    isDown = true;
                }
                else {
                    // left button pressed => drag event
                    if(!dragging) {
                        dragging = true;
                        return "START_DRAG";
                    }
                }
            }
            else if(action == MotionEvent.ACTION_UP) {
                if(!dragging) {
                    lastUp = System.currentTimeMillis();
                    isDown = false;
                    if (lastUp - lastDown > 0 && lastUp - lastDown < 200) {
                        lastClick = newClick;
                        newClick = System.currentTimeMillis();
                        if (newClick - lastClick < 200) {
                            return "DOUBLE_CLICK";
                        } else {
                            clickX = (int) motionEvent.getX();
                            clickY = (int) motionEvent.getY();
                            return "CLICK";
                        }
                    }
                }
            }
            else if(action == MotionEvent.ACTION_MOVE) {
                if(Math.abs(Integer.parseInt(xs))+Math.abs(Integer.parseInt(ys)) < 300) {
                    return "MOVING."+xs+"."+ys;
                }
            }
        }
        else  if (motionEvent.getPointerCount() == 2) {
            if(System.currentTimeMillis()-lastM > 500) {
                lastM = System.currentTimeMillis();
                return "RIGHT";
            }
        }
        else {
            Log.i("TOUCH_HANDLER","More than 2 fingers down!");
        }
        return null;
    }

    public String leftClicked(MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                lastDown = System.currentTimeMillis();
                isDown = true;
                break;
            case MotionEvent.ACTION_UP:
                rightButton.playSoundEffect(SoundEffectConstants.CLICK);
                lastUp = System.currentTimeMillis();
                isDown = false;
                if(dragging) {
                    dragging = false;
                    return "DROP";
                }
                else if(lastUp-lastDown < 200) {
                    lastClick = newClick;
                    newClick = System.currentTimeMillis();
                    if(newClick - lastClick < 200) {
                        return "DOUBLE_CLICK";
                    } else {
                        clickX = (int) motionEvent.getX();
                        clickY = (int) motionEvent.getY();
                        return "CLICK";
                    }
                }
                break;
        }
        return null;
    }

    public String rightClicked(MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // pass
                break;
            case MotionEvent.ACTION_UP:
                rightButton.playSoundEffect(SoundEffectConstants.CLICK);
                return "RIGHT";
        }
        return null;
    }
}
